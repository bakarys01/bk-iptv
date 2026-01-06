package com.bkiptv.app.data.repository

import com.bkiptv.app.db.dao.ChannelDao
import com.bkiptv.app.db.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for channel operations
 */
@Singleton
class ChannelRepository @Inject constructor(
    private val channelDao: ChannelDao
) {
    fun getAllChannels(): Flow<List<ChannelEntity>> = channelDao.getAllChannels()

    fun getChannelsByPlaylist(playlistId: Long): Flow<List<ChannelEntity>> = 
        channelDao.getChannelsByPlaylist(playlistId)

    fun getChannelsByGroup(group: String): Flow<List<ChannelEntity>> = 
        channelDao.getChannelsByGroup(group)

    fun getChannelsByCountry(country: String): Flow<List<ChannelEntity>> = 
        channelDao.getChannelsByCountry(country)

    fun getAllCountries(): Flow<List<String>> = channelDao.getAllCountries()

    fun getAllGroups(): Flow<List<String>> = channelDao.getAllGroups()

    fun getAllCategories(): Flow<List<String>> = channelDao.getAllCategories()

    fun getFavoriteChannels(): Flow<List<ChannelEntity>> = channelDao.getFavoriteChannels()

    fun searchChannels(query: String): Flow<List<ChannelEntity>> = channelDao.searchChannels(query)

    suspend fun getChannelById(id: Long): ChannelEntity? = channelDao.getChannelById(id)

    suspend fun getChannelByTvgId(tvgId: String): ChannelEntity? = channelDao.getChannelByTvgId(tvgId)

    suspend fun toggleFavorite(channelId: Long) {
        val channel = channelDao.getChannelById(channelId)
        channel?.let {
            channelDao.setFavorite(channelId, !it.isFavorite)
        }
    }

    suspend fun updateLastWatched(channelId: Long) {
        channelDao.updateLastWatched(channelId)
    }
}
