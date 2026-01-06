package com.bkiptv.app.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bkiptv.app.db.dao.*
import com.bkiptv.app.db.entity.*

/**
 * Main Room database for BK IPTV application
 * Contains all entities for playlists, media content, EPG, and user data
 */
@Database(
    entities = [
        PlaylistEntity::class,
        ChannelEntity::class,
        MovieEntity::class,
        SeriesEntity::class,
        EpisodeEntity::class,
        EPGProgramEntity::class,
        FavoriteEntity::class,
        HistoryEntity::class,
        ProfileEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao
    abstract fun channelDao(): ChannelDao
    abstract fun movieDao(): MovieDao
    abstract fun seriesDao(): SeriesDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun epgDao(): EPGDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun profileDao(): ProfileDao

    companion object {
        const val DATABASE_NAME = "bk_iptv_database"
    }
}
