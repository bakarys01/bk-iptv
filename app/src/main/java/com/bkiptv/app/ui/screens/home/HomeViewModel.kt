package com.bkiptv.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bkiptv.app.data.repository.ChannelRepository
import com.bkiptv.app.data.repository.PlaylistRepository
import com.bkiptv.app.db.dao.MovieDao
import com.bkiptv.app.db.dao.SeriesDao
import com.bkiptv.app.db.dao.HistoryDao
import com.bkiptv.app.db.entity.ChannelEntity
import com.bkiptv.app.db.entity.MovieEntity
import com.bkiptv.app.db.entity.SeriesEntity
import com.bkiptv.app.db.entity.HistoryEntity
import com.bkiptv.app.db.entity.PlaylistEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the home screen
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val channelRepository: ChannelRepository,
    private val movieDao: MovieDao,
    private val seriesDao: SeriesDao,
    private val historyDao: HistoryDao
) : ViewModel() {

    // Playlists
    val playlists: StateFlow<List<PlaylistEntity>> = playlistRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recently watched
    val recentHistory: StateFlow<List<HistoryEntity>> = historyDao.getRecentHistory(limit = 10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Popular channels (by group)
    val popularChannels: StateFlow<List<ChannelEntity>> = channelRepository.getAllChannels()
        .map { it.take(20) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recent movies
    val recentMovies: StateFlow<List<MovieEntity>> = movieDao.getRecentlyWatchedMovies(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All movies for browsing
    val allMovies: StateFlow<List<MovieEntity>> = movieDao.getAllMovies()
        .map { it.take(20) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Series
    val allSeries: StateFlow<List<SeriesEntity>> = seriesDao.getAllSeries()
        .map { it.take(20) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Countries for quick access
    val countries: StateFlow<List<String>> = channelRepository.getAllCountries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Categories
    val categories: StateFlow<List<String>> = channelRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Content counts
    val hasContent: StateFlow<Boolean> = combine(
        popularChannels,
        allMovies,
        allSeries
    ) { channels, movies, series ->
        channels.isNotEmpty() || movies.isNotEmpty() || series.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * Refresh all playlists
     */
    fun refreshPlaylists() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                playlists.value.filter { it.isEnabled }.forEach { playlist ->
                    playlistRepository.syncPlaylist(playlist.id)
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
