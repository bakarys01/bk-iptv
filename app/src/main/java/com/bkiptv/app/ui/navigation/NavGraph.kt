package com.bkiptv.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bkiptv.app.ui.screens.home.HomeScreen
import com.bkiptv.app.ui.screens.livetv.LiveTvScreen
import com.bkiptv.app.ui.screens.movies.MoviesScreen
import com.bkiptv.app.ui.screens.series.SeriesScreen
import com.bkiptv.app.ui.screens.player.PlayerScreen
import com.bkiptv.app.ui.screens.search.SearchScreen
import com.bkiptv.app.ui.screens.favorites.FavoritesScreen
import com.bkiptv.app.ui.screens.settings.SettingsScreen
import com.bkiptv.app.ui.screens.playlist.PlaylistScreen
import com.bkiptv.app.ui.screens.playlist.AddPlaylistScreen
import com.bkiptv.app.ui.screens.qrscanner.QRScannerScreen

/**
 * Main navigation host for the app
 */
@Composable
fun BKIPTVNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally { it / 4 } },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally { it / 4 } }
    ) {
        // Home
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToLiveTv = { navController.navigate(Screen.LiveTv.route) },
                onNavigateToMovies = { navController.navigate(Screen.Movies.route) },
                onNavigateToSeries = { navController.navigate(Screen.Series.route) },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onPlayChannel = { channelId -> 
                    navController.navigate(Screen.PlayerChannel.createRoute(channelId))
                },
                onPlayMovie = { movieId ->
                    navController.navigate(Screen.PlayerMovie.createRoute(movieId))
                }
            )
        }

        // Live TV
        composable(Screen.LiveTv.route) {
            LiveTvScreen(
                onNavigateBack = { navController.popBackStack() },
                onPlayChannel = { channelId ->
                    navController.navigate(Screen.PlayerChannel.createRoute(channelId))
                }
            )
        }

        // Movies
        composable(Screen.Movies.route) {
            MoviesScreen(
                onNavigateBack = { navController.popBackStack() },
                onPlayMovie = { movieId ->
                    navController.navigate(Screen.PlayerMovie.createRoute(movieId))
                }
            )
        }

        // Series
        composable(Screen.Series.route) {
            SeriesScreen(
                onNavigateBack = { navController.popBackStack() },
                onPlayEpisode = { episodeId ->
                    navController.navigate(Screen.PlayerEpisode.createRoute(episodeId))
                }
            )
        }

        // Player - Channel
        composable(
            route = Screen.PlayerChannel.route,
            arguments = listOf(navArgument("channelId") { type = NavType.LongType })
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getLong("channelId") ?: return@composable
            PlayerScreen(
                channelId = channelId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Player - Movie
        composable(
            route = Screen.PlayerMovie.route,
            arguments = listOf(navArgument("movieId") { type = NavType.LongType })
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getLong("movieId") ?: return@composable
            PlayerScreen(
                movieId = movieId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Player - Episode
        composable(
            route = Screen.PlayerEpisode.route,
            arguments = listOf(navArgument("episodeId") { type = NavType.LongType })
        ) { backStackEntry ->
            val episodeId = backStackEntry.arguments?.getLong("episodeId") ?: return@composable
            PlayerScreen(
                episodeId = episodeId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Search
        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onPlayChannel = { channelId ->
                    navController.navigate(Screen.PlayerChannel.createRoute(channelId))
                },
                onPlayMovie = { movieId ->
                    navController.navigate(Screen.PlayerMovie.createRoute(movieId))
                }
            )
        }

        // Favorites
        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onNavigateBack = { navController.popBackStack() },
                onPlayChannel = { channelId ->
                    navController.navigate(Screen.PlayerChannel.createRoute(channelId))
                },
                onPlayMovie = { movieId ->
                    navController.navigate(Screen.PlayerMovie.createRoute(movieId))
                }
            )
        }

        // Settings
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlaylists = { navController.navigate(Screen.Playlists.route) },
                onNavigateToProfiles = { navController.navigate(Screen.Profiles.route) }
            )
        }

        // Playlists
        composable(Screen.Playlists.route) {
            PlaylistScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddPlaylist = { navController.navigate(Screen.AddPlaylist.route) },
                onScanQR = { navController.navigate(Screen.QRScanner.route) }
            )
        }

        // Add Playlist
        composable(Screen.AddPlaylist.route) {
            AddPlaylistScreen(
                onNavigateBack = { navController.popBackStack() },
                onScanQR = { navController.navigate(Screen.QRScanner.route) }
            )
        }

        // QR Scanner
        composable(Screen.QRScanner.route) {
            QRScannerScreen(
                onNavigateBack = { navController.popBackStack() },
                onQRScanned = { url ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("scanned_url", url)
                    navController.popBackStack()
                }
            )
        }
    }
}
