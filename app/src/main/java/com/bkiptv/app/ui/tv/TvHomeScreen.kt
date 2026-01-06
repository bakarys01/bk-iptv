package com.bkiptv.app.ui.tv

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import com.bkiptv.app.db.entity.ChannelEntity
import com.bkiptv.app.ui.screens.home.HomeViewModel
import com.bkiptv.app.ui.theme.*

/**
 * Home screen for Android TV
 * Optimized for DPAD navigation with large touch targets
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvHomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToLiveTv: () -> Unit,
    onNavigateToMovies: () -> Unit,
    onNavigateToSeries: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onPlayChannel: (Long) -> Unit
) {
    val channels by viewModel.popularChannels.collectAsState()
    val movies by viewModel.allMovies.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp)
    ) {
        // App title
        Text(
            text = "BK IPTV",
            style = androidx.compose.material3.MaterialTheme.typography.displayMedium,
            color = Primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Quick navigation cards
        TvLazyRow(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            item {
                TvNavigationCard(
                    title = "TV en Direct",
                    onClick = onNavigateToLiveTv
                )
            }
            item {
                TvNavigationCard(
                    title = "Films",
                    onClick = onNavigateToMovies
                )
            }
            item {
                TvNavigationCard(
                    title = "Séries",
                    onClick = onNavigateToSeries
                )
            }
            item {
                TvNavigationCard(
                    title = "Paramètres",
                    onClick = onNavigateToSettings
                )
            }
        }

        // Channels row
        if (channels.isNotEmpty()) {
            Text(
                text = "Chaînes populaires",
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            TvLazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(channels) { channel ->
                    TvChannelCard(
                        channel = channel,
                        onClick = { onPlayChannel(channel.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvNavigationCard(
    title: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .height(120.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvChannelCard(
    channel: ChannelEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(180.dp)
            .height(140.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            coil.compose.AsyncImage(
                model = channel.logoUrl,
                contentDescription = channel.name,
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = channel.name,
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 2
            )
        }
    }
}
