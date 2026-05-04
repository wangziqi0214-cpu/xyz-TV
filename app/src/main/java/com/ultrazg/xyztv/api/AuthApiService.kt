package com.ultrazg.xyztv.api

import com.ultrazg.xyztv.data.model.LoginRequest
import com.ultrazg.xyztv.data.model.SendCodeRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("v1/auth/send-code")
    suspend fun sendCode(@Body body: SendCodeRequest): Response<ResponseBody>

    @POST("v1/auth/get-user-with-sms")
    suspend fun login(@Body body: LoginRequest): Response<ResponseBody>

    @POST("management/episode-transcript/get")
    suspend fun episodeTranscriptGet(@Body body: okhttp3.RequestBody): Response<ResponseBody>
}
