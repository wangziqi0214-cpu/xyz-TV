package com.ultrazg.xyztv.api

import com.ultrazg.xyztv.data.AppLogger
import com.ultrazg.xyztv.data.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val AUTH_BASE_URL = "https://podcaster-api.xiaoyuzhoufm.com/"
    private const val APP_BASE_URL = "https://api.xiaoyuzhoufm.com/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE
    }

    private val auditInterceptor = Interceptor { chain ->
        val request = chain.request()
        val startedAt = System.currentTimeMillis()
        AppLogger.network(
            "request ${request.method} ${request.url.encodedPath} " +
                "body=${requestBodyPreview(request.body)}"
        )
        try {
            val response = chain.proceed(request)
            val elapsed = System.currentTimeMillis() - startedAt
            val errorSnippet = if (!response.isSuccessful) {
                response.peekBody(400).string().replace('\n', ' ').take(240)
            } else {
                ""
            }
            AppLogger.network(
                "response ${response.code} ${request.method} ${request.url.encodedPath} ${elapsed}ms" +
                    if (errorSnippet.isNotBlank()) " body=$errorSnippet" else ""
            )
            response
        } catch (e: IOException) {
            val elapsed = System.currentTimeMillis() - startedAt
            AppLogger.error(
                "network",
                "failure ${request.method} ${request.url.encodedPath} ${elapsed}ms",
                e
            )
            throw e
        }
    }

    private val contentHeaderInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
        TokenManager.accessToken?.takeIf { it.isNotBlank() }?.let { token ->
            request.header("x-jike-access-token", token)
        }
        TokenManager.refreshToken?.takeIf { it.isNotBlank() }?.let { token ->
            request.header("x-jike-refresh-token", token)
        }
        val cookies = listOfNotNull(
            TokenManager.accessToken?.takeIf { it.isNotBlank() }?.let { "x-jike-access-token=$it" },
            TokenManager.refreshToken?.takeIf { it.isNotBlank() }?.let { "x-jike-refresh-token=$it" }
        )
        if (cookies.isNotEmpty()) {
            request.header("Cookie", cookies.joinToString("; "))
        }
        request.header("User-Agent", "Xiaoyuzhou/2.57.1 (build:1576; iOS 17.4.1)")
        request.header("Market", "AppStore")
        request.header("App-BuildNo", "1576")
        request.header("OS", "ios")
        request.header("Manufacturer", "Apple")
        request.header("BundleID", "app.podcast.cosmos")
        request.header("Model", "iPhone14,2")
        request.header("App-Version", "2.57.1")
        request.header("OS-Version", "17.4.1")
        request.header("Timezone", "Asia/Shanghai")
        request.header("WifiConnected", "true")
        request.header("app-permissions", "4")
        request.header("Accept-Language", "zh-Hant-HK;q=1.0, zh-Hans-CN;q=0.9")
        request.header("x-custom-xiaoyuzhou-app-dev", "")
        request.header("x-jike-device-id", TokenManager.deviceId)
        request.header("x-jike-device-properties", TokenManager.deviceProperties)
        request.header("Accept", "*/*")
        request.header("Content-Type", "application/json")
        chain.proceed(request.build())
    }

    private val authHeaderInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("accept", "application/json, text/plain, */*")
            .header("accept-encoding", "gzip, deflate, br, zstd")
            .header("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
            .header("content-type", "application/json;charset=UTF-8")
            .header("origin", "https://podcaster.xiaoyuzhoufm.com")
            .header("referer", "https://podcaster.xiaoyuzhoufm.com/")
            .header("x-app-build-time", "2026-04-28 15:29:48 +0800")
            .header("x-jike-allow-app-token-in-cookie", "true")
            .header(
                "user-agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36"
            )
        TokenManager.accessToken?.takeIf { it.isNotBlank() }?.let { token ->
            request.header("x-jike-access-token", token)
        }
        TokenManager.refreshToken?.takeIf { it.isNotBlank() }?.let { token ->
            request.header("x-jike-refresh-token", token)
        }
        val cookies = listOfNotNull(
            TokenManager.accessToken?.takeIf { it.isNotBlank() }?.let { "x-jike-access-token=$it" },
            TokenManager.refreshToken?.takeIf { it.isNotBlank() }?.let { "x-jike-refresh-token=$it" }
        )
        if (cookies.isNotEmpty()) {
            request.header("cookie", cookies.joinToString("; "))
        }
        chain.proceed(request.build())
    }

    private val contentClient = OkHttpClient.Builder()
        .addInterceptor(auditInterceptor)
        .addInterceptor(contentHeaderInterceptor)
        .addInterceptor(logging)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    private val authClient = OkHttpClient.Builder()
        .addInterceptor(auditInterceptor)
        .addInterceptor(authHeaderInterceptor)
        .addInterceptor(logging)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    val instance: XyzApiService by lazy {
        Retrofit.Builder()
            .baseUrl(TokenManager.baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(contentClient)
            .build()
            .create(XyzApiService::class.java)
    }

    val authApi: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(AUTH_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(authClient)
            .build()
            .create(AuthApiService::class.java)
    }

    val officialContentApi: OfficialContentApiService by lazy {
        Retrofit.Builder()
            .baseUrl(APP_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(contentClient)
            .build()
            .create(OfficialContentApiService::class.java)
    }

    val officialUserApi: OfficialUserApiService by lazy {
        Retrofit.Builder()
            .baseUrl(APP_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(contentClient)
            .build()
            .create(OfficialUserApiService::class.java)
    }

    private fun requestBodyPreview(body: RequestBody?): String {
        if (body == null) return "<empty>"
        return runCatching {
            val buffer = okio.Buffer()
            body.writeTo(buffer)
            redact(buffer.readUtf8())
        }.getOrElse { "<unavailable:${it.javaClass.simpleName}>" }
    }

    private fun redact(text: String): String {
        return text
            .replace(Regex("\"mobilePhoneNumber\"\\s*:\\s*\"[^\"]+\""), "\"mobilePhoneNumber\":\"***\"")
            .replace(Regex("\"verifyCode\"\\s*:\\s*\"[^\"]+\""), "\"verifyCode\":\"***\"")
            .replace(Regex("\"accessToken\"\\s*:\\s*\"[^\"]+\""), "\"accessToken\":\"***\"")
            .replace(Regex("\"refreshToken\"\\s*:\\s*\"[^\"]+\""), "\"refreshToken\":\"***\"")
            .take(400)
    }
}
