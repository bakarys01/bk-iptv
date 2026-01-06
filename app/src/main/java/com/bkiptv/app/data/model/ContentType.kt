package com.bkiptv.app.data.model

/**
 * Enum representing the type of media content
 * Used to categorize and organize content from M3U playlists
 */
enum class ContentType {
    LIVE_TV,      // Live TV channels
    MOVIE,        // VOD movies
    SERIES,       // TV series (grouped episodes)
    EPISODE,      // Individual series episode
    UNKNOWN;      // Uncategorized content

    companion object {
        /**
         * Detects content type based on M3U entry attributes and URL patterns
         */
        fun detect(
            groupTitle: String?,
            name: String?,
            url: String?
        ): ContentType {
            val lowerGroup = groupTitle?.lowercase() ?: ""
            val lowerName = name?.lowercase() ?: ""
            val lowerUrl = url?.lowercase() ?: ""

            // Check for series patterns (S01E01, Season, Episode)
            val seriesPattern = Regex("""[sS]\d{1,2}[eE]\d{1,2}|season|saison|episode|épisode""")
            if (seriesPattern.containsMatchIn(lowerName) || 
                seriesPattern.containsMatchIn(lowerGroup)) {
                return EPISODE
            }

            // Check for series group indicators
            if (lowerGroup.contains("series") || 
                lowerGroup.contains("séries") ||
                lowerGroup.contains("shows")) {
                return SERIES
            }

            // Check for VOD/Movie patterns
            if (lowerGroup.contains("vod") || 
                lowerGroup.contains("movie") ||
                lowerGroup.contains("film") ||
                lowerGroup.contains("cinema") ||
                lowerGroup.contains("cinéma")) {
                return MOVIE
            }

            // Check URL patterns for VOD
            if (lowerUrl.contains("/movie/") || 
                lowerUrl.contains("/vod/") ||
                lowerUrl.contains("/films/")) {
                return MOVIE
            }

            // Check URL patterns for series
            if (lowerUrl.contains("/series/") || 
                lowerUrl.contains("/episode/")) {
                return EPISODE
            }

            // Default to Live TV for stream URLs
            if (lowerUrl.endsWith(".m3u8") || 
                lowerUrl.endsWith(".ts") ||
                lowerUrl.contains(":8080/") ||
                lowerUrl.contains("/live/")) {
                return LIVE_TV
            }

            return LIVE_TV // Default assumption
        }
    }
}
