package com.bkiptv.app.data.parser

import com.bkiptv.app.data.model.ContentType

/**
 * Represents a parsed entry from an M3U/M3U8 playlist file
 * Contains all extracted metadata and stream information
 */
data class M3UEntry(
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val groupTitle: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val tvgCountry: String? = null,
    val tvgLanguage: String? = null,
    val duration: Int = -1,
    val contentType: ContentType = ContentType.UNKNOWN,
    val headers: Map<String, String> = emptyMap(),
    
    // Series-specific fields (extracted from name patterns)
    val seriesName: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    
    // Movie-specific fields
    val year: Int? = null,
    val rating: Float? = null
) {
    /**
     * Returns a display-friendly name
     * For episodes, formats as "Series - S01E02"
     */
    fun getDisplayName(): String {
        return when (contentType) {
            ContentType.EPISODE -> {
                val season = seasonNumber?.let { "S${it.toString().padStart(2, '0')}" } ?: ""
                val episode = episodeNumber?.let { "E${it.toString().padStart(2, '0')}" } ?: ""
                if (seriesName != null && (season.isNotEmpty() || episode.isNotEmpty())) {
                    "$seriesName - $season$episode"
                } else {
                    name
                }
            }
            else -> name
        }
    }

    /**
     * Extracts country code from tvgCountry or groupTitle
     */
    fun getCountryCode(): String? {
        tvgCountry?.let { return it.uppercase() }
        // Try to extract from group title patterns like "France |"
        groupTitle?.let { group ->
            val countryPrefixPattern = Regex("""^([A-Z]{2,3})\s*[\|:]""")
            countryPrefixPattern.find(group)?.let { match ->
                return match.groupValues[1]
            }
        }
        return null
    }
}
