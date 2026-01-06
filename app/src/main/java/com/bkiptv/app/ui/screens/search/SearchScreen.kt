package com.bkiptv.app.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bkiptv.app.data.repository.ChannelRepository
import com.bkiptv.app.db.dao.MovieDao
import com.bkiptv.app.db.dao.SeriesDao
import com.bkiptv.app.db.entity.ChannelEntity
import com.bkiptv.app.db.entity.MovieEntity
import com.bkiptv.app.db.entity.SeriesEntity
import com.bkiptv.app.ui.components.*
import com.bkiptv.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onPlayChannel: (Long) -> Unit,
    onPlayMovie: (Long) -> Unit
) {
    val query by viewModel.query.collectAsState()
    val channels by viewModel.channelResults.collectAsState()
    val movies by viewModel.movieResults.collectAsState()
    val series by viewModel.seriesResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rechercher") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search field
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.updateQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Rechercher chaînes, films, séries...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateQuery("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Effacer")
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = TextTertiary
                )
            )

            if (isSearching) {
                LoadingIndicator()
            } else if (query.isEmpty()) {
                EmptyState(
                    title = "Rechercher",
                    subtitle = "Entrez un terme pour rechercher"
                )
            } else if (channels.isEmpty() && movies.isEmpty() && series.isEmpty()) {
                EmptyState(
                    title = "Aucun résultat",
                    subtitle = "Essayez avec d'autres termes"
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // Channels
                    if (channels.isNotEmpty()) {
                        item {
                            Text(
                                "Chaînes (${channels.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        items(channels.take(5)) { channel ->
                            ChannelListItem(
                                channel = channel,
                                onClick = { onPlayChannel(channel.id) }
                            )
                        }
                    }

                    // Movies
                    if (movies.isNotEmpty()) {
                        item {
                            Text(
                                "Films (${movies.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        items(movies.take(5)) { movie ->
                            MovieListItem(
                                movie = movie,
                                onClick = { onPlayMovie(movie.id) }
                            )
                        }
                    }

                    // Series
                    if (series.isNotEmpty()) {
                        item {
                            Text(
                                "Séries (${series.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        items(series.take(5)) { s ->
                            SeriesListItem(series = s, onClick = {})
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieListItem(movie: MovieEntity, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(movie.title, color = TextPrimary) },
        supportingContent = { movie.genre?.let { Text(it, color = TextSecondary) } },
        leadingContent = {
            coil.compose.AsyncImage(
                model = movie.posterUrl,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = SurfaceDark)
    )
}

@Composable
private fun SeriesListItem(series: SeriesEntity, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(series.name, color = TextPrimary) },
        supportingContent = { Text("${series.seasonCount} saisons", color = TextSecondary) },
        leadingContent = {
            coil.compose.AsyncImage(
                model = series.posterUrl,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = SurfaceDark)
    )
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val movieDao: MovieDao,
    private val seriesDao: SeriesDao
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    val channelResults: StateFlow<List<ChannelEntity>> = _query
        .debounce(300)
        .filter { it.length >= 2 }
        .flatMapLatest { q ->
            _isSearching.value = true
            channelRepository.searchChannels(q)
        }
        .onEach { _isSearching.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val movieResults: StateFlow<List<MovieEntity>> = _query
        .debounce(300)
        .filter { it.length >= 2 }
        .flatMapLatest { movieDao.searchMovies(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val seriesResults: StateFlow<List<SeriesEntity>> = _query
        .debounce(300)
        .filter { it.length >= 2 }
        .flatMapLatest { seriesDao.searchSeries(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateQuery(newQuery: String) {
        _query.value = newQuery
    }
}
