package com.bkiptv.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bkiptv.app.R
import com.bkiptv.app.ui.components.*
import com.bkiptv.app.ui.theme.*

/**
 * Home screen - main dashboard showing all content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToLiveTv: () -> Unit,
    onNavigateToMovies: () -> Unit,
    onNavigateToSeries: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onPlayChannel: (Long) -> Unit,
    onPlayMovie: (Long) -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val recentHistory by viewModel.recentHistory.collectAsState()
    val popularChannels by viewModel.popularChannels.collectAsState()
    val allMovies by viewModel.allMovies.collectAsState()
    val allSeries by viewModel.allSeries.collectAsState()
    val hasContent by viewModel.hasContent.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "BK IPTV",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Primary
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Rechercher")
                    }
                    IconButton(onClick = onNavigateToFavorites) {
                        Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Favoris")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Paramètres")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!hasContent && playlists.isEmpty()) {
                // Empty state - no playlists
                EmptyState(
                    title = "Bienvenue sur BK IPTV",
                    subtitle = "Ajoutez une playlist pour commencer",
                    action = {
                        Button(onClick = onNavigateToSettings) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ajouter une playlist")
                        }
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // Quick access category cards
                    item {
                        QuickAccessRow(
                            onLiveTvClick = onNavigateToLiveTv,
                            onMoviesClick = onNavigateToMovies,
                            onSeriesClick = onNavigateToSeries
                        )
                    }

                    // Continue watching (from history)
                    if (recentHistory.isNotEmpty()) {
                        item {
                            ContentRow(
                                title = stringResource(R.string.home_continue_watching),
                                items = recentHistory.take(10)
                            ) { history ->
                                // Display as wide card based on content type
                                ContinueWatchingCard(
                                    name = history.name,
                                    logoUrl = history.logoUrl,
                                    progress = if (history.duration > 0) {
                                        (history.playPosition.toFloat() / history.duration)
                                    } else 0f,
                                    onClick = {
                                        when (history.contentType) {
                                            "CHANNEL" -> onPlayChannel(history.contentId)
                                            "MOVIE" -> onPlayMovie(history.contentId)
                                            else -> {}
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Live TV Channels
                    if (popularChannels.isNotEmpty()) {
                        item {
                            ContentRow(
                                title = stringResource(R.string.nav_live_tv),
                                items = popularChannels,
                                onSeeAllClick = onNavigateToLiveTv
                            ) { channel ->
                                ChannelCard(
                                    channel = channel,
                                    onClick = { onPlayChannel(channel.id) }
                                )
                            }
                        }
                    }

                    // Movies
                    if (allMovies.isNotEmpty()) {
                        item {
                            ContentRow(
                                title = stringResource(R.string.nav_movies),
                                items = allMovies,
                                onSeeAllClick = onNavigateToMovies
                            ) { movie ->
                                MovieCard(
                                    movie = movie,
                                    onClick = { onPlayMovie(movie.id) }
                                )
                            }
                        }
                    }

                    // Series
                    if (allSeries.isNotEmpty()) {
                        item {
                            ContentRow(
                                title = stringResource(R.string.nav_series),
                                items = allSeries,
                                onSeeAllClick = onNavigateToSeries
                            ) { series ->
                                SeriesCard(
                                    series = series,
                                    onClick = { /* Navigate to series detail */ }
                                )
                            }
                        }
                    }
                }
            }

            // Loading overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Pull to refresh could be added here
        }
    }
}

@Composable
private fun QuickAccessRow(
    onLiveTvClick: () -> Unit,
    onMoviesClick: () -> Unit,
    onSeriesClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickAccessCard(
            title = "TV en Direct",
            icon = Icons.Filled.LiveTv,
            gradientColors = listOf(CategoryLiveTv, Primary),
            onClick = onLiveTvClick,
            modifier = Modifier.weight(1f)
        )
        QuickAccessCard(
            title = "Films",
            icon = Icons.Filled.Movie,
            gradientColors = listOf(CategoryMovies, GradientEnd),
            onClick = onMoviesClick,
            modifier = Modifier.weight(1f)
        )
        QuickAccessCard(
            title = "Séries",
            icon = Icons.Filled.Tv,
            gradientColors = listOf(CategorySeries, Secondary),
            onClick = onSeriesClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAccessCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(gradientColors)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContinueWatchingCard(
    name: String,
    logoUrl: String?,
    progress: Float,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(240.dp)
            .height(140.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            coil.compose.AsyncImage(
                model = logoUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )
            
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
            
            if (progress > 0) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = Primary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeriesCard(
    series: com.bkiptv.app.db.entity.SeriesEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .height(210.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            coil.compose.AsyncImage(
                model = series.posterUrl,
                contentDescription = series.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 300f
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = series.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 2
                )
                Text(
                    text = "${series.seasonCount} saisons",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}
