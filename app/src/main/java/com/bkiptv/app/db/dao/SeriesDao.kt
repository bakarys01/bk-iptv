package com.bkiptv.app.db.dao

import androidx.room.*
import com.bkiptv.app.db.entity.SeriesEntity
import com.bkiptv.app.db.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeriesDao {

    @Query("SELECT * FROM series ORDER BY name")
    fun getAllSeries(): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE playlistId = :playlistId ORDER BY name")
    fun getSeriesByPlaylist(playlistId: Long): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE genre LIKE '%' || :genre || '%' ORDER BY name")
    fun getSeriesByGenre(genre: String): Flow<List<SeriesEntity>>

    @Query("SELECT DISTINCT genre FROM series WHERE genre IS NOT NULL ORDER BY genre")
    fun getAllGenres(): Flow<List<String>>

    @Query("SELECT * FROM series WHERE id = :id")
    suspend fun getSeriesById(id: Long): SeriesEntity?

    @Query("SELECT * FROM series WHERE isFavorite = 1 ORDER BY name")
    fun getFavoriteSeries(): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE name LIKE '%' || :query || '%' ORDER BY name")
    fun searchSeries(query: String): Flow<List<SeriesEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeries(series: SeriesEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeriesList(seriesList: List<SeriesEntity>)

    @Update
    suspend fun updateSeries(series: SeriesEntity)

    @Delete
    suspend fun deleteSeries(series: SeriesEntity)

    @Query("DELETE FROM series WHERE playlistId = :playlistId")
    suspend fun deleteSeriesByPlaylist(playlistId: Long)

    @Query("UPDATE series SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM series WHERE playlistId = :playlistId")
    suspend fun getSeriesCountByPlaylist(playlistId: Long): Int
}

@Dao
interface EpisodeDao {

    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY seasonNumber, episodeNumber")
    fun getEpisodesBySeries(seriesId: Long): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId AND seasonNumber = :season ORDER BY episodeNumber")
    fun getEpisodesBySeason(seriesId: Long, season: Int): Flow<List<EpisodeEntity>>

    @Query("SELECT DISTINCT seasonNumber FROM episodes WHERE seriesId = :seriesId ORDER BY seasonNumber")
    fun getSeasonNumbers(seriesId: Long): Flow<List<Int>>

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun getEpisodeById(id: Long): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE lastPlayPosition > 0 ORDER BY lastWatchedAt DESC LIMIT :limit")
    fun getRecentlyWatchedEpisodes(limit: Int = 20): Flow<List<EpisodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisode(episode: EpisodeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<EpisodeEntity>)

    @Update
    suspend fun updateEpisode(episode: EpisodeEntity)

    @Delete
    suspend fun deleteEpisode(episode: EpisodeEntity)

    @Query("DELETE FROM episodes WHERE seriesId = :seriesId")
    suspend fun deleteEpisodesBySeries(seriesId: Long)

    @Query("UPDATE episodes SET lastPlayPosition = :position, lastWatchedAt = :timestamp WHERE id = :id")
    suspend fun updatePlayProgress(id: Long, position: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM episodes WHERE seriesId = :seriesId")
    suspend fun getEpisodeCountBySeries(seriesId: Long): Int

    @Query("SELECT COUNT(DISTINCT seasonNumber) FROM episodes WHERE seriesId = :seriesId")
    suspend fun getSeasonCountBySeries(seriesId: Long): Int
}
