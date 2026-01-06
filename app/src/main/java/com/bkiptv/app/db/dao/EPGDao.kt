package com.bkiptv.app.db.dao

import androidx.room.*
import com.bkiptv.app.db.entity.EPGProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EPGDao {

    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId AND endTime > :currentTime ORDER BY startTime")
    fun getUpcomingPrograms(channelId: String, currentTime: Long = System.currentTimeMillis()): Flow<List<EPGProgramEntity>>

    @Query("""
        SELECT * FROM epg_programs 
        WHERE channelId = :channelId 
        AND startTime <= :currentTime 
        AND endTime > :currentTime 
        LIMIT 1
    """)
    suspend fun getCurrentProgram(channelId: String, currentTime: Long = System.currentTimeMillis()): EPGProgramEntity?

    @Query("""
        SELECT * FROM epg_programs 
        WHERE channelId = :channelId 
        AND startTime > :currentTime 
        ORDER BY startTime 
        LIMIT 1
    """)
    suspend fun getNextProgram(channelId: String, currentTime: Long = System.currentTimeMillis()): EPGProgramEntity?

    @Query("""
        SELECT * FROM epg_programs 
        WHERE channelId = :channelId 
        AND startTime >= :startTime 
        AND endTime <= :endTime 
        ORDER BY startTime
    """)
    fun getProgramsInRange(channelId: String, startTime: Long, endTime: Long): Flow<List<EPGProgramEntity>>

    @Query("""
        SELECT * FROM epg_programs 
        WHERE startTime >= :startTime 
        AND endTime <= :endTime 
        ORDER BY channelId, startTime
    """)
    fun getAllProgramsInRange(startTime: Long, endTime: Long): Flow<List<EPGProgramEntity>>

    @Query("SELECT * FROM epg_programs WHERE id = :id")
    suspend fun getProgramById(id: Long): EPGProgramEntity?

    @Query("SELECT * FROM epg_programs WHERE title LIKE '%' || :query || '%' ORDER BY startTime")
    fun searchPrograms(query: String): Flow<List<EPGProgramEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgram(program: EPGProgramEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<EPGProgramEntity>)

    @Delete
    suspend fun deleteProgram(program: EPGProgramEntity)

    @Query("DELETE FROM epg_programs WHERE channelId = :channelId")
    suspend fun deleteProgramsByChannel(channelId: String)

    @Query("DELETE FROM epg_programs WHERE endTime < :timestamp")
    suspend fun deleteOldPrograms(timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM epg_programs")
    suspend fun deleteAllPrograms()

    @Query("SELECT COUNT(*) FROM epg_programs")
    suspend fun getProgramCount(): Int

    @Query("SELECT MIN(startTime) FROM epg_programs")
    suspend fun getEarliestProgramTime(): Long?

    @Query("SELECT MAX(endTime) FROM epg_programs")
    suspend fun getLatestProgramTime(): Long?
}
