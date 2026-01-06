package com.bkiptv.app.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.bkiptv.app.data.repository.PlaylistRepository
import com.bkiptv.app.db.dao.PlaylistDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for periodic playlist synchronization
 */
@HiltWorker
class PlaylistSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val playlistRepository: PlaylistRepository,
    private val playlistDao: PlaylistDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Get all enabled playlists with auto-refresh
            val playlists = playlistDao.getEnabledPlaylists().first()
            
            var hasErrors = false
            for (playlist in playlists.filter { it.isAutoRefresh }) {
                val result = playlistRepository.syncPlaylist(playlist.id)
                if (result.isFailure) {
                    hasErrors = true
                }
            }

            if (hasErrors) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "playlist_sync_work"

        /**
         * Schedule periodic playlist sync
         */
        fun schedule(context: Context, intervalHours: Long = 6) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<PlaylistSyncWorker>(
                intervalHours, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1, TimeUnit.HOURS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }

        /**
         * Cancel scheduled sync
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /**
         * Trigger immediate sync
         */
        fun syncNow(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<PlaylistSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
