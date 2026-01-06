package com.bkiptv.app.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for EPG (Electronic Program Guide) data
 */
@Entity(
    tableName = "epg_programs",
    indices = [
        Index(value = ["channelId"]),
        Index(value = ["startTime"]),
        Index(value = ["endTime"]),
        Index(value = ["channelId", "startTime"])
    ]
)
data class EPGProgramEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val channelId: String, // tvg-id from XMLTV
    
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val subTitle: String? = null,
    val episodeNum: String? = null,
    val icon: String? = null,
    val rating: String? = null,
    
    val startTime: Long, // timestamp in millis
    val endTime: Long,
    
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Entity for user favorites
 * Supports all content types (channels, movies, episodes)
 */
@Entity(
    tableName = "favorites",
    indices = [
        Index(value = ["contentId", "contentType"], unique = true),
        Index(value = ["profileId"])
    ]
)
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val profileId: Long = 0, // 0 = default profile
    val contentId: Long,
    val contentType: String, // CHANNEL, MOVIE, SERIES
    
    val name: String,
    val logoUrl: String? = null,
    val streamUrl: String? = null,
    
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Entity for watch history
 */
@Entity(
    tableName = "history",
    indices = [
        Index(value = ["contentId", "contentType"]),
        Index(value = ["profileId"]),
        Index(value = ["watchedAt"])
    ]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val profileId: Long = 0,
    val contentId: Long,
    val contentType: String,
    
    val name: String,
    val logoUrl: String? = null,
    val streamUrl: String,
    
    val playPosition: Long = 0, // Resume position in millis
    val duration: Long = 0,
    
    val watchedAt: Long = System.currentTimeMillis()
)

/**
 * Entity for user profiles
 */
@Entity(
    tableName = "profiles",
    indices = [Index(value = ["name"], unique = true)]
)
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    val avatarUrl: String? = null,
    val isDefault: Boolean = false,
    val isKidsProfile: Boolean = false,
    
    val pin: String? = null, // Hashed PIN for parental controls
    val isPinRequired: Boolean = false,
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
