package com.bkiptv.app.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Entity representing a playlist source
 * Users can add multiple playlists via URL, file, or QR code
 */
@Entity(
    tableName = "playlists",
    indices = [Index(value = ["url"], unique = true)]
)
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    val url: String,
    val type: PlaylistType = PlaylistType.M3U,
    val isEnabled: Boolean = true,
    val isAutoRefresh: Boolean = true,
    val refreshIntervalHours: Int = 24,
    
    // Xtream Codes credentials
    val xtreamUsername: String? = null,
    val xtreamPassword: String? = null,
    
    val lastSyncTimestamp: Long? = null,
    val lastSyncStatus: SyncStatus = SyncStatus.PENDING,
    val lastSyncError: String? = null,
    
    val channelCount: Int = 0,
    val movieCount: Int = 0,
    val seriesCount: Int = 0,
    
    val epgUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class PlaylistType {
    M3U,
    M3U8,
    XTREAM_API
}

enum class SyncStatus {
    PENDING,
    SYNCING,
    SUCCESS,
    FAILED
}
