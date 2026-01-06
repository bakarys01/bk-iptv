package com.bkiptv.app.data.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser for XMLTV format EPG (Electronic Program Guide) files
 * 
 * XMLTV is a standard format for TV listings data containing:
 * - Channel definitions with IDs and names
 * - Program listings with start/end times, titles, descriptions
 * 
 * This parser uses Android's built-in XmlPullParser for efficient streaming parsing
 */
@Singleton
class XMLTVParser @Inject constructor() {

    companion object {
        // XMLTV date format: 20240115120000 +0000
        private val XMLTV_DATE_FORMATTERS = listOf(
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss Z"),
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
            DateTimeFormatter.ofPattern("yyyyMMddHHmm Z"),
            DateTimeFormatter.ofPattern("yyyyMMddHHmm")
        )

        // XML Tags
        private const val TAG_TV = "tv"
        private const val TAG_CHANNEL = "channel"
        private const val TAG_PROGRAMME = "programme"
        private const val TAG_DISPLAY_NAME = "display-name"
        private const val TAG_ICON = "icon"
        private const val TAG_TITLE = "title"
        private const val TAG_DESC = "desc"
        private const val TAG_CATEGORY = "category"
        private const val TAG_RATING = "rating"
        private const val TAG_VALUE = "value"
        private const val TAG_SUB_TITLE = "sub-title"
        private const val TAG_EPISODE_NUM = "episode-num"

        // Attributes
        private const val ATTR_ID = "id"
        private const val ATTR_CHANNEL = "channel"
        private const val ATTR_START = "start"
        private const val ATTR_STOP = "stop"
        private const val ATTR_SRC = "src"
    }

    /**
     * Parse result containing both channels and programs
     */
    data class ParseResult(
        val channels: List<EPGChannelEntry>,
        val programs: List<EPGProgramEntry>
    )

    /**
     * Parse XMLTV content from an InputStream
     * @param inputStream The input stream containing XMLTV data
     * @return ParseResult with channels and programs
     */
    suspend fun parse(inputStream: InputStream): ParseResult = withContext(Dispatchers.IO) {
        val channels = mutableListOf<EPGChannelEntry>()
        val programs = mutableListOf<EPGProgramEntry>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(InputStreamReader(inputStream, Charsets.UTF_8))

            var eventType = parser.eventType
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            TAG_CHANNEL -> {
                                parseChannel(parser)?.let { channels.add(it) }
                            }
                            TAG_PROGRAMME -> {
                                parseProgram(parser)?.let { programs.add(it) }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // Log error but return what we have
            e.printStackTrace()
        }

        ParseResult(channels, programs)
    }

    /**
     * Parse a <channel> element
     */
    private fun parseChannel(parser: XmlPullParser): EPGChannelEntry? {
        val id = parser.getAttributeValue(null, ATTR_ID) ?: return null
        var displayName: String? = null
        var icon: String? = null

        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    depth++
                    when (parser.name) {
                        TAG_DISPLAY_NAME -> {
                            displayName = parser.nextText()
                            depth--
                        }
                        TAG_ICON -> {
                            icon = parser.getAttributeValue(null, ATTR_SRC)
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    depth--
                }
                XmlPullParser.END_DOCUMENT -> break
            }
        }

        return if (displayName != null) {
            EPGChannelEntry(id, displayName, icon)
        } else null
    }

    /**
     * Parse a <programme> element
     */
    private fun parseProgram(parser: XmlPullParser): EPGProgramEntry? {
        val channelId = parser.getAttributeValue(null, ATTR_CHANNEL) ?: return null
        val startStr = parser.getAttributeValue(null, ATTR_START) ?: return null
        val stopStr = parser.getAttributeValue(null, ATTR_STOP) ?: return null

        val startTime = parseDateTime(startStr) ?: return null
        val endTime = parseDateTime(stopStr) ?: return null

        var title: String? = null
        var description: String? = null
        var category: String? = null
        var icon: String? = null
        var rating: String? = null
        var subTitle: String? = null
        var episodeNum: String? = null

        var depth = 1
        var inRating = false

        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    depth++
                    when (parser.name) {
                        TAG_TITLE -> {
                            title = parser.nextText()
                            depth--
                        }
                        TAG_DESC -> {
                            description = parser.nextText()
                            depth--
                        }
                        TAG_CATEGORY -> {
                            category = parser.nextText()
                            depth--
                        }
                        TAG_ICON -> {
                            icon = parser.getAttributeValue(null, ATTR_SRC)
                        }
                        TAG_RATING -> {
                            inRating = true
                        }
                        TAG_VALUE -> {
                            if (inRating) {
                                rating = parser.nextText()
                                depth--
                            }
                        }
                        TAG_SUB_TITLE -> {
                            subTitle = parser.nextText()
                            depth--
                        }
                        TAG_EPISODE_NUM -> {
                            episodeNum = parser.nextText()
                            depth--
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == TAG_RATING) {
                        inRating = false
                    }
                    depth--
                }
                XmlPullParser.END_DOCUMENT -> break
            }
        }

        return if (title != null) {
            EPGProgramEntry(
                channelId = channelId,
                title = title,
                description = description,
                category = category,
                startTime = startTime,
                endTime = endTime,
                icon = icon,
                rating = rating,
                episodeNum = episodeNum,
                subTitle = subTitle
            )
        } else null
    }

    /**
     * Parse XMLTV date format to LocalDateTime
     * Handles multiple common formats
     */
    private fun parseDateTime(dateStr: String): LocalDateTime? {
        val cleanedDate = dateStr.trim()
        
        for (formatter in XMLTV_DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(cleanedDate, formatter)
            } catch (e: DateTimeParseException) {
                continue
            }
        }
        
        // Fallback: try to parse first 14 characters as basic format
        try {
            val basicStr = cleanedDate.take(14)
            if (basicStr.length == 14) {
                val year = basicStr.substring(0, 4).toInt()
                val month = basicStr.substring(4, 6).toInt()
                val day = basicStr.substring(6, 8).toInt()
                val hour = basicStr.substring(8, 10).toInt()
                val minute = basicStr.substring(10, 12).toInt()
                val second = basicStr.substring(12, 14).toInt()
                return LocalDateTime.of(year, month, day, hour, minute, second)
            }
        } catch (e: Exception) {
            // Ignore
        }
        
        return null
    }
}
