package com.bkiptv.app.data.repository

import com.bkiptv.app.data.parser.XMLTVParser
import com.bkiptv.app.db.dao.EPGDao
import com.bkiptv.app.db.entity.EPGProgramEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for EPG (Electronic Program Guide) operations
 */
@Singleton
class EPGRepository @Inject constructor(
    private val epgDao: EPGDao,
    private val xmltvParser: XMLTVParser,
    private val okHttpClient: OkHttpClient
) {
    /**
     * Get upcoming programs for a channel
     */
    fun getUpcomingPrograms(channelId: String): Flow<List<EPGProgramEntity>> =
        epgDao.getUpcomingPrograms(channelId)

    /**
     * Get currently playing program
     */
    suspend fun getCurrentProgram(channelId: String): EPGProgramEntity? =
        epgDao.getCurrentProgram(channelId)

    /**
     * Get next program
     */
    suspend fun getNextProgram(channelId: String): EPGProgramEntity? =
        epgDao.getNextProgram(channelId)

    /**
     * Get programs in a time range for timeline view
     */
    fun getProgramsInRange(
        channelId: String,
        startTime: Long,
        endTime: Long
    ): Flow<List<EPGProgramEntity>> =
        epgDao.getProgramsInRange(channelId, startTime, endTime)

    /**
     * Get all programs in a time range (for EPG grid)
     */
    fun getAllProgramsInRange(startTime: Long, endTime: Long): Flow<List<EPGProgramEntity>> =
        epgDao.getAllProgramsInRange(startTime, endTime)

    /**
     * Search programs by title
     */
    fun searchPrograms(query: String): Flow<List<EPGProgramEntity>> =
        epgDao.searchPrograms(query)

    /**
     * Sync EPG data from URL
     */
    suspend fun syncEPG(epgUrl: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(epgUrl)
                .header("User-Agent", "BK IPTV/1.0")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to download EPG: ${response.code}"))
                }

                val inputStream = response.body?.byteStream()
                    ?: return@withContext Result.failure(Exception("Empty EPG response"))

                syncEPGFromStream(inputStream)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sync EPG from input stream (for local files)
     */
    suspend fun syncEPGFromStream(inputStream: InputStream): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val parseResult = xmltvParser.parse(inputStream)

            // Delete old programs first
            epgDao.deleteOldPrograms()

            // Convert and insert programs
            val programs = parseResult.programs.map { entry ->
                EPGProgramEntity(
                    channelId = entry.channelId,
                    title = entry.title,
                    description = entry.description,
                    category = entry.category,
                    subTitle = entry.subTitle,
                    episodeNum = entry.episodeNum,
                    icon = entry.icon,
                    rating = entry.rating,
                    startTime = entry.startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    endTime = entry.endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                )
            }

            if (programs.isNotEmpty()) {
                epgDao.insertPrograms(programs)
            }

            Result.success(programs.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clean old EPG data
     */
    suspend fun cleanOldData() {
        epgDao.deleteOldPrograms()
    }

    /**
     * Get EPG statistics
     */
    suspend fun getStats(): EPGStats {
        return EPGStats(
            programCount = epgDao.getProgramCount(),
            earliestTime = epgDao.getEarliestProgramTime(),
            latestTime = epgDao.getLatestProgramTime()
        )
    }

    data class EPGStats(
        val programCount: Int,
        val earliestTime: Long?,
        val latestTime: Long?
    )
}
