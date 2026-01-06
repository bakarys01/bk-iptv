package com.bkiptv.app.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.bkiptv.app.data.repository.EPGRepository
import com.bkiptv.app.db.dao.PlaylistDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for periodic EPG synchronization
 */
@HiltWorker
class EPGSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val epgRepository: EPGRepository,
    private val playlistDao: PlaylistDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Get all enabled playlists with EPG URLs
            val playlists = playlistDao.getEnabledPlaylists().first()
                .filter { it.epgUrl != null }
            
            var hasErrors = false
            for (playlist in playlists) {
                playlist.epgUrl?.let { epgUrl ->
                    val result = epgRepository.syncEPG(epgUrl)
                    if (result.isFailure) {
                        hasErrors = true
                    }
                }
            }

            // Clean old EPG data
            epgRepository.cleanOldData()

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
        const val WORK_NAME = "epg_sync_work"

        /**
         * Schedule periodic EPG sync
         */
        fun schedule(context: Context, intervalHours: Long = 12) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<EPGSyncWorker>(
                intervalHours, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    2, TimeUnit.HOURS
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
            val workRequest = OneTimeWorkRequestBuilder<EPGSyncWorker>()
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
