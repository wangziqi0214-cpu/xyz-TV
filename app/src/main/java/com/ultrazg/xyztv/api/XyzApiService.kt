package com.ultrazg.xyztv.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface XyzApiService {

    @POST("discovery")
    suspend fun discovery(@Body body: RequestBody): Response<ResponseBody>

    @POST("search")
    suspend fun search(@Body body: RequestBody): Response<ResponseBody>

    @POST("podcast_detail")
    suspend fun podcastDetail(@Body body: RequestBody): Response<ResponseBody>

    @POST("episode_list")
    suspend fun episodeList(@Body body: RequestBody): Response<ResponseBody>

    @POST("episode_detail")
    suspend fun episodeDetail(@Body body: RequestBody): Response<ResponseBody>
}
