package com.bkiptv.app.db.dao

import androidx.room.*
import com.bkiptv.app.db.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels ORDER BY sortOrder, name")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY sortOrder, name")
    fun getChannelsByPlaylist(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE groupTitle = :group ORDER BY sortOrder, name")
    fun getChannelsByGroup(group: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE country = :country ORDER BY sortOrder, name")
    fun getChannelsByCountry(country: String): Flow<List<ChannelEntity>>

    @Query("SELECT DISTINCT country FROM channels WHERE country IS NOT NULL ORDER BY country")
    fun getAllCountries(): Flow<List<String>>

    @Query("SELECT DISTINCT groupTitle FROM channels WHERE groupTitle IS NOT NULL ORDER BY groupTitle")
    fun getAllGroups(): Flow<List<String>>

    @Query("SELECT DISTINCT category FROM channels WHERE category IS NOT NULL ORDER BY category")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun getChannelById(id: Long): ChannelEntity?

    @Query("SELECT * FROM channels WHERE tvgId = :tvgId LIMIT 1")
    suspend fun getChannelByTvgId(tvgId: String): ChannelEntity?

    @Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY sortOrder, name")
    fun getFavoriteChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%' OR tvgName LIKE '%' || :query || '%' ORDER BY name")
    fun searchChannels(query: String): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Update
    suspend fun updateChannel(channel: ChannelEntity)

    @Delete
    suspend fun deleteChannel(channel: ChannelEntity)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteChannelsByPlaylist(playlistId: Long)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE channels SET lastWatchedAt = :timestamp WHERE id = :id")
    suspend fun updateLastWatched(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM channels WHERE playlistId = :playlistId")
    suspend fun getChannelCountByPlaylist(playlistId: Long): Int

    @Query("SELECT COUNT(*) FROM channels")
    suspend fun getTotalChannelCount(): Int
}
