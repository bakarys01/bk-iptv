package com.bkiptv.app.ui.screens.movies

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bkiptv.app.db.dao.MovieDao
import com.bkiptv.app.db.entity.MovieEntity
import com.bkiptv.app.ui.components.*
import com.bkiptv.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesScreen(
    viewModel: MoviesViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onPlayMovie: (Long) -> Unit
) {
    val movies by viewModel.movies.collectAsState()
    val genres by viewModel.genres.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Films") },
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
            // Genre filter
            if (genres.isNotEmpty()) {
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(1),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedGenre == null,
                            onClick = { viewModel.selectGenre(null) },
                            label = { Text("Tous") }
                        )
                    }
                    items(genres) { genre ->
                        FilterChip(
                            selected = selectedGenre == genre,
                            onClick = { viewModel.selectGenre(genre) },
                            label = { Text(genre) }
                        )
                    }
                }
            }

            if (isLoading) {
                LoadingIndicator()
            } else if (movies.isEmpty()) {
                EmptyState(title = "Aucun film", subtitle = "Importez une playlist contenant des films")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 130.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(movies, key = { it.id }) { movie ->
                        MovieCard(
                            movie = movie,
                            onClick = { onPlayMovie(movie.id) },
                            onFavoriteClick = { viewModel.toggleFavorite(movie.id) }
                        )
                    }
                }
            }
        }
    }
}

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val movieDao: MovieDao
) : ViewModel() {

    val movies: StateFlow<List<MovieEntity>> = movieDao.getAllMovies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val genres: StateFlow<List<String>> = movieDao.getAllGenres()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun selectGenre(genre: String?) {
        _selectedGenre.value = genre
    }

    fun toggleFavorite(movieId: Long) {
        viewModelScope.launch {
            movieDao.getMovieById(movieId)?.let { movie ->
                movieDao.setFavorite(movieId, !movie.isFavorite)
            }
        }
    }
}
