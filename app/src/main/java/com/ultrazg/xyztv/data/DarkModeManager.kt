package com.ultrazg.xyztv.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object DarkModeManager {
    private const val PREF_NAME = "xyz_tv_settings"
    private const val KEY_DARK_MODE = "dark_mode_enabled"

    private lateinit var prefs: SharedPreferences

    var isDarkMode by mutableStateOf(true)
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        isDarkMode = prefs.getBoolean(KEY_DARK_MODE, true)
    }

    fun setEnabled(enabled: Boolean) {
        isDarkMode = enabled
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        AppLogger.info("dark_mode", "setDarkMode enabled=$enabled")
    }
}
