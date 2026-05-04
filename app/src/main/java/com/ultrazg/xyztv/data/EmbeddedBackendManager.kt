package com.ultrazg.xyztv.data

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import embeddedxyz.Embeddedxyz
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

object EmbeddedBackendManager {
    private const val TAG = "EmbeddedBackend"
    private const val FALLBACK_BASE_URL = "http://127.0.0.1:23020/"
    private const val MAX_PING_ATTEMPTS = 12
    private const val PING_INTERVAL_MS = 500L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .writeTimeout(2, TimeUnit.SECONDS)
        .callTimeout(3, TimeUnit.SECONDS)
        .build()

    var baseUrl by mutableStateOf(FALLBACK_BASE_URL)
        private set

    var statusMessage by mutableStateOf("Embedded backend not started")
        private set

    var isHealthy by mutableStateOf(false)
        private set

    var lastError by mutableStateOf<String?>(null)
        private set

    fun start(): String {
        AppLogger.info("embedded", "Start requested")
        val startResult = runCatching { Embeddedxyz.start() }
        val resolvedBaseUrl = runCatching { Embeddedxyz.baseURL() }.getOrDefault(FALLBACK_BASE_URL)
        TokenManager.baseUrl = resolvedBaseUrl
        baseUrl = resolvedBaseUrl

        val error = startResult.exceptionOrNull()?.message.orEmpty()
            .ifBlank { startResult.getOrDefault("") }

        if (error.isBlank()) {
            Log.i(TAG, "Embedded xyz backend starting at $resolvedBaseUrl")
            AppLogger.info("embedded", "Start accepted, baseUrl=$resolvedBaseUrl")
            updateStatus(
                healthy = false,
                message = "Embedded backend is starting...",
                error = null
            )
            refreshHealthAsync()
        } else {
            Log.e(TAG, "Failed to start embedded xyz backend: $error")
            AppLogger.error("embedded", "Failed to start embedded backend: $resolvedBaseUrl | $error")
            updateStatus(
                healthy = false,
                message = "Embedded backend failed to start",
                error = error
            )
        }

        return error
    }

    fun ensureHealthy(): String? {
        AppLogger.debug("embedded", "ensureHealthy invoked")
        val error = waitForHealthy()
        return if (error == null) {
            updateStatus(
                healthy = true,
                message = "Embedded backend is healthy",
                error = null
            )
            null
        } else {
            updateStatus(
                healthy = false,
                message = "Embedded backend is unavailable",
                error = error
            )
            error
        }
    }

    fun refreshHealthAsync() {
        thread(start = true, isDaemon = true, name = "embedded-backend-health") {
            AppLogger.debug("embedded", "refreshHealthAsync worker started")
            val error = waitForHealthy()
            if (error == null) {
                updateStatus(
                    healthy = true,
                    message = "Embedded backend is healthy",
                    error = null
                )
            } else {
                updateStatus(
                    healthy = false,
                    message = "Embedded backend is unavailable",
                    error = error
                )
            }
        }
    }

    fun stop(): String {
        AppLogger.info("embedded", "Stop requested")
        return runCatching { Embeddedxyz.stop() }
            .onFailure { Log.e(TAG, "Failed to stop embedded xyz backend", it) }
            .getOrDefault("")
    }

    private fun waitForHealthy(): String? {
        var lastFailure = "Unknown error"
        repeat(MAX_PING_ATTEMPTS) {
            val failure = pingOnce()
            if (failure == null) {
                return null
            }
            lastFailure = failure
            Thread.sleep(PING_INTERVAL_MS)
        }
        return lastFailure
    }

    private fun pingOnce(): String? {
        val url = "${baseUrl}ping"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful && body.contains("pong")) {
                    AppLogger.debug("embedded", "Ping OK: $url")
                    null
                } else {
                    AppLogger.warn("embedded", "Ping bad response ${response.code}: ${body.take(200)}")
                    "GET $url returned ${response.code}: ${body.take(200)}"
                }
            }
        } catch (e: Exception) {
            AppLogger.error("embedded", "Ping exception for $url", e)
            "${e.javaClass.simpleName}: ${e.message}"
        }
    }

    private fun updateStatus(healthy: Boolean, message: String, error: String?) {
        AppLogger.info("embedded", "Status update healthy=$healthy message=$message error=${error ?: "<none>"}")
        mainHandler.post {
            isHealthy = healthy
            statusMessage = message
            lastError = error
        }
    }
}
