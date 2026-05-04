package com.ultrazg.xyztv.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface OfficialContentApiService {

    @POST("v1/category/list-all")
    suspend fun categoryList(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/category/podcast/list-tabs")
    suspend fun categoryTabs(@Body body: RequestBody): Response<ResponseBody>

    @POST("v1/category/podcast/list-by-tab")
    suspend fun categoryPodcastList(@Body body: RequestBody): Response<ResponseBody>

    @GET("v1/search/get-preset")
    suspend fun searchPreset(): Response<ResponseBody>
}
