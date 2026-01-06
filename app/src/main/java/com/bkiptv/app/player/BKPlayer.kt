package com.bkiptv.app.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BKPlayer - Premium Media Player wrapper around Media3/ExoPlayer
 * 
 * Features:
 * - Optimized buffering for smooth playback
 * - HTTP header injection for IPTV streams
 * - Multi-quality support with track selection
 * - Smart error handling with retry logic
 * - Background audio support
 */
@Singleton
@OptIn(UnstableApi::class)
class BKPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer?
        get() = _exoPlayer

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _currentMedia = MutableStateFlow<MediaInfo?>(null)
    val currentMedia: StateFlow<MediaInfo?> = _currentMedia.asStateFlow()

    private var retryCount = 0
    private val maxRetries = 3
    private var lastUrl: String? = null
    private var lastHeaders: Map<String, String>? = null

    /**
     * Initialize the player instance
     */
    fun initialize() {
        if (_exoPlayer != null) return

        // Configure track selector for quality switching
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoSizeSd() // Start with SD for faster initial load
                    .setPreferredAudioLanguage("fr") // French audio preferred
            )
        }

        // Configure load control for optimized buffering
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000,  // Min buffer
                50_000,  // Max buffer
                2_500,   // Buffer for playback
                5_000    // Buffer for rebuffer
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // Audio attributes for proper focus handling
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        _exoPlayer = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekForwardIncrementMs(10_000)
            .setSeekBackIncrementMs(10_000)
            .build()

        _exoPlayer?.addListener(playerListener)
    }

    /**
     * Play a stream URL with optional headers
     */
    fun play(url: String, headers: Map<String, String> = emptyMap(), title: String? = null) {
        if (_exoPlayer == null) initialize()

        lastUrl = url
        lastHeaders = headers
        retryCount = 0

        val dataSourceFactory = createDataSourceFactory(headers)
        val mediaSource = createMediaSource(url, dataSourceFactory)

        _currentMedia.value = MediaInfo(
            url = url,
            title = title,
            headers = headers
        )

        _exoPlayer?.apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }

        _playerState.value = _playerState.value.copy(isLoading = true)
    }

    /**
     * Create data source factory with custom headers
     */
    private fun createDataSourceFactory(headers: Map<String, String>): DefaultDataSource.Factory {
        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setDefaultRequestProperties(headers.toMutableMap().apply {
                if (!containsKey("User-Agent")) {
                    put("User-Agent", "BK IPTV/1.0")
                }
            })

        return DefaultDataSource.Factory(context, httpDataSourceFactory)
    }

    /**
     * Create appropriate media source based on URL type
     */
    private fun createMediaSource(
        url: String,
        dataSourceFactory: DefaultDataSource.Factory
    ): MediaSource {
        val mediaItem = MediaItem.fromUri(url)

        return when {
            url.contains(".m3u8", ignoreCase = true) || 
            url.contains("/hls/", ignoreCase = true) ||
            url.contains("format=m3u8", ignoreCase = true) -> {
                HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            }
            else -> {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
        }
    }

    /**
     * Player event listener
     */
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _playerState.value = _playerState.value.copy(
                isLoading = playbackState == Player.STATE_BUFFERING,
                isPlaying = _exoPlayer?.isPlaying == true,
                isEnded = playbackState == Player.STATE_ENDED
            )

            if (playbackState == Player.STATE_READY) {
                retryCount = 0 // Reset retry count on success
                updateQualityOptions()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
        }

        override fun onPlayerError(error: PlaybackException) {
            handleError(error)
        }
    }

    /**
     * Handle playback errors with retry logic
     */
    private fun handleError(error: PlaybackException) {
        val shouldRetry = when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> true
            else -> false
        }

        if (shouldRetry && retryCount < maxRetries) {
            retryCount++
            _playerState.value = _playerState.value.copy(
                isLoading = true,
                error = "Reconnecting... ($retryCount/$maxRetries)"
            )
            
            // Retry with exponential backoff
            _exoPlayer?.let { player ->
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    lastUrl?.let { url ->
                        play(url, lastHeaders ?: emptyMap(), _currentMedia.value?.title)
                    }
                }, (retryCount * 1000L))
            }
        } else {
            _playerState.value = _playerState.value.copy(
                isLoading = false,
                error = getErrorMessage(error)
            )
        }
    }

    /**
     * Get user-friendly error message
     */
    private fun getErrorMessage(error: PlaybackException): String {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> 
                "Erreur réseau - Vérifiez votre connexion"
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> 
                "Délai d'attente dépassé"
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> 
                "Flux non disponible"
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> 
                "Format de flux invalide"
            else -> "Erreur de lecture"
        }
    }

    /**
     * Update available quality options
     */
    private fun updateQualityOptions() {
        val trackSelector = (_exoPlayer?.trackSelector as? DefaultTrackSelector) ?: return
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo ?: return

        val qualities = mutableListOf<QualityOption>()

        for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
            if (mappedTrackInfo.getRendererType(rendererIndex) == C.TRACK_TYPE_VIDEO) {
                val trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex)
                for (groupIndex in 0 until trackGroups.length) {
                    val group = trackGroups.get(groupIndex)
                    for (trackIndex in 0 until group.length) {
                        val format = group.getFormat(trackIndex)
                        val label = when {
                            format.height >= 2160 -> "4K"
                            format.height >= 1080 -> "1080p"
                            format.height >= 720 -> "720p"
                            format.height >= 480 -> "480p"
                            format.height >= 360 -> "360p"
                            else -> "${format.height}p"
                        }
                        qualities.add(QualityOption(label, format.height, rendererIndex, groupIndex, trackIndex))
                    }
                }
            }
        }

        _playerState.value = _playerState.value.copy(
            qualities = qualities.distinctBy { it.height }.sortedByDescending { it.height }
        )
    }

    /**
     * Set specific quality
     */
    fun setQuality(quality: QualityOption) {
        val trackSelector = (_exoPlayer?.trackSelector as? DefaultTrackSelector) ?: return
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo ?: return
        
        val trackGroups = mappedTrackInfo.getTrackGroups(quality.rendererIndex)
        val override = TrackSelectionOverride(
            trackGroups.get(quality.groupIndex),
            quality.trackIndex
        )
        
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                .addOverride(override)
        )
    }

    /**
     * Set auto quality
     */
    fun setAutoQuality() {
        val trackSelector = (_exoPlayer?.trackSelector as? DefaultTrackSelector) ?: return
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                .setMaxVideoSizeSd()
        )
    }

    // Playback controls
    fun play() { _exoPlayer?.play() }
    fun pause() { _exoPlayer?.pause() }
    fun togglePlayPause() { if (_exoPlayer?.isPlaying == true) pause() else play() }
    fun seekTo(position: Long) { _exoPlayer?.seekTo(position) }
    fun seekForward() { _exoPlayer?.seekForward() }
    fun seekBack() { _exoPlayer?.seekBack() }
    fun stop() { _exoPlayer?.stop() }
    
    fun getCurrentPosition(): Long = _exoPlayer?.currentPosition ?: 0L
    fun getDuration(): Long = _exoPlayer?.duration ?: 0L
    fun getBufferedPercentage(): Int = _exoPlayer?.bufferedPercentage ?: 0

    /**
     * Release player resources
     */
    fun release() {
        _exoPlayer?.removeListener(playerListener)
        _exoPlayer?.release()
        _exoPlayer = null
        _playerState.value = PlayerState()
        _currentMedia.value = null
    }
}

/**
 * Player state data class
 */
data class PlayerState(
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val isEnded: Boolean = false,
    val error: String? = null,
    val qualities: List<QualityOption> = emptyList()
)

/**
 * Current media information
 */
data class MediaInfo(
    val url: String,
    val title: String? = null,
    val headers: Map<String, String> = emptyMap()
)

/**
 * Quality option for quality switching
 */
data class QualityOption(
    val label: String,
    val height: Int,
    val rendererIndex: Int,
    val groupIndex: Int,
    val trackIndex: Int
)
