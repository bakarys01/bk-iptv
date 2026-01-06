package com.bkiptv.app.di

import com.bkiptv.app.player.BKPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    // BKPlayer is already @Singleton and uses @Inject constructor
    // so Hilt can provide it directly. This module can be used
    // for any additional player-related dependencies if needed.
}
