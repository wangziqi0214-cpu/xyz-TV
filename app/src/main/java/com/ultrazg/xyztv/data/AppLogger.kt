package com.ultrazg.xyztv.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

object AppLogger {
    private const val TAG = "XyzTv"
    private const val MAX_LOG_SIZE_BYTES = 1_000_000L
    private const val ACTION_DEDUPE_WINDOW_MS = 750L

    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val logExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "xyz-tv-logger").apply { isDaemon = true }
    }
    private val timestampFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    }
    private var logFile: File? = null
    private var lastActionLine = ""
    private var lastActionAtMs = 0L

    var lastLine by mutableStateOf("")
        private set

    var recentLines by mutableStateOf<List<String>>(emptyList())
        private set

    var diagnosticLines by mutableStateOf<List<String>>(emptyList())
        private set

    fun init(context: Context) {
        synchronized(lock) {
            val logDir = File(context.filesDir, "logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            logFile = File(logDir, "app.log")
            rotateIfNeeded()
            info("logger", "Logger initialized at ${logFile?.absolutePath}")
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                error("crash", "Uncaught exception on ${thread.name}", throwable)
            }
        }
    }

    fun filePath(): String = synchronized(lock) { logFile?.absolutePath ?: "uninitialized" }

    fun debug(scope: String, message: String) = write("DEBUG", scope, message)

    fun info(scope: String, message: String) = write("INFO", scope, message)

    fun warn(scope: String, message: String) = write("WARN", scope, message)

    fun error(scope: String, message: String, throwable: Throwable? = null) {
        val suffix = throwable?.let { " | ${it.javaClass.simpleName}: ${it.message}" }.orEmpty()
        write("ERROR", scope, message + suffix)
    }

    fun action(name: String, details: String) {
        val message = "$name | $details"
        val now = System.currentTimeMillis()
        synchronized(lock) {
            if (message == lastActionLine && now - lastActionAtMs < ACTION_DEDUPE_WINDOW_MS) {
                return
            }
            lastActionLine = message
            lastActionAtMs = now
        }
        info("action", message)
    }

    fun network(message: String) = info("network", message)

    private fun write(level: String, scope: String, message: String) {
        val line = "${timestampFormat.get()!!.format(Date())} [$level] [$scope] $message"
        Log.d(TAG, line)
        mainHandler.post {
            lastLine = line
            recentLines = (recentLines + line).takeLast(6)
            if (scope in setOf("network", "login", "embedded", "repo", "pairing", "crash", "web-login") || level != "INFO") {
                diagnosticLines = (diagnosticLines + line).takeLast(12)
            }
        }
        logExecutor.execute {
            synchronized(lock) {
                runCatching {
                    rotateIfNeeded()
                    val file = logFile ?: return@execute
                    file.appendText(line + "\n")
                }.onFailure {
                    Log.e(TAG, "Failed to write log", it)
                }
            }
        }
    }

    private fun rotateIfNeeded() {
        val current = logFile ?: return
        if (current.exists() && current.length() >= MAX_LOG_SIZE_BYTES) {
            val backup = File(current.parentFile, "app-prev.log")
            if (backup.exists()) {
                backup.delete()
            }
            current.copyTo(backup, overwrite = true)
            current.writeText("")
        }
    }
}
