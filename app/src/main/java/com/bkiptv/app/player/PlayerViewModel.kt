package com.bkiptv.app.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bkiptv.app.data.repository.ChannelRepository
import com.bkiptv.app.data.repository.EPGRepository
import com.bkiptv.app.db.entity.ChannelEntity
import com.bkiptv.app.db.entity.EPGProgramEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for player screen
 * Manages playback state, channel info, and EPG data
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val bkPlayer: BKPlayer,
    private val channelRepository: ChannelRepository,
    private val epgRepository: EPGRepository,
    private val gson: Gson
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = bkPlayer.playerState
    val currentMedia: StateFlow<MediaInfo?> = bkPlayer.currentMedia

    private val _currentChannel = MutableStateFlow<ChannelEntity?>(null)
    val currentChannel: StateFlow<ChannelEntity?> = _currentChannel.asStateFlow()

    private val _currentProgram = MutableStateFlow<EPGProgramEntity?>(null)
    val currentProgram: StateFlow<EPGProgramEntity?> = _currentProgram.asStateFlow()

    private val _nextProgram = MutableStateFlow<EPGProgramEntity?>(null)
    val nextProgram: StateFlow<EPGProgramEntity?> = _nextProgram.asStateFlow()

    private val _showControls = MutableStateFlow(true)
    val showControls: StateFlow<Boolean> = _showControls.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    init {
        bkPlayer.initialize()
    }

    /**
     * Play a channel
     */
    fun playChannel(channel: ChannelEntity) {
        _currentChannel.value = channel

        // Parse headers from JSON
        val headers = channel.headers?.let {
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                gson.fromJson<Map<String, String>>(it, type)
            } catch (e: Exception) {
                emptyMap()
            }
        } ?: emptyMap()

        bkPlayer.play(channel.streamUrl, headers, channel.name)

        // Update last watched
        viewModelScope.launch {
            channelRepository.updateLastWatched(channel.id)
        }

        // Load EPG data
        channel.tvgId?.let { tvgId ->
            loadEPGData(tvgId)
        }
    }

    /**
     * Play a URL directly
     */
    fun playUrl(url: String, title: String? = null, headers: Map<String, String> = emptyMap()) {
        _currentChannel.value = null
        _currentProgram.value = null
        _nextProgram.value = null
        bkPlayer.play(url, headers, title)
    }

    /**
     * Load EPG data for current channel
     */
    private fun loadEPGData(tvgId: String) {
        viewModelScope.launch {
            _currentProgram.value = epgRepository.getCurrentProgram(tvgId)
            _nextProgram.value = epgRepository.getNextProgram(tvgId)
        }
    }

    // Playback controls
    fun togglePlayPause() = bkPlayer.togglePlayPause()
    fun play() = bkPlayer.play()
    fun pause() = bkPlayer.pause()
    fun seekTo(position: Long) = bkPlayer.seekTo(position)
    fun seekForward() = bkPlayer.seekForward()
    fun seekBack() = bkPlayer.seekBack()
    fun stop() = bkPlayer.stop()

    // UI controls
    fun toggleControls() {
        _showControls.value = !_showControls.value
    }

    fun showControls() {
        _showControls.value = true
    }

    fun hideControls() {
        _showControls.value = false
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    // Quality
    fun setQuality(quality: QualityOption) = bkPlayer.setQuality(quality)
    fun setAutoQuality() = bkPlayer.setAutoQuality()

    // Position
    fun getCurrentPosition(): Long = bkPlayer.getCurrentPosition()
    fun getDuration(): Long = bkPlayer.getDuration()
    fun getBufferedPercentage(): Int = bkPlayer.getBufferedPercentage()

    // Player instance for Compose
    fun getExoPlayer() = bkPlayer.exoPlayer

    override fun onCleared() {
        super.onCleared()
        bkPlayer.release()
    }
}
