package com.ultrazg.xyztv.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.ultrazg.xyztv.api.RetrofitClient
import com.ultrazg.xyztv.data.AppLogger
import com.ultrazg.xyztv.data.TokenManager
import com.ultrazg.xyztv.data.model.LoginRequest
import com.ultrazg.xyztv.data.model.SendCodeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.ResponseBody
import retrofit2.Response
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class LoginViewModel : ViewModel() {
    private val requestTimeoutMs = 20_000L
    private val areaCodeCandidates = listOf("+86", "86")
    var status by mutableStateOf("Idle")
        private set

    var phone by mutableStateOf("")
        private set

    var code by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var isCodeSent by mutableStateOf(false)
        private set

    var isLoggedIn by mutableStateOf(TokenManager.isLoggedIn())
        private set

    fun onPhoneChange(value: String) {
        phone = value.filter { it.isDigit() }.take(11)
        error = null
        status = "Phone edited: ${phone.length}/11"
        AppLogger.action("phone_input", "length=${phone.length}")
    }

    fun onCodeChange(value: String) {
        code = value.filter { it.isDigit() }.take(6)
        error = null
        status = "Code edited: ${code.length}"
        AppLogger.action("code_input", "length=${code.length}")
    }

    fun sendCode() {
        if (isLoading) {
            AppLogger.warn("login", "sendCode ignored because request is already running")
            return
        }
        status = "Send code tapped"
        AppLogger.action("send_code_tapped", "phone_length=${phone.length}")
        if (phone.length != 11) {
            error = "Please enter an 11-digit phone number"
            status = "Validation failed: phone length is ${phone.length}"
            AppLogger.warn("login", status)
            return
        }

        viewModelScope.launch {
            isLoading = true
            error = null
            status = "Calling SMS API..."
            AppLogger.info("login", "Calling sendCode API")
            try {
                val (res, raw, areaCode) = withContext(Dispatchers.IO) {
                    withTimeout(requestTimeoutMs) {
                        attemptSendCode()
                    }
                }
                if (res.isSuccessful) {
                    isCodeSent = true
                    status = "SMS API success ($areaCode)"
                    AppLogger.info("login", "sendCode success areaCode=$areaCode")
                } else {
                    val body = raw.ifBlank { res.errorBody()?.string().orEmpty() }.take(300)
                    error = "Send code failed ${res.code()}: $body"
                    status = "SMS API failed: ${res.code()} ($areaCode)"
                    AppLogger.warn("login", "sendCode failed areaCode=$areaCode ${res.code()} body=${body.take(200)}")
                }
            } catch (e: TimeoutCancellationException) {
                error = "Network timeout while sending SMS code"
                status = "SMS API timeout"
                AppLogger.error("login", "sendCode timeout", e)
            } catch (e: Exception) {
                error = "Network error: ${e.message}"
                status = "SMS API exception: ${e.javaClass.simpleName}"
                AppLogger.error("login", "sendCode exception", e)
            }
            isLoading = false
        }
    }

    fun login(onSuccess: () -> Unit) {
        if (isLoading) {
            AppLogger.warn("login", "login ignored because request is already running")
            return
        }
        status = "Login tapped"
        AppLogger.action("login_tapped", "phone_length=${phone.length}, code_length=${code.length}")
        if (phone.length != 11) {
            error = "Please enter an 11-digit phone number"
            status = "Validation failed: phone length is ${phone.length}"
            AppLogger.warn("login", status)
            return
        }
        if (code.length !in 4..6) {
            error = "Please enter a 4-6 digit code"
            status = "Validation failed: code length is ${code.length}"
            AppLogger.warn("login", status)
            return
        }

        viewModelScope.launch {
            isLoading = true
            error = null
            status = "Calling login API..."
            AppLogger.info("login", "Calling login API")
            try {
                val (res, raw, areaCode) = withContext(Dispatchers.IO) {
                    withTimeout(requestTimeoutMs) {
                        attemptLogin()
                    }
                }
                val tokens = extractAuthTokens(
                    rawBody = raw,
                    headers = res.headers().toMultimap()
                )
                val accessToken = tokens?.first.orEmpty()
                val refreshToken = tokens?.second.orEmpty()

                if (res.isSuccessful && accessToken.isNotBlank()) {
                    TokenManager.accessToken = accessToken
                    TokenManager.refreshToken = refreshToken.ifBlank { null }
                    isLoggedIn = true
                    status = "Login success ($areaCode)"
                    AppLogger.info("login", "Login success areaCode=$areaCode tokenPrefix=${accessToken.take(12)}")
                    onSuccess()
                } else {
                    val body = raw.ifBlank { res.errorBody()?.string().orEmpty() }.take(300)
                    error = "Login failed ${res.code()}: $body"
                    status = "Login failed: ${res.code()} ($areaCode)"
                    if (res.isSuccessful) {
                        AppLogger.warn(
                            "login",
                            "login response had no auth token headerNames=${res.headers().names()} setCookieCount=${res.headers().values("Set-Cookie").size} bodyPrefix=${body.take(120)}"
                        )
                    }
                    AppLogger.warn("login", "login failed areaCode=$areaCode ${res.code()} body=${body.take(200)}")
                }
            } catch (e: TimeoutCancellationException) {
                error = "Network timeout while logging in"
                status = "Login API timeout"
                AppLogger.error("login", "login timeout", e)
            } catch (e: Exception) {
                error = "Network error: ${e.message}"
                status = "Login exception: ${e.javaClass.simpleName}"
                AppLogger.error("login", "login exception", e)
            }
            isLoading = false
        }
    }

    fun logout() {
        AppLogger.action("logout", "viewmodel logout")
        TokenManager.clear()
        isLoggedIn = false
        phone = ""
        code = ""
        isCodeSent = false
    }

    private suspend fun attemptSendCode(): Triple<Response<ResponseBody>, String, String> {
        var last: Triple<Response<ResponseBody>, String, String>? = null
        for (areaCode in areaCodeCandidates) {
            AppLogger.info("login", "sendCode attempt areaCode=$areaCode")
            val res = RetrofitClient.authApi.sendCode(SendCodeRequest(phone, areaCode))
            val raw = res.body()?.string().orEmpty()
            last = Triple(res, raw, areaCode)
            if (res.isSuccessful) return last
            val body = raw.ifBlank { res.errorBody()?.string().orEmpty() }
            val retryableInvalidParams = res.code() == 400 && body.contains("无效参数")
            if (!retryableInvalidParams) return Triple(res, body, areaCode)
        }
        return last ?: throw IllegalStateException("No sendCode attempt executed")
    }

    private suspend fun attemptLogin(): Triple<Response<ResponseBody>, String, String> {
        var last: Triple<Response<ResponseBody>, String, String>? = null
        for (areaCode in areaCodeCandidates) {
            AppLogger.info("login", "login attempt areaCode=$areaCode")
            val res = RetrofitClient.authApi.login(LoginRequest(phone, code, areaCode))
            val raw = res.body()?.string().orEmpty()
            last = Triple(res, raw, areaCode)
            if (res.isSuccessful) return last
            val body = raw.ifBlank { res.errorBody()?.string().orEmpty() }
            val retryableInvalidParams = res.code() == 400 && body.contains("无效参数")
            if (!retryableInvalidParams) return Triple(res, body, areaCode)
        }
        return last ?: throw IllegalStateException("No login attempt executed")
    }

}

internal fun extractAuthTokens(
    rawBody: String,
    headers: Map<String, List<String>>
): Pair<String, String?>? {
    val accessToken = findHeaderValue(headers, "x-jike-access-token")
        ?: findCookieValue(headers, "x-jike-access-token")
        ?: findJsonString(rawBody, "x-jike-access-token", "accessToken", "access_token", "token")
    val refreshToken = findHeaderValue(headers, "x-jike-refresh-token")
        ?: findCookieValue(headers, "x-jike-refresh-token")
        ?: findJsonString(rawBody, "x-jike-refresh-token", "refreshToken", "refresh_token")
    return accessToken?.takeIf { it.isNotBlank() }?.let { it to refreshToken }
}

private fun findHeaderValue(headers: Map<String, List<String>>, name: String): String? {
    return headers.entries
        .firstOrNull { it.key.equals(name, ignoreCase = true) }
        ?.value
        ?.firstOrNull { it.isNotBlank() }
}

private fun findCookieValue(headers: Map<String, List<String>>, name: String): String? {
    return headers.entries
        .filter { it.key.equals("Set-Cookie", ignoreCase = true) }
        .flatMap { it.value }
        .asSequence()
        .mapNotNull { cookie ->
            cookie.substringBefore(";")
                .takeIf { it.startsWith("$name=") }
                ?.substringAfter("=")
        }
        .firstOrNull()
        ?.takeIf { it.isNotBlank() }
        ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
}

private fun findJsonString(rawBody: String, vararg names: String): String? {
    val root = runCatching { JsonParser().parse(rawBody) }.getOrNull() ?: return null
    return findJsonString(root, names.toSet())
}

private fun findJsonString(value: Any?, names: Set<String>): String? {
    return when (value) {
        is JsonObject -> {
            for ((key, element) in value.entrySet()) {
                if (names.any { it.equals(key, ignoreCase = true) }) {
                    val direct = element.asStringOrNull()?.takeIf { it.isNotBlank() }
                    if (direct != null) return direct
                }
                val nested = findJsonString(element, names)
                if (nested != null) return nested
            }
            null
        }
        is JsonArray -> {
            for (element in value) {
                val nested = findJsonString(element, names)
                if (nested != null) return nested
            }
            null
        }
        else -> null
    }
}

private fun JsonElement.asStringOrNull(): String? {
    return takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
}
