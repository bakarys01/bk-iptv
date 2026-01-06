package com.bkiptv.app.db.dao

import androidx.room.*
import com.bkiptv.app.db.entity.PlaylistEntity
import com.bkiptv.app.db.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE isEnabled = 1 ORDER BY createdAt DESC")
    fun getEnabledPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE url = :url LIMIT 1")
    suspend fun getPlaylistByUrl(url: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: Long)

    @Query("UPDATE playlists SET isEnabled = :enabled, updatedAt = :timestamp WHERE id = :id")
    suspend fun setPlaylistEnabled(id: Long, enabled: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE playlists SET 
            lastSyncTimestamp = :timestamp, 
            lastSyncStatus = :status,
            lastSyncError = :error,
            channelCount = :channelCount,
            movieCount = :movieCount,
            seriesCount = :seriesCount,
            updatedAt = :timestamp
        WHERE id = :id
    """)
    suspend fun updateSyncStatus(
        id: Long,
        timestamp: Long,
        status: SyncStatus,
        error: String?,
        channelCount: Int,
        movieCount: Int,
        seriesCount: Int
    )

    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun getPlaylistCount(): Int
}
