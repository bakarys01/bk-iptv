package com.bkiptv.app.data.parser

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Represents a parsed TV program from XMLTV/EPG data
 */
data class EPGProgramEntry(
    val channelId: String,
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val icon: String? = null,
    val rating: String? = null,
    val episodeNum: String? = null,
    val subTitle: String? = null
) {
    /**
     * Check if this program is currently airing
     */
    fun isNowPlaying(): Boolean {
        val now = LocalDateTime.now()
        return now.isAfter(startTime) && now.isBefore(endTime)
    }

    /**
     * Get duration in minutes
     */
    fun getDurationMinutes(): Long {
        return java.time.Duration.between(startTime, endTime).toMinutes()
    }

    /**
     * Get progress percentage (0-100) if currently playing
     */
    fun getProgress(): Int {
        if (!isNowPlaying()) return 0
        val now = LocalDateTime.now()
        val elapsed = java.time.Duration.between(startTime, now).toMinutes()
        val total = getDurationMinutes()
        return if (total > 0) ((elapsed * 100) / total).toInt().coerceIn(0, 100) else 0
    }
}

/**
 * Represents a channel definition from XMLTV
 */
data class EPGChannelEntry(
    val id: String,
    val displayName: String,
    val icon: String? = null
)
