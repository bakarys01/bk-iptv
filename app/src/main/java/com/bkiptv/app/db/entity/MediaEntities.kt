package com.bkiptv.app.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bkiptv.app.data.model.ContentType

/**
 * Entity representing a TV channel from a playlist
 */
@Entity(
    tableName = "channels",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["tvgId"]),
        Index(value = ["groupTitle"]),
        Index(value = ["country"])
    ]
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val playlistId: Long,
    
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    
    val tvgId: String? = null,
    val tvgName: String? = null,
    val groupTitle: String? = null,
    val category: String? = null,
    val country: String? = null,
    val language: String? = null,
    
    val headers: String? = null, // JSON encoded headers
    
    val isFavorite: Boolean = false,
    val sortOrder: Int = 0,
    
    val lastWatchedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing a VOD movie
 */
@Entity(
    tableName = "movies",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["genre"]),
        Index(value = ["country"]),
        Index(value = ["year"])
    ]
)
data class MovieEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val playlistId: Long,
    
    val title: String,
    val streamUrl: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    
    val description: String? = null,
    val genre: String? = null,
    val country: String? = null,
    val year: Int? = null,
    val duration: Int? = null, // in minutes
    val rating: Float? = null,
    
    val headers: String? = null,
    
    val isFavorite: Boolean = false,
    
    val lastPlayPosition: Long = 0, // in milliseconds
    val lastWatchedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing a TV series
 */
@Entity(
    tableName = "series",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["genre"]),
        Index(value = ["country"])
    ]
)
data class SeriesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val playlistId: Long,
    
    val name: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    
    val description: String? = null,
    val genre: String? = null,
    val country: String? = null,
    val year: Int? = null,
    val rating: Float? = null,
    
    val seasonCount: Int = 0,
    val episodeCount: Int = 0,
    
    val isFavorite: Boolean = false,
    val lastWatchedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing a series episode
 */
@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = SeriesEntity::class,
            parentColumns = ["id"],
            childColumns = ["seriesId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["seriesId"]),
        Index(value = ["seasonNumber", "episodeNumber"])
    ]
)
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val seriesId: Long,
    
    val title: String,
    val streamUrl: String,
    val thumbnailUrl: String? = null,
    
    val seasonNumber: Int,
    val episodeNumber: Int,
    val description: String? = null,
    val duration: Int? = null,
    
    val headers: String? = null,
    
    val lastPlayPosition: Long = 0,
    val lastWatchedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
