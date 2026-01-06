package com.bkiptv.app.data.parser

import com.bkiptv.app.data.model.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser for M3U/M3U8 playlist files
 * Handles various M3U formats with robust error handling
 * 
 * Supports:
 * - Standard #EXTINF entries
 * - Extended attributes (tvg-id, tvg-logo, group-title, etc.)
 * - HTTP headers (User-Agent, Referer)
 * - Multiple content types (Live, VOD, Series)
 */
@Singleton
class M3UParser @Inject constructor() {

    companion object {
        private const val EXTM3U = "#EXTM3U"
        private const val EXTINF = "#EXTINF:"
        private const val EXTVLCOPT = "#EXTVLCOPT:"
        private const val KODIPROP = "#KODIPROP:"
        private const val EXTGRP = "#EXTGRP:"

        // Regex patterns for attribute extraction
        private val TVG_ID_PATTERN = Regex("""tvg-id\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
        private val TVG_NAME_PATTERN = Regex("""tvg-name\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
        private val TVG_LOGO_PATTERN = Regex("""tvg-logo\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
        private val GROUP_TITLE_PATTERN = Regex("""group-title\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
        private val TVG_COUNTRY_PATTERN = Regex("""tvg-country\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
        private val TVG_LANGUAGE_PATTERN = Regex("""tvg-language\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
        private val DURATION_PATTERN = Regex("""^#EXTINF:\s*(-?\d+)""")

        // Patterns for series episode detection
        private val SERIES_PATTERN = Regex("""(?i)[sS](\d{1,2})\s*[eE](\d{1,2})""")
        private val SEASON_EPISODE_PATTERN = Regex("""(?i)(?:season|saison)\s*(\d+).*?(?:episode|épisode)\s*(\d+)""")
        private val YEAR_PATTERN = Regex("""\((\d{4})\)|\[(\d{4})\]""")
    }

    /**
     * Parse M3U content from an InputStream
     * @param inputStream The input stream containing M3U data
     * @return List of parsed M3U entries
     */
    suspend fun parse(inputStream: InputStream): List<M3UEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<M3UEntry>()
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        
        var currentExtinf: String? = null
        var currentHeaders = mutableMapOf<String, String>()
        var currentGroup: String? = null
        
        reader.useLines { lines ->
            for (line in lines) {
                val trimmedLine = line.trim()
                
                when {
                    trimmedLine.isEmpty() -> continue
                    
                    trimmedLine.startsWith(EXTM3U) -> {
                        // Header line - could extract global headers if needed
                        continue
                    }
                    
                    trimmedLine.startsWith(EXTINF) -> {
                        currentExtinf = trimmedLine
                    }
                    
                    trimmedLine.startsWith(EXTVLCOPT) || trimmedLine.startsWith(KODIPROP) -> {
                        // Parse VLC/Kodi options for headers
                        parseHeaderOption(trimmedLine)?.let { (key, value) ->
                            currentHeaders[key] = value
                        }
                    }
                    
                    trimmedLine.startsWith(EXTGRP) -> {
                        // Group marker
                        currentGroup = trimmedLine.substringAfter(EXTGRP).trim()
                    }
                    
                    trimmedLine.startsWith("#") -> {
                        // Other comment/directive - ignore
                        continue
                    }
                    
                    else -> {
                        // This should be a URL
                        if (currentExtinf != null && isValidUrl(trimmedLine)) {
                            parseEntry(currentExtinf!!, trimmedLine, currentHeaders, currentGroup)?.let {
                                entries.add(it)
                            }
                        }
                        // Reset for next entry
                        currentExtinf = null
                        currentHeaders = mutableMapOf()
                        currentGroup = null
                    }
                }
            }
        }
        
        entries
    }

    /**
     * Parse M3U content from a string
     */
    suspend fun parse(content: String): List<M3UEntry> {
        return parse(content.byteInputStream())
    }

    /**
     * Parse a single EXTINF line and URL into an M3UEntry
     */
    private fun parseEntry(
        extinfLine: String,
        url: String,
        headers: Map<String, String>,
        extGroup: String?
    ): M3UEntry? {
        try {
            // Extract duration
            val duration = DURATION_PATTERN.find(extinfLine)?.groupValues?.get(1)?.toIntOrNull() ?: -1
            
            // Extract name (everything after the last comma)
            val name = extractName(extinfLine)
            if (name.isBlank()) return null
            
            // Extract attributes
            val tvgId = TVG_ID_PATTERN.find(extinfLine)?.groupValues?.get(1)
            val tvgName = TVG_NAME_PATTERN.find(extinfLine)?.groupValues?.get(1)
            val tvgLogo = TVG_LOGO_PATTERN.find(extinfLine)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
            val groupTitle = GROUP_TITLE_PATTERN.find(extinfLine)?.groupValues?.get(1) ?: extGroup
            val tvgCountry = TVG_COUNTRY_PATTERN.find(extinfLine)?.groupValues?.get(1)
            val tvgLanguage = TVG_LANGUAGE_PATTERN.find(extinfLine)?.groupValues?.get(1)
            
            // Detect content type
            val contentType = ContentType.detect(groupTitle, name, url)
            
            // Extract series info if applicable
            val (seriesName, seasonNum, episodeNum) = extractSeriesInfo(name, contentType)
            
            // Extract year from name
            val year = extractYear(name)
            
            return M3UEntry(
                name = name,
                url = url,
                logoUrl = tvgLogo,
                groupTitle = groupTitle,
                tvgId = tvgId,
                tvgName = tvgName,
                tvgCountry = tvgCountry,
                tvgLanguage = tvgLanguage,
                duration = duration,
                contentType = contentType,
                headers = headers,
                seriesName = seriesName,
                seasonNumber = seasonNum,
                episodeNumber = episodeNum,
                year = year
            )
        } catch (e: Exception) {
            // Log parsing error but don't crash
            return null
        }
    }

    /**
     * Extract the display name from EXTINF line
     * Handles edge cases where commas may appear in attributes
     */
    private fun extractName(extinfLine: String): String {
        // Find the last comma that's not inside quotes
        var inQuotes = false
        var quoteChar = ' '
        var lastCommaIndex = -1
        
        for (i in extinfLine.indices) {
            val char = extinfLine[i]
            when {
                (char == '"' || char == '\'') && !inQuotes -> {
                    inQuotes = true
                    quoteChar = char
                }
                char == quoteChar && inQuotes -> {
                    inQuotes = false
                }
                char == ',' && !inQuotes -> {
                    lastCommaIndex = i
                }
            }
        }
        
        return if (lastCommaIndex >= 0) {
            extinfLine.substring(lastCommaIndex + 1).trim()
        } else {
            // Fallback: try simple extraction
            extinfLine.substringAfterLast(",").trim()
        }
    }

    /**
     * Parse VLC/Kodi option for HTTP headers
     */
    private fun parseHeaderOption(line: String): Pair<String, String>? {
        val content = when {
            line.startsWith(EXTVLCOPT) -> line.substringAfter(EXTVLCOPT)
            line.startsWith(KODIPROP) -> line.substringAfter(KODIPROP)
            else -> return null
        }
        
        // VLC format: http-user-agent=value or http-referrer=value
        val parts = content.split("=", limit = 2)
        if (parts.size != 2) return null
        
        val key = parts[0].trim().lowercase()
        val value = parts[1].trim()
        
        return when {
            key.contains("user-agent") -> "User-Agent" to value
            key.contains("referrer") || key.contains("referer") -> "Referer" to value
            key.contains("origin") -> "Origin" to value
            else -> null
        }
    }

    /**
     * Extract series name, season, and episode numbers
     */
    private fun extractSeriesInfo(name: String, contentType: ContentType): Triple<String?, Int?, Int?> {
        if (contentType != ContentType.EPISODE && contentType != ContentType.SERIES) {
            return Triple(null, null, null)
        }
        
        // Try S01E02 pattern
        SERIES_PATTERN.find(name)?.let { match ->
            val seasonNum = match.groupValues[1].toIntOrNull()
            val episodeNum = match.groupValues[2].toIntOrNull()
            val seriesName = name.substring(0, match.range.first).trim().trimEnd('-', ':', ' ')
            return Triple(seriesName.takeIf { it.isNotBlank() }, seasonNum, episodeNum)
        }
        
        // Try Season X Episode Y pattern
        SEASON_EPISODE_PATTERN.find(name)?.let { match ->
            val seasonNum = match.groupValues[1].toIntOrNull()
            val episodeNum = match.groupValues[2].toIntOrNull()
            val seriesName = name.substring(0, match.range.first).trim()
            return Triple(seriesName.takeIf { it.isNotBlank() }, seasonNum, episodeNum)
        }
        
        return Triple(null, null, null)
    }

    /**
     * Extract year from movie/series name
     */
    private fun extractYear(name: String): Int? {
        YEAR_PATTERN.find(name)?.let { match ->
            return match.groupValues[1].takeIf { it.isNotEmpty() }?.toIntOrNull()
                ?: match.groupValues[2].toIntOrNull()
        }
        return null
    }

    /**
     * Validate URL format
     */
    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://") || 
               url.startsWith("https://") || 
               url.startsWith("rtmp://") ||
               url.startsWith("rtsp://") ||
               url.startsWith("mms://")
    }
}
