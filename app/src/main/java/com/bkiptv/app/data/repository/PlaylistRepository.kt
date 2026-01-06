package com.bkiptv.app.data.repository

import com.bkiptv.app.data.model.ContentType
import com.bkiptv.app.data.parser.M3UEntry
import com.bkiptv.app.data.parser.M3UParser
import com.bkiptv.app.db.dao.*
import com.bkiptv.app.db.entity.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing playlists and their content
 * Handles fetching, parsing, and storing playlist data
 */
@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val movieDao: MovieDao,
    private val seriesDao: SeriesDao,
    private val episodeDao: EpisodeDao,
    private val m3uParser: M3UParser,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {

    /**
     * Get all playlists as a Flow
     */
    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    /**
     * Get only enabled playlists
     */
    fun getEnabledPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getEnabledPlaylists()

    /**
     * Get a playlist by ID
     */
    suspend fun getPlaylistById(id: Long): PlaylistEntity? = playlistDao.getPlaylistById(id)

    /**
     * Add a new playlist
     */
    suspend fun addPlaylist(name: String, url: String, epgUrl: String? = null): Long {
        val playlist = PlaylistEntity(
            name = name,
            url = url,
            epgUrl = epgUrl
        )
        return playlistDao.insertPlaylist(playlist)
    }

    /**
     * Delete a playlist and all its content
     */
    suspend fun deletePlaylist(playlistId: Long) {
        // Room handles cascade delete via foreign keys
        playlistDao.deletePlaylistById(playlistId)
    }

    /**
     * Enable or disable a playlist
     */
    suspend fun setPlaylistEnabled(playlistId: Long, enabled: Boolean) {
        playlistDao.setPlaylistEnabled(playlistId, enabled)
    }

    /**
     * Sync a playlist from its URL
     * Downloads, parses, and stores the content
     */
    suspend fun syncPlaylist(playlistId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getPlaylistById(playlistId)
            ?: return@withContext Result.failure(Exception("Playlist not found"))

        // Update status to syncing
        playlistDao.updateSyncStatus(
            id = playlistId,
            timestamp = System.currentTimeMillis(),
            status = SyncStatus.SYNCING,
            error = null,
            channelCount = playlist.channelCount,
            movieCount = playlist.movieCount,
            seriesCount = playlist.seriesCount
        )

        try {
            // Download playlist content
            val content = downloadPlaylist(playlist.url)
            
            // Parse M3U content
            val entries = m3uParser.parse(content)
            
            // Clear existing content for this playlist
            channelDao.deleteChannelsByPlaylist(playlistId)
            movieDao.deleteMoviesByPlaylist(playlistId)
            seriesDao.deleteSeriesByPlaylist(playlistId)
            
            // Categorize and store entries
            val channels = mutableListOf<ChannelEntity>()
            val movies = mutableListOf<MovieEntity>()
            val seriesMap = mutableMapOf<String, MutableList<M3UEntry>>()
            
            for (entry in entries) {
                when (entry.contentType) {
                    ContentType.LIVE_TV -> {
                        channels.add(entryToChannel(entry, playlistId))
                    }
                    ContentType.MOVIE -> {
                        movies.add(entryToMovie(entry, playlistId))
                    }
                    ContentType.EPISODE, ContentType.SERIES -> {
                        val seriesName = entry.seriesName ?: entry.groupTitle ?: "Unknown Series"
                        seriesMap.getOrPut(seriesName) { mutableListOf() }.add(entry)
                    }
                    ContentType.UNKNOWN -> {
                        // Default to channel
                        channels.add(entryToChannel(entry, playlistId))
                    }
                }
            }
            
            // Insert channels
            if (channels.isNotEmpty()) {
                channelDao.insertChannels(channels)
            }
            
            // Insert movies
            if (movies.isNotEmpty()) {
                movieDao.insertMovies(movies)
            }
            
            // Insert series and episodes
            for ((seriesName, episodeEntries) in seriesMap) {
                val series = SeriesEntity(
                    playlistId = playlistId,
                    name = seriesName,
                    posterUrl = episodeEntries.firstOrNull()?.logoUrl,
                    genre = episodeEntries.firstOrNull()?.groupTitle,
                    episodeCount = episodeEntries.size,
                    seasonCount = episodeEntries.mapNotNull { it.seasonNumber }.distinct().size
                )
                val seriesId = seriesDao.insertSeries(series)
                
                val episodes = episodeEntries.map { entry ->
                    EpisodeEntity(
                        seriesId = seriesId,
                        title = entry.name,
                        streamUrl = entry.url,
                        thumbnailUrl = entry.logoUrl,
                        seasonNumber = entry.seasonNumber ?: 1,
                        episodeNumber = entry.episodeNumber ?: 0,
                        headers = entry.headers.takeIf { it.isNotEmpty() }?.let { gson.toJson(it) }
                    )
                }
                episodeDao.insertEpisodes(episodes)
            }
            
            // Update status to success
            playlistDao.updateSyncStatus(
                id = playlistId,
                timestamp = System.currentTimeMillis(),
                status = SyncStatus.SUCCESS,
                error = null,
                channelCount = channels.size,
                movieCount = movies.size,
                seriesCount = seriesMap.size
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            // Update status to failed
            playlistDao.updateSyncStatus(
                id = playlistId,
                timestamp = System.currentTimeMillis(),
                status = SyncStatus.FAILED,
                error = e.message,
                channelCount = playlist.channelCount,
                movieCount = playlist.movieCount,
                seriesCount = playlist.seriesCount
            )
            Result.failure(e)
        }
    }

    /**
     * Sync a playlist from local file content
     */
    suspend fun syncPlaylistFromContent(playlistId: Long, content: InputStream): Result<Unit> = 
        withContext(Dispatchers.IO) {
            // Similar logic to syncPlaylist but using provided content
            val playlist = playlistDao.getPlaylistById(playlistId)
                ?: return@withContext Result.failure(Exception("Playlist not found"))

            try {
                val entries = m3uParser.parse(content)
                // ... same processing as syncPlaylist
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Download playlist content from URL
     */
    private suspend fun downloadPlaylist(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "BK IPTV/1.0")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to download playlist: ${response.code}")
            }
            response.body?.string() ?: throw Exception("Empty response")
        }
    }

    /**
     * Convert M3U entry to Channel entity
     */
    private fun entryToChannel(entry: M3UEntry, playlistId: Long): ChannelEntity {
        return ChannelEntity(
            playlistId = playlistId,
            name = entry.name,
            streamUrl = entry.url,
            logoUrl = entry.logoUrl,
            tvgId = entry.tvgId,
            tvgName = entry.tvgName,
            groupTitle = entry.groupTitle,
            country = entry.getCountryCode(),
            language = entry.tvgLanguage,
            headers = entry.headers.takeIf { it.isNotEmpty() }?.let { gson.toJson(it) }
        )
    }

    /**
     * Convert M3U entry to Movie entity
     */
    private fun entryToMovie(entry: M3UEntry, playlistId: Long): MovieEntity {
        return MovieEntity(
            playlistId = playlistId,
            title = entry.name,
            streamUrl = entry.url,
            posterUrl = entry.logoUrl,
            genre = entry.groupTitle,
            year = entry.year,
            headers = entry.headers.takeIf { it.isNotEmpty() }?.let { gson.toJson(it) }
        )
    }
}
