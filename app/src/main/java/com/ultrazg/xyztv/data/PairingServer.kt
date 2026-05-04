package com.ultrazg.xyztv.data

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

object PairingServer {
    private const val PORT = 8888
    private const val CLIENT_TIMEOUT_MS = 10_000

    var serverUrl by mutableStateOf<String?>(null)
        private set

    var allIps by mutableStateOf<List<String>>(emptyList())
        private set

    var statusMessage by mutableStateOf("Legacy pairing server is not running")
        private set

    var isRunning by mutableStateOf(false)
        private set

    var lastReceivedTokenPrefix by mutableStateOf<String?>(null)
        private set

    private var serverSocket: ServerSocket? = null
    private var serverThread: Thread? = null

    fun start(onPaired: () -> Unit) {
        if (isRunning) return
        AppLogger.info("pairing", "Legacy pairing start requested")

        val ips = findAllLocalIps()
        allIps = ips
        if (ips.isEmpty()) {
            statusMessage = "No LAN IPv4 address found. Make sure the TV is on the same network as the phone."
            AppLogger.warn("pairing", statusMessage)
            return
        }

        val socket = runCatching { ServerSocket(PORT) }.getOrNull()
        if (socket == null) {
            statusMessage = "Port $PORT is busy. Legacy pairing server failed to start."
            AppLogger.warn("pairing", statusMessage)
            return
        }

        serverSocket = socket
        serverUrl = "http://${ips.first()}:$PORT"
        statusMessage = "Open this URL from your phone browser on the same LAN."
        AppLogger.info("pairing", "Legacy pairing listening at $serverUrl")
        isRunning = true

        serverThread = thread(start = true, isDaemon = true, name = "pairing-server") {
            while (isRunning) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                runCatching { handleClient(client, onPaired) }
                    .onFailure { AppLogger.warn("pairing", "Client handling failed: ${it.message}") }
            }
            stop()
        }
    }

    fun stop() {
        AppLogger.info("pairing", "Legacy pairing stop requested")
        isRunning = false
        runCatching { serverSocket?.close() }
        serverSocket = null
        serverThread = null
    }

    private fun handleClient(client: Socket, onPaired: () -> Unit) {
        client.use { socket ->
            socket.soTimeout = CLIENT_TIMEOUT_MS
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
            val writer = OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0]
            val path = parts[1]

            val headers = linkedMapOf<String, String>()
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isBlank()) break
                val index = line.indexOf(':')
                if (index > 0) {
                    headers[line.substring(0, index).trim().lowercase()] = line.substring(index + 1).trim()
                }
            }

            if (method == "GET") {
                respondHtml(writer, pairingPage())
                return
            }

            if (method == "POST" && path.startsWith("/submit")) {
                val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                val bodyChars = CharArray(contentLength)
                var totalRead = 0
                while (totalRead < contentLength) {
                    val read = reader.read(bodyChars, totalRead, contentLength - totalRead)
                    if (read <= 0) break
                    totalRead += read
                }

                val form = parseForm(String(bodyChars, 0, totalRead))
                val accessToken = form["accessToken"]?.trim().orEmpty()
                val refreshToken = form["refreshToken"]?.trim().orEmpty()

                if (accessToken.isBlank()) {
                    respondHtml(writer, pairingPage("Access token is required."))
                    return
                }

                TokenManager.accessToken = accessToken
                TokenManager.refreshToken = refreshToken.ifBlank { null }
                lastReceivedTokenPrefix = accessToken.take(12)
                statusMessage = "Legacy pairing succeeded"
                AppLogger.info("pairing", "Token received via legacy pairing prefix=${lastReceivedTokenPrefix ?: "null"}")
                respondHtml(writer, successPage())
                Handler(Looper.getMainLooper()).post { onPaired() }
                stop()
                return
            }

            respondNotFound(writer)
        }
    }

    private fun parseForm(body: String): Map<String, String> {
        if (body.isBlank()) return emptyMap()
        return body.split("&")
            .mapNotNull {
                val index = it.indexOf('=')
                if (index < 0) return@mapNotNull null
                val key = URLDecoder.decode(it.substring(0, index), StandardCharsets.UTF_8.name())
                val value = URLDecoder.decode(it.substring(index + 1), StandardCharsets.UTF_8.name())
                key to value
            }
            .toMap()
    }

    private fun respondHtml(writer: OutputStreamWriter, html: String) {
        val bodyBytes = html.toByteArray(StandardCharsets.UTF_8)
        writer.write("HTTP/1.1 200 OK\r\n")
        writer.write("Content-Type: text/html; charset=utf-8\r\n")
        writer.write("Content-Length: ${bodyBytes.size}\r\n")
        writer.write("Connection: close\r\n")
        writer.write("\r\n")
        writer.write(html)
        writer.flush()
    }

    private fun respondNotFound(writer: OutputStreamWriter) {
        val html = "<html><body><h1>404</h1></body></html>"
        val bodyBytes = html.toByteArray(StandardCharsets.UTF_8)
        writer.write("HTTP/1.1 404 Not Found\r\n")
        writer.write("Content-Type: text/html; charset=utf-8\r\n")
        writer.write("Content-Length: ${bodyBytes.size}\r\n")
        writer.write("Connection: close\r\n")
        writer.write("\r\n")
        writer.write(html)
        writer.flush()
    }

    private fun pairingPage(error: String? = null): String {
        val errorHtml = if (error.isNullOrBlank()) {
            ""
        } else {
            "<p style='color:#ff6b6b;'>${escapeHtml(error)}</p>"
        }
        return """
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1" />
              <title>Legacy TV Pairing</title>
              <style>
                body { font-family: sans-serif; background:#111; color:#fff; padding:24px; }
                input, textarea { width:100%; padding:12px; margin:8px 0 16px; border-radius:8px; border:none; box-sizing:border-box; }
                button { width:100%; padding:14px; border:none; border-radius:8px; background:#ffd54f; color:#000; font-size:16px; }
                .card { max-width:720px; margin:0 auto; background:#1f1f1f; padding:24px; border-radius:16px; }
              </style>
            </head>
            <body>
              <div class="card">
                <h2>Legacy TV Pairing</h2>
                <p>This page is separate from the embedded xyz backend. Paste the access token below and submit it to the TV.</p>
                $errorHtml
                <form method="POST" action="/submit">
                  <label>Access Token</label>
                  <textarea name="accessToken" rows="6"></textarea>
                  <label>Refresh Token (optional)</label>
                  <textarea name="refreshToken" rows="4"></textarea>
                  <button type="submit">Submit to TV</button>
                </form>
              </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun escapeHtml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun successPage(): String {
        return """
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1" />
              <title>Pairing Complete</title>
              <style>
                body { font-family: sans-serif; background:#111; color:#fff; display:flex; align-items:center; justify-content:center; min-height:100vh; margin:0; }
                .card { background:#1f1f1f; padding:24px; border-radius:16px; max-width:560px; }
              </style>
            </head>
            <body>
              <div class="card">
                <h2>Pairing Complete</h2>
                <p>The TV has received the token. You can go back to the TV now.</p>
              </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun findAllLocalIps(): List<String> {
        val result = mutableListOf<Pair<Int, String>>()
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (!networkInterface.isUp || networkInterface.isLoopback) continue
            val name = networkInterface.name.lowercase()
            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (address is Inet4Address && !address.isLoopbackAddress && address.isSiteLocalAddress) {
                    val priority = when {
                        name.contains("wlan") || name.contains("wifi") -> 0
                        name.contains("eth") -> 1
                        name.contains("rndis") || name.contains("docker") || name.contains("tun") || name.contains("ppp") -> 10
                        else -> 5
                    }
                    result.add(priority to address.hostAddress!!)
                }
            }
        }
        return result.sortedBy { it.first }.map { it.second }
    }
}
