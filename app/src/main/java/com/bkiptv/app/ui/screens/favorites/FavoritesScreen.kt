package com.bkiptv.app.ui.screens.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bkiptv.app.db.dao.FavoriteDao
import com.bkiptv.app.db.entity.FavoriteEntity
import com.bkiptv.app.ui.components.*
import com.bkiptv.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onPlayChannel: (Long) -> Unit,
    onPlayMovie: (Long) -> Unit
) {
    val favorites by viewModel.favorites.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favoris") },
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
        if (favorites.isEmpty()) {
            EmptyState(
                title = "Aucun favori",
                subtitle = "Ajoutez des chaînes ou films à vos favoris",
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(favorites, key = { it.id }) { favorite ->
                    FavoriteItem(
                        favorite = favorite,
                        onClick = {
                            when (favorite.contentType) {
                                "CHANNEL" -> onPlayChannel(favorite.contentId)
                                "MOVIE" -> onPlayMovie(favorite.contentId)
                            }
                        },
                        onRemove = { viewModel.removeFavorite(favorite) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteItem(
    favorite: FavoriteEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        ListItem(
            headlineContent = { Text(favorite.name, color = TextPrimary) },
            supportingContent = { Text(favorite.contentType, color = TextSecondary) },
            leadingContent = {
                coil.compose.AsyncImage(
                    model = favorite.logoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            },
            trailingContent = {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Retirer",
                        tint = Error
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = SurfaceDark)
        )
    }
}

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteDao: FavoriteDao
) : ViewModel() {

    val favorites: StateFlow<List<FavoriteEntity>> = favoriteDao.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeFavorite(favorite: FavoriteEntity) {
        viewModelScope.launch {
            favoriteDao.deleteFavorite(favorite)
        }
    }
}
