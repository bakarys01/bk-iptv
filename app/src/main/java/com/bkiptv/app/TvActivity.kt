package com.bkiptv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.bkiptv.app.ui.tv.TvNavHost
import com.bkiptv.app.ui.theme.BKIPTVTvTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for Android TV devices
 * Uses Compose TV (tv-material) for proper DPAD navigation
 */
@AndroidEntryPoint
class TvActivity : ComponentActivity() {

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BKIPTVTvTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    TvNavHost()
                }
            }
        }
    }
}
