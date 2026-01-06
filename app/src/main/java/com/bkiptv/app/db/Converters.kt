package com.bkiptv.app.db

import androidx.room.TypeConverter
import com.bkiptv.app.db.entity.PlaylistType
import com.bkiptv.app.db.entity.SyncStatus

/**
 * Type converters for Room database
 * Handles conversion of enums to/from strings for storage
 */
class Converters {

    @TypeConverter
    fun fromPlaylistType(type: PlaylistType): String = type.name

    @TypeConverter
    fun toPlaylistType(value: String): PlaylistType = 
        PlaylistType.valueOf(value)

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = 
        SyncStatus.valueOf(value)
}
