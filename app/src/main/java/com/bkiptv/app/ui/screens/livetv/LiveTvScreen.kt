package com.bkiptv.app.ui.screens.livetv

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bkiptv.app.db.entity.ChannelEntity
import com.bkiptv.app.ui.components.*
import com.bkiptv.app.ui.theme.*

/**
 * Live TV screen with country/category/channel navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTvScreen(
    viewModel: LiveTvViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onPlayChannel: (Long) -> Unit
) {
    val channels by viewModel.channels.collectAsState()
    val countries by viewModel.countries.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TV en Direct") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    // View mode toggle
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (viewMode == ViewMode.GRID) Icons.Filled.ViewList else Icons.Filled.GridView,
                            contentDescription = "Changer de vue"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark
                )
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter chips
            FilterChipsRow(
                countries = countries,
                categories = categories,
                selectedCountry = selectedCountry,
                selectedCategory = selectedCategory,
                onCountrySelected = { viewModel.selectCountry(it) },
                onCategorySelected = { viewModel.selectCategory(it) }
            )

            // Channels
            if (isLoading) {
                LoadingIndicator()
            } else if (filteredChannels.isEmpty()) {
                EmptyState(
                    title = "Aucune chaîne",
                    subtitle = "Aucune chaîne ne correspond aux filtres"
                )
            } else {
                when (viewMode) {
                    ViewMode.GRID -> {
                        ChannelGrid(
                            channels = filteredChannels,
                            onChannelClick = { onPlayChannel(it.id) },
                            onFavoriteClick = { viewModel.toggleFavorite(it.id) }
                        )
                    }
                    ViewMode.LIST -> {
                        ChannelList(
                            channels = filteredChannels,
                            onChannelClick = { onPlayChannel(it.id) },
                            onFavoriteClick = { viewModel.toggleFavorite(it.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipsRow(
    countries: List<String>,
    categories: List<String>,
    selectedCountry: String?,
    selectedCategory: String?,
    onCountrySelected: (String?) -> Unit,
    onCategorySelected: (String?) -> Unit
) {
    Column(modifier = Modifier.padding(8.dp)) {
        // Countries
        if (countries.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCountry == null,
                        onClick = { onCountrySelected(null) },
                        label = { Text("Tous") }
                    )
                }
                items(countries) { country ->
                    FilterChip(
                        selected = selectedCountry == country,
                        onClick = { onCountrySelected(country) },
                        label = { Text(country) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Categories
        if (categories.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onCategorySelected(null) },
                        label = { Text("Toutes") }
                    )
                }
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onCategorySelected(category) },
                        label = { Text(category) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelGrid(
    channels: List<ChannelEntity>,
    onChannelClick: (ChannelEntity) -> Unit,
    onFavoriteClick: (ChannelEntity) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(channels, key = { it.id }) { channel ->
            ChannelCard(
                channel = channel,
                onClick = { onChannelClick(channel) },
                onFavoriteClick = { onFavoriteClick(channel) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ChannelList(
    channels: List<ChannelEntity>,
    onChannelClick: (ChannelEntity) -> Unit,
    onFavoriteClick: (ChannelEntity) -> Unit
) {
    LazyColumn {
        items(channels, key = { it.id }) { channel ->
            ChannelListItem(
                channel = channel,
                onClick = { onChannelClick(channel) },
                onFavoriteClick = { onFavoriteClick(channel) }
            )
            Divider(color = SurfaceDarkVariant.copy(alpha = 0.5f))
        }
    }
}

enum class ViewMode {
    GRID, LIST
}
