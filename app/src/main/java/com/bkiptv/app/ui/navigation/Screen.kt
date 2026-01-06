package com.bkiptv.app.ui.navigation

/**
 * Screen destinations for navigation
 */
sealed class Screen(val route: String) {
    // Main screens
    object Splash : Screen("splash")
    object Home : Screen("home")
    
    // Content screens
    object LiveTv : Screen("live_tv")
    object LiveTvCountry : Screen("live_tv/{country}") {
        fun createRoute(country: String) = "live_tv/$country"
    }
    object LiveTvCategory : Screen("live_tv/{country}/{category}") {
        fun createRoute(country: String, category: String) = "live_tv/$country/$category"
    }
    
    object Movies : Screen("movies")
    object MovieDetails : Screen("movie/{movieId}") {
        fun createRoute(movieId: Long) = "movie/$movieId"
    }
    
    object Series : Screen("series")
    object SeriesDetails : Screen("series/{seriesId}") {
        fun createRoute(seriesId: Long) = "series/$seriesId"
    }
    object SeasonEpisodes : Screen("series/{seriesId}/season/{seasonNumber}") {
        fun createRoute(seriesId: Long, seasonNumber: Int) = "series/$seriesId/season/$seasonNumber"
    }
    
    // Player
    object Player : Screen("player")
    object PlayerChannel : Screen("player/channel/{channelId}") {
        fun createRoute(channelId: Long) = "player/channel/$channelId"
    }
    object PlayerMovie : Screen("player/movie/{movieId}") {
        fun createRoute(movieId: Long) = "player/movie/$movieId"
    }
    object PlayerEpisode : Screen("player/episode/{episodeId}") {
        fun createRoute(episodeId: Long) = "player/episode/$episodeId"
    }
    
    // Utility
    object Search : Screen("search")
    object Favorites : Screen("favorites")
    object History : Screen("history")
    object EPG : Screen("epg")
    
    // Settings
    object Settings : Screen("settings")
    object Playlists : Screen("playlists")
    object AddPlaylist : Screen("playlists/add")
    object QRScanner : Screen("qr_scanner")
    object Profiles : Screen("profiles")
    object AddProfile : Screen("profiles/add")
    object EditProfile : Screen("profiles/{profileId}") {
        fun createRoute(profileId: Long) = "profiles/$profileId"
    }
}
