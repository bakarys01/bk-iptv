package com.bkiptv.app.data.xtream

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Xtream Codes API Client
 * Handles communication with Xtream-compatible IPTV servers
 */
@Singleton
class XtreamApiClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    
    /**
     * Xtream credentials
     */
    data class XtreamCredentials(
        val server: String,
        val username: String,
        val password: String
    ) {
        val baseUrl: String
            get() = server.trimEnd('/')
        
        val playerApiUrl: String
            get() = "$baseUrl/player_api.php"
        
        fun getLiveStreamUrl(streamId: Int, extension: String = "ts"): String =
            "$baseUrl/$username/$password/$streamId.$extension"
        
        fun getVodStreamUrl(streamId: Int, extension: String): String =
            "$baseUrl/movie/$username/$password/$streamId.$extension"
        
        fun getSeriesStreamUrl(streamId: Int, extension: String): String =
            "$baseUrl/series/$username/$password/$streamId.$extension"
    }
    
    /**
     * Authenticate and get user/server info
     */
    suspend fun authenticate(credentials: XtreamCredentials): Result<XtreamUserInfo> = 
        withContext(Dispatchers.IO) {
            try {
                val url = "${credentials.playerApiUrl}?username=${credentials.username}&password=${credentials.password}"
                val response = makeRequest(url)
                val userInfo = gson.fromJson(response, XtreamUserInfo::class.java)
                
                if (userInfo.userInfo?.auth == 1 || userInfo.userInfo?.status == "Active") {
                    Result.success(userInfo)
                } else {
                    Result.failure(Exception("Authentification échouée: ${userInfo.userInfo?.message ?: "Identifiants invalides"}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Erreur de connexion au serveur: ${e.message}"))
            }
        }
    
    /**
     * Get live TV categories
     */
    suspend fun getLiveCategories(credentials: XtreamCredentials): Result<List<XtreamCategory>> =
        withContext(Dispatchers.IO) {
            try {
                val url = "${credentials.playerApiUrl}?username=${credentials.username}&password=${credentials.password}&action=get_live_categories"
                val response = makeRequest(url)
                val type = object : TypeToken<List<XtreamCategory>>() {}.type
                val categories: List<XtreamCategory> = gson.fromJson(response, type)
                Result.success(categories)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * Get all live streams
     */
    suspend fun getLiveStreams(credentials: XtreamCredentials): Result<List<XtreamLiveStream>> =
        withContext(Dispatchers.IO) {
            try {
                val url = "${credentials.playerApiUrl}?username=${credentials.username}&password=${credentials.password}&action=get_live_streams"
                val response = makeRequest(url)
                val type = object : TypeToken<List<XtreamLiveStream>>() {}.type
                val streams: List<XtreamLiveStream> = gson.fromJson(response, type)
                Result.success(streams)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * Get VOD categories
     */
    suspend fun getVodCategories(credentials: XtreamCredentials): Result<List<XtreamCategory>> =
        withContext(Dispatchers.IO) {
            try {
                val url = "${credentials.playerApiUrl}?username=${credentials.username}&password=${credentials.password}&action=get_vod_categories"
                val response = makeRequest(url)
                val type = object : TypeToken<List<XtreamCategory>>() {}.type
                val categories: List<XtreamCategory> = gson.fromJson(response, type)
                Result.success(categories)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * Get all VOD streams (movies)
     */
    suspend fun getVodStreams(credentials: XtreamCredentials): Result<List<XtreamVodStream>> =
        withContext(Dispatchers.IO) {
            try {
                val url = "${credentials.playerApiUrl}?username=${credentials.username}&password=${credentials.password}&action=get_vod_streams"
                val response = makeRequest(url)
                val type = object : TypeToken<List<XtreamVodStream>>() {}.type
                val streams: List<XtreamVodStream> = gson.fromJson(response, type)
                Result.success(streams)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * Get series categories
     */
    suspend fun getSeriesCategories(credentials: XtreamCredentials): Result<List<XtreamCategory>> =
        withContext(Dispatchers.IO) {
            try {
                val url = "${credentials.playerApiUrl}?username=${credentials.username}&password=${credentials.password}&action=get_series_categories"
                val response = makeRequest(url)
                val type = object : TypeToken<List<XtreamCategory>>() {}.type
                val categories: List<XtreamCategory> = gson.fromJson(response, type)
                Result.success(categories)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * Get all series
     */
    suspend fun getSeries(credentials: XtreamCredentials): Result<List<XtreamSeries>> =
        withContext(Dispatchers.IO) {
            try {
                val url = "${credentials.playerApiUrl}?username=${credentials.username}&password=${credentials.password}&action=get_series"
                val response = makeRequest(url)
                val type = object : TypeToken<List<XtreamSeries>>() {}.type
                val series: List<XtreamSeries> = gson.fromJson(response, type)
                Result.success(series)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * Get series info with episodes
     */
    suspend fun getSeriesInfo(credentials: XtreamCredentials, seriesId: Int): Result<XtreamSeriesInfo> =
        withContext(Dispatchers.IO) {
            try {
                val url = "${credentials.playerApiUrl}?username=${credentials.username}&password=${credentials.password}&action=get_series_info&series_id=$seriesId"
                val response = makeRequest(url)
                val seriesInfo = gson.fromJson(response, XtreamSeriesInfo::class.java)
                Result.success(seriesInfo)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * Make HTTP request
     */
    private fun makeRequest(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "BK IPTV/1.0")
            .build()
        
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Erreur serveur: ${response.code}")
            }
            return response.body?.string() ?: throw Exception("Réponse vide")
        }
    }
}
