package com.bkiptv.app.ui.tv

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bkiptv.app.ui.navigation.Screen

/**
 * Navigation host for Android TV
 * Uses Compose TV components for DPAD navigation
 */
@Composable
fun TvNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            TvHomeScreen(
                onNavigateToLiveTv = { navController.navigate(Screen.LiveTv.route) },
                onNavigateToMovies = { navController.navigate(Screen.Movies.route) },
                onNavigateToSeries = { navController.navigate(Screen.Series.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onPlayChannel = { channelId -> 
                    navController.navigate(Screen.PlayerChannel.createRoute(channelId))
                }
            )
        }

        // Other TV-specific routes would be added here
        // For now, TV uses same screens as mobile with TV theme
    }
}
