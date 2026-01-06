package com.bkiptv.app.db.dao

import androidx.room.*
import com.bkiptv.app.db.entity.MovieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    @Query("SELECT * FROM movies ORDER BY title")
    fun getAllMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE playlistId = :playlistId ORDER BY title")
    fun getMoviesByPlaylist(playlistId: Long): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE genre LIKE '%' || :genre || '%' ORDER BY title")
    fun getMoviesByGenre(genre: String): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE country = :country ORDER BY title")
    fun getMoviesByCountry(country: String): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE year = :year ORDER BY title")
    fun getMoviesByYear(year: Int): Flow<List<MovieEntity>>

    @Query("SELECT DISTINCT genre FROM movies WHERE genre IS NOT NULL ORDER BY genre")
    fun getAllGenres(): Flow<List<String>>

    @Query("SELECT DISTINCT country FROM movies WHERE country IS NOT NULL ORDER BY country")
    fun getAllCountries(): Flow<List<String>>

    @Query("SELECT DISTINCT year FROM movies WHERE year IS NOT NULL ORDER BY year DESC")
    fun getAllYears(): Flow<List<Int>>

    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun getMovieById(id: Long): MovieEntity?

    @Query("SELECT * FROM movies WHERE isFavorite = 1 ORDER BY title")
    fun getFavoriteMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE lastPlayPosition > 0 ORDER BY lastWatchedAt DESC LIMIT :limit")
    fun getRecentlyWatchedMovies(limit: Int = 20): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE title LIKE '%' || :query || '%' ORDER BY title")
    fun searchMovies(query: String): Flow<List<MovieEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: MovieEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<MovieEntity>)

    @Update
    suspend fun updateMovie(movie: MovieEntity)

    @Delete
    suspend fun deleteMovie(movie: MovieEntity)

    @Query("DELETE FROM movies WHERE playlistId = :playlistId")
    suspend fun deleteMoviesByPlaylist(playlistId: Long)

    @Query("UPDATE movies SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE movies SET lastPlayPosition = :position, lastWatchedAt = :timestamp WHERE id = :id")
    suspend fun updatePlayProgress(id: Long, position: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM movies WHERE playlistId = :playlistId")
    suspend fun getMovieCountByPlaylist(playlistId: Long): Int
}
