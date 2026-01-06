package com.bkiptv.app.util

/**
 * Application constants
 */
object Constants {
    // App info
    const val APP_NAME = "BK IPTV"
    const val APP_VERSION = "1.0.0"
    
    // Network
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 60L
    const val WRITE_TIMEOUT_SECONDS = 30L
    
    // Cache
    const val HTTP_CACHE_SIZE_MB = 50L
    const val IMAGE_CACHE_MEMORY_PERCENT = 0.25
    const val IMAGE_CACHE_DISK_PERCENT = 0.05
    
    // Player
    const val MIN_BUFFER_MS = 15_000
    const val MAX_BUFFER_MS = 50_000
    const val BUFFER_FOR_PLAYBACK_MS = 2_500
    const val BUFFER_FOR_REBUFFER_MS = 5_000
    const val SEEK_INCREMENT_MS = 10_000L
    const val MAX_RETRY_COUNT = 3
    
    // Sync
    const val DEFAULT_PLAYLIST_REFRESH_HOURS = 6
    const val DEFAULT_EPG_REFRESH_HOURS = 12
    
    // UI
    const val CONTROLS_HIDE_DELAY_MS = 5_000L
    const val ANIMATION_DURATION_MS = 300
    
    // Content types
    const val CONTENT_TYPE_CHANNEL = "CHANNEL"
    const val CONTENT_TYPE_MOVIE = "MOVIE"
    const val CONTENT_TYPE_SERIES = "SERIES"
    const val CONTENT_TYPE_EPISODE = "EPISODE"
}

/**
 * Content type enum for favorites/history
 */
enum class ContentTypeKey(val value: String) {
    CHANNEL("CHANNEL"),
    MOVIE("MOVIE"),
    SERIES("SERIES"),
    EPISODE("EPISODE")
}
