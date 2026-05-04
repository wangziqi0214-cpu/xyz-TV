package com.ultrazg.xyztv.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface OfficialUserApiService {

    @GET("v1/search/get-preset")
    suspend fun searchPreset(): Response<ResponseBody>

    @POST("v1/subscription/list")
    suspend fun subscriptionList(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/subscription/update")
    suspend fun subscriptionUpdate(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/favorite/list")
    suspend fun favoriteEpisodeList(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/favorite/update")
    suspend fun favoriteEpisodeUpdate(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/episode-played/list-history")
    suspend fun playedHistoryList(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/episode-played/create")
    suspend fun playedHistoryCreate(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/playback-progress/list")
    suspend fun playbackProgressList(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/playback-progress/update")
    suspend fun playbackProgressUpdate(@Body body: RequestBody): Response<ResponseBody>

    @GET("v1/profile/get")
    suspend fun profile(@Query("uid") uid: String? = null): Response<ResponseBody>

    @GET("v1/user-stats/get")
    suspend fun userStats(@Query("uid") uid: String): Response<ResponseBody>

    @GET("v1/unread-count/get")
    suspend fun unreadCount(): Response<ResponseBody>

    @POST("v1/podcaster/owned-podcasts")
    suspend fun ownedPodcasts(@Body body: RequestBody): Response<ResponseBody>

    @GET("v1/top-list/get")
    suspend fun topList(@Query("category") category: String): Response<ResponseBody>

    @POST("v1/related-podcast/list")
    suspend fun relatedPodcasts(@Body body: RequestBody): Response<ResponseBody>

    @GET("v1/podcast-bulletin/get-by-pid")
    suspend fun podcastBulletin(@Query("pid") pid: String): Response<ResponseBody>

    @GET("v1/podcast/get-info")
    suspend fun podcastInfo(@Query("pid") pid: String): Response<ResponseBody>

    @POST("v1/podcast-honor/list")
    suspend fun podcastHonorList(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/inbox/list")
    suspend fun inboxList(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/pilot-discovery/list")
    suspend fun pilotDiscoveryList(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/editor-pick/list-history")
    suspend fun editorPickListHistory(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/pick/list-recent")
    suspend fun pickListRecent(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/pick/list-history")
    suspend fun pickListHistory(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/subscription-star/list")
    suspend fun starSubscriptionList(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/subscription/list-non-starred")
    suspend fun nonStarredSubscriptionList(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/subscription-star/update")
    suspend fun starSubscriptionUpdate(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/user-relation/list-following")
    suspend fun followingList(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/user-relation/list-follower")
    suspend fun followerList(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/user-relation/update")
    suspend fun relationUpdate(@Body body: RequestBody): Response<ResponseBody>

    @GET("v1/user-preference/get")
    suspend fun userPreferenceGet(): Response<ResponseBody>

    @POST("v1/user-preference/update")
    suspend fun userPreferenceUpdate(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/comment/list-primary")
    suspend fun commentPrimary(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/episode/list-by-filter")
    suspend fun popularEpisodeList(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/comment/list-thread")
    suspend fun commentThread(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/like/update")
    suspend fun commentLikeUpdate(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/comment/collect/create")
    suspend fun commentCollectCreate(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/comment/collect/remove")
    suspend fun commentCollectRemove(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/comment/collect/list")
    suspend fun commentCollectList(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/comment/create")
    suspend fun commentCreate(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/comment/remove")
    suspend fun commentRemove(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/blocked-user/list")
    suspend fun blockedUserList(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/blocked-user/create")
    suspend fun blockedUserCreate(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/blocked-user/remove")
    suspend fun blockedUserRemove(@Body body: RequestBody): Response<ResponseBody>

    @GET("v1/mileage/get")
    suspend fun mileageGet(): Response<ResponseBody>

    @POST("v1/mileage/list")
    suspend fun mileageList(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/mileage/update")
    suspend fun mileageUpdate(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/clap/list")
    suspend fun clapList(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/clap/create")
    suspend fun clapCreate(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/sticker/list")
    suspend fun stickerList(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/sticker/get-board")
    suspend fun stickerBoard(@Body body: RequestBody): Response<ResponseBody>
}
