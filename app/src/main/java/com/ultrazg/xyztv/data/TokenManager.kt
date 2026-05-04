package com.ultrazg.xyztv.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import org.json.JSONObject
import java.util.UUID

object TokenManager {
    private const val PREF_NAME = "xyz_tv_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_DEVICE_PROPERTIES = "device_properties"
    private const val KEY_BASE_URL = "base_url"
    private const val DEFAULT_BASE_URL = "http://127.0.0.1:23020/"

    private lateinit var prefs: SharedPreferences
    private val _loginState = mutableStateOf(false)
    val loginState: State<Boolean> = _loginState

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        _loginState.value = isLoggedIn()
    }

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()
            _loginState.value = isLoggedIn()
        }

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()
            _loginState.value = isLoggedIn()
        }

    var baseUrl: String
        get() = normalizeBaseUrl(prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL)
        set(value) = prefs.edit().putString(KEY_BASE_URL, normalizeBaseUrl(value)).apply()

    val deviceId: String
        get() {
            var id = prefs.getString(KEY_DEVICE_ID, null)
            if (id.isNullOrBlank()) {
                id = UUID.randomUUID().toString().lowercase().replace("-", "")
                prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            }
            return id
        }

    val deviceProperties: String
        get() {
            var properties = prefs.getString(KEY_DEVICE_PROPERTIES, null)
            if (properties.isNullOrBlank()) {
                val androidId = UUID.randomUUID().toString().lowercase().replace("-", "").take(16)
                properties = JSONObject()
                    .put("uuid", deviceId)
                    .put("android_id", androidId)
                    .put("oaid", "")
                    .put("vaid", "")
                    .put("aaid", "")
                    .toString()
                prefs.edit().putString(KEY_DEVICE_PROPERTIES, properties).apply()
            }
            return properties
        }

    fun isLoggedIn(): Boolean = !accessToken.isNullOrBlank()

    fun clear() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
        _loginState.value = false
    }

    private fun normalizeBaseUrl(value: String): String {
        var normalized = value.trim()
        if (normalized.isBlank()) {
            normalized = DEFAULT_BASE_URL
        }
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://$normalized"
        }
        if (!normalized.endsWith("/")) {
            normalized += "/"
        }
        return normalized
    }
}
