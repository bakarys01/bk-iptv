package com.bkiptv.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * DataStore-based settings storage
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        // Appearance
        val DARK_MODE = stringPreferencesKey("dark_mode") // "system", "on", "off"
        
        // Playback
        val AUTO_PLAY = booleanPreferencesKey("auto_play")
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality") // "auto", "1080p", "720p", etc.
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        
        // Sync
        val AUTO_REFRESH = booleanPreferencesKey("auto_refresh")
        val REFRESH_INTERVAL_HOURS = intPreferencesKey("refresh_interval_hours")
        val EPG_AUTO_REFRESH = booleanPreferencesKey("epg_auto_refresh")
        
        // Profile
        val CURRENT_PROFILE_ID = longPreferencesKey("current_profile_id")
        
        // Parental
        val PARENTAL_PIN = stringPreferencesKey("parental_pin")
        val PARENTAL_ENABLED = booleanPreferencesKey("parental_enabled")
    }

    // Dark mode
    val darkMode: Flow<String> = dataStore.data.map { it[DARK_MODE] ?: "system" }
    suspend fun setDarkMode(value: String) {
        dataStore.edit { it[DARK_MODE] = value }
    }

    // Auto play
    val autoPlay: Flow<Boolean> = dataStore.data.map { it[AUTO_PLAY] ?: true }
    suspend fun setAutoPlay(value: Boolean) {
        dataStore.edit { it[AUTO_PLAY] = value }
    }

    // Resume playback
    val resumePlayback: Flow<Boolean> = dataStore.data.map { it[RESUME_PLAYBACK] ?: true }
    suspend fun setResumePlayback(value: Boolean) {
        dataStore.edit { it[RESUME_PLAYBACK] = value }
    }

    // Auto refresh
    val autoRefresh: Flow<Boolean> = dataStore.data.map { it[AUTO_REFRESH] ?: true }
    suspend fun setAutoRefresh(value: Boolean) {
        dataStore.edit { it[AUTO_REFRESH] = value }
    }

    // Refresh interval
    val refreshIntervalHours: Flow<Int> = dataStore.data.map { it[REFRESH_INTERVAL_HOURS] ?: 6 }
    suspend fun setRefreshIntervalHours(value: Int) {
        dataStore.edit { it[REFRESH_INTERVAL_HOURS] = value }
    }

    // Current profile
    val currentProfileId: Flow<Long> = dataStore.data.map { it[CURRENT_PROFILE_ID] ?: 0L }
    suspend fun setCurrentProfileId(value: Long) {
        dataStore.edit { it[CURRENT_PROFILE_ID] = value }
    }

    // Parental controls
    val parentalEnabled: Flow<Boolean> = dataStore.data.map { it[PARENTAL_ENABLED] ?: false }
    suspend fun setParentalEnabled(value: Boolean) {
        dataStore.edit { it[PARENTAL_ENABLED] = value }
    }

    val parentalPin: Flow<String?> = dataStore.data.map { it[PARENTAL_PIN] }
    suspend fun setParentalPin(value: String?) {
        dataStore.edit {
            if (value != null) {
                it[PARENTAL_PIN] = value
            } else {
                it.remove(PARENTAL_PIN)
            }
        }
    }
}
