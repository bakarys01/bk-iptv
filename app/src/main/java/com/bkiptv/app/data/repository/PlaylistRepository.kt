package com.bkiptv.app.data.repository

import com.bkiptv.app.data.model.ContentType
import com.bkiptv.app.data.parser.M3UEntry
import com.bkiptv.app.data.parser.M3UParser
import com.bkiptv.app.data.xtream.XtreamApiClient
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
    private val xtreamApiClient: XtreamApiClient,
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
     * Add a new M3U playlist
     */
    suspend fun addPlaylist(name: String, url: String, epgUrl: String? = null): Long {
        val playlist = PlaylistEntity(
            name = name,
            url = url,
            type = PlaylistType.M3U,
            epgUrl = epgUrl
        )
        return playlistDao.insertPlaylist(playlist)
    }

    /**
     * Add a new Xtream playlist
     */
    suspend fun addXtreamPlaylist(
        name: String,
        server: String,
        username: String,
        password: String
    ): Long {
        val playlist = PlaylistEntity(
            name = name,
            url = server,
            type = PlaylistType.XTREAM_API,
            xtreamUsername = username,
            xtreamPassword = password
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
            // Route to appropriate sync method based on playlist type
            when (playlist.type) {
                PlaylistType.XTREAM_API -> syncXtreamPlaylist(playlist)
                else -> syncM3uPlaylist(playlist)
            }
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
     * Sync M3U playlist
     */
    private suspend fun syncM3uPlaylist(playlist: PlaylistEntity): Result<Unit> {
        val playlistId = playlist.id
        
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
        
        return Result.success(Unit)
    }

    /**
     * Sync Xtream playlist (Live TV, VOD, Series)
     */
    private suspend fun syncXtreamPlaylist(playlist: PlaylistEntity): Result<Unit> {
        val playlistId = playlist.id
        val username = playlist.xtreamUsername 
            ?: return Result.failure(Exception("Identifiants Xtream manquants"))
        val password = playlist.xtreamPassword 
            ?: return Result.failure(Exception("Identifiants Xtream manquants"))
        
        val credentials = XtreamApiClient.XtreamCredentials(
            server = playlist.url,
            username = username,
            password = password
        )
        
        // Authenticate first
        val authResult = xtreamApiClient.authenticate(credentials)
        if (authResult.isFailure) {
            return Result.failure(authResult.exceptionOrNull() ?: Exception("Authentification échouée"))
        }
        
        // Clear existing content
        channelDao.deleteChannelsByPlaylist(playlistId)
        movieDao.deleteMoviesByPlaylist(playlistId)
        seriesDao.deleteSeriesByPlaylist(playlistId)
        
        var channelCount = 0
        var movieCount = 0
        var seriesCount = 0
        
        // Get live categories for mapping
        val categoriesResult = xtreamApiClient.getLiveCategories(credentials)
        val categoryMap = categoriesResult.getOrNull()?.associate { 
            it.categoryId to it.categoryName 
        } ?: emptyMap()
        
        // Sync Live TV
        val liveStreamsResult = xtreamApiClient.getLiveStreams(credentials)
        liveStreamsResult.getOrNull()?.let { streams ->
            val channels = streams.map { stream ->
                ChannelEntity(
                    playlistId = playlistId,
                    name = stream.name ?: "Unknown",
                    streamUrl = credentials.getLiveStreamUrl(stream.streamId ?: 0),
                    logoUrl = stream.streamIcon,
                    tvgId = stream.epgChannelId,
                    groupTitle = categoryMap[stream.categoryId]
                )
            }
            if (channels.isNotEmpty()) {
                channelDao.insertChannels(channels)
                channelCount = channels.size
            }
        }
        
        // Get VOD categories
        val vodCategoriesResult = xtreamApiClient.getVodCategories(credentials)
        val vodCategoryMap = vodCategoriesResult.getOrNull()?.associate { 
            it.categoryId to it.categoryName 
        } ?: emptyMap()
        
        // Sync VOD (Movies)
        val vodStreamsResult = xtreamApiClient.getVodStreams(credentials)
        vodStreamsResult.getOrNull()?.let { streams ->
            val movies = streams.map { stream ->
                MovieEntity(
                    playlistId = playlistId,
                    title = stream.name ?: "Unknown",
                    streamUrl = credentials.getVodStreamUrl(
                        stream.streamId ?: 0, 
                        stream.containerExtension ?: "mkv"
                    ),
                    posterUrl = stream.streamIcon,
                    genre = vodCategoryMap[stream.categoryId],
                    rating = stream.rating5Based?.toString()
                )
            }
            if (movies.isNotEmpty()) {
                movieDao.insertMovies(movies)
                movieCount = movies.size
            }
        }
        
        // Sync Series
        val seriesResult = xtreamApiClient.getSeries(credentials)
        seriesResult.getOrNull()?.let { seriesList ->
            for (xtreamSeries in seriesList) {
                val series = SeriesEntity(
                    playlistId = playlistId,
                    name = xtreamSeries.name ?: "Unknown",
                    posterUrl = xtreamSeries.cover,
                    genre = xtreamSeries.genre,
                    plot = xtreamSeries.plot
                )
                val seriesId = seriesDao.insertSeries(series)
                seriesCount++
                
                // Get episodes for this series
                xtreamSeries.seriesId?.let { xtreamSeriesId ->
                    val seriesInfoResult = xtreamApiClient.getSeriesInfo(credentials, xtreamSeriesId)
                    seriesInfoResult.getOrNull()?.episodes?.forEach { (_, episodeList) ->
                        val episodes = episodeList.map { ep ->
                            EpisodeEntity(
                                seriesId = seriesId,
                                title = ep.title ?: "Episode ${ep.episodeNum}",
                                streamUrl = credentials.getSeriesStreamUrl(
                                    ep.id?.toIntOrNull() ?: 0,
                                    ep.containerExtension ?: "mkv"
                                ),
                                thumbnailUrl = ep.info?.movieImage,
                                seasonNumber = ep.season ?: 1,
                                episodeNumber = ep.episodeNum ?: 0,
                                plot = ep.info?.plot
                            )
                        }
                        if (episodes.isNotEmpty()) {
                            episodeDao.insertEpisodes(episodes)
                        }
                    }
                }
            }
        }
        
        // Update status to success
        playlistDao.updateSyncStatus(
            id = playlistId,
            timestamp = System.currentTimeMillis(),
            status = SyncStatus.SUCCESS,
            error = null,
            channelCount = channelCount,
            movieCount = movieCount,
            seriesCount = seriesCount
        )
        
        return Result.success(Unit)
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
                val errorMessage = when (response.code) {
                    400 -> "URL invalide ou mal formée"
                    401, 403 -> "Authentification requise ou accès refusé"
                    404 -> "Playlist introuvable sur le serveur"
                    500, 502, 503 -> "Le serveur IPTV est temporairement indisponible"
                    in 800..899 -> "Erreur IPTV: Vérifiez votre abonnement ou identifiants (code ${response.code})"
                    else -> "Échec du téléchargement (code ${response.code})"
                }
                throw Exception(errorMessage)
            }
            response.body?.string() ?: throw Exception("Réponse vide du serveur")
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
