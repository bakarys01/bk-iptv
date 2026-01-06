package com.bkiptv.app.db.dao

import androidx.room.*
import com.bkiptv.app.db.entity.FavoriteEntity
import com.bkiptv.app.db.entity.HistoryEntity
import com.bkiptv.app.db.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites WHERE profileId = :profileId ORDER BY sortOrder, createdAt DESC")
    fun getFavorites(profileId: Long = 0): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE profileId = :profileId AND contentType = :type ORDER BY sortOrder")
    fun getFavoritesByType(profileId: Long = 0, type: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE contentId = :contentId AND contentType = :type AND profileId = :profileId LIMIT 1")
    suspend fun getFavorite(contentId: Long, type: String, profileId: Long = 0): FavoriteEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE contentId = :contentId AND contentType = :type AND profileId = :profileId)")
    suspend fun isFavorite(contentId: Long, type: String, profileId: Long = 0): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity): Long

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE contentId = :contentId AND contentType = :type AND profileId = :profileId")
    suspend fun deleteFavoriteByContent(contentId: Long, type: String, profileId: Long = 0)

    @Query("DELETE FROM favorites WHERE profileId = :profileId")
    suspend fun clearFavorites(profileId: Long = 0)

    @Query("SELECT COUNT(*) FROM favorites WHERE profileId = :profileId")
    suspend fun getFavoriteCount(profileId: Long = 0): Int
}

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history WHERE profileId = :profileId ORDER BY watchedAt DESC")
    fun getHistory(profileId: Long = 0): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE profileId = :profileId ORDER BY watchedAt DESC LIMIT :limit")
    fun getRecentHistory(profileId: Long = 0, limit: Int = 20): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE profileId = :profileId AND contentType = :type ORDER BY watchedAt DESC")
    fun getHistoryByType(profileId: Long = 0, type: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE contentId = :contentId AND contentType = :type AND profileId = :profileId LIMIT 1")
    suspend fun getHistoryEntry(contentId: Long, type: String, profileId: Long = 0): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity): Long

    @Update
    suspend fun updateHistory(history: HistoryEntity)

    @Query("UPDATE history SET playPosition = :position, watchedAt = :timestamp WHERE id = :id")
    suspend fun updatePlayPosition(id: Long, position: Long, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteHistory(history: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM history WHERE profileId = :profileId")
    suspend fun clearHistory(profileId: Long = 0)

    @Query("SELECT COUNT(*) FROM history WHERE profileId = :profileId")
    suspend fun getHistoryCount(profileId: Long = 0): Int
}

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY isDefault DESC, name")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultProfile(): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE name = :name LIMIT 1")
    suspend fun getProfileByName(name: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Long)

    @Query("UPDATE profiles SET isDefault = 0")
    suspend fun clearDefaultProfile()

    @Query("UPDATE profiles SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultProfile(id: Long)

    @Query("UPDATE profiles SET pin = :pin, isPinRequired = :required, updatedAt = :timestamp WHERE id = :id")
    suspend fun updatePin(id: Long, pin: String?, required: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun getProfileCount(): Int
}
