package com.bkiptv.app.data.xtream

import com.google.gson.annotations.SerializedName

/**
 * Xtream Codes API data classes
 */

// User info response
data class XtreamUserInfo(
    @SerializedName("user_info") val userInfo: UserInfo?,
    @SerializedName("server_info") val serverInfo: ServerInfo?
)

data class UserInfo(
    val username: String?,
    val password: String?,
    val message: String?,
    val auth: Int?,
    val status: String?,
    @SerializedName("exp_date") val expDate: String?,
    @SerializedName("is_trial") val isTrial: String?,
    @SerializedName("active_cons") val activeCons: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("max_connections") val maxConnections: String?,
    @SerializedName("allowed_output_formats") val allowedOutputFormats: List<String>?
)

data class ServerInfo(
    val url: String?,
    val port: String?,
    @SerializedName("https_port") val httpsPort: String?,
    @SerializedName("server_protocol") val serverProtocol: String?,
    @SerializedName("rtmp_port") val rtmpPort: String?,
    val timezone: String?,
    @SerializedName("timestamp_now") val timestampNow: Long?,
    @SerializedName("time_now") val timeNow: String?
)

// Category
data class XtreamCategory(
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("parent_id") val parentId: Int?
)

// Live stream
data class XtreamLiveStream(
    val num: Int?,
    val name: String?,
    @SerializedName("stream_type") val streamType: String?,
    @SerializedName("stream_id") val streamId: Int?,
    @SerializedName("stream_icon") val streamIcon: String?,
    @SerializedName("epg_channel_id") val epgChannelId: String?,
    val added: String?,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("custom_sid") val customSid: String?,
    @SerializedName("tv_archive") val tvArchive: Int?,
    @SerializedName("direct_source") val directSource: String?,
    @SerializedName("tv_archive_duration") val tvArchiveDuration: Int?
)

// VOD stream (Movie)
data class XtreamVodStream(
    val num: Int?,
    val name: String?,
    @SerializedName("stream_type") val streamType: String?,
    @SerializedName("stream_id") val streamId: Int?,
    @SerializedName("stream_icon") val streamIcon: String?,
    val rating: String?,
    @SerializedName("rating_5based") val rating5Based: Double?,
    val added: String?,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("container_extension") val containerExtension: String?,
    @SerializedName("custom_sid") val customSid: String?,
    @SerializedName("direct_source") val directSource: String?
)

// Series
data class XtreamSeries(
    val num: Int?,
    val name: String?,
    @SerializedName("series_id") val seriesId: Int?,
    val cover: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("last_modified") val lastModified: String?,
    val rating: String?,
    @SerializedName("rating_5based") val rating5Based: Double?,
    @SerializedName("backdrop_path") val backdropPath: List<String>?,
    val youtube_trailer: String?,
    @SerializedName("episode_run_time") val episodeRunTime: String?,
    @SerializedName("category_id") val categoryId: String?
)

// Series info (episodes)
data class XtreamSeriesInfo(
    val seasons: List<XtreamSeason>?,
    val info: XtreamSeriesDetails?,
    val episodes: Map<String, List<XtreamEpisode>>?
)

data class XtreamSeason(
    @SerializedName("season_number") val seasonNumber: Int,
    val name: String?,
    val cover: String?,
    @SerializedName("episode_count") val episodeCount: Int?
)

data class XtreamSeriesDetails(
    val name: String?,
    val cover: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    @SerializedName("release_date") val releaseDate: String?,
    val rating: String?,
    @SerializedName("backdrop_path") val backdropPath: List<String>?,
    @SerializedName("youtube_trailer") val youtubeTrailer: String?
)

data class XtreamEpisode(
    val id: String?,
    @SerializedName("episode_num") val episodeNum: Int?,
    val title: String?,
    @SerializedName("container_extension") val containerExtension: String?,
    val info: XtreamEpisodeInfo?,
    @SerializedName("custom_sid") val customSid: String?,
    val added: String?,
    val season: Int?,
    @SerializedName("direct_source") val directSource: String?
)

data class XtreamEpisodeInfo(
    @SerializedName("movie_image") val movieImage: String?,
    val plot: String?,
    @SerializedName("release_date") val releaseDate: String?,
    val rating: Double?,
    val duration: String?
)
