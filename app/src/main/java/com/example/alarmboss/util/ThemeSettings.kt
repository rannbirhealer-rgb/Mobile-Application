package com.example.alarmboss.util

import android.content.Context

/** Which colour scheme the app uses: follow the phone, or always light, or always dark. */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    /** Whether this mode means the dark scheme, given whether the phone is currently in dark mode. */
    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemIsDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        /** The stored mode, or [SYSTEM] if nothing valid is stored. */
        fun parse(stored: String?): ThemeMode = values().firstOrNull { it.name == stored } ?: SYSTEM
    }
}

class ThemeSettings(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var mode: ThemeMode
        get() = ThemeMode.parse(prefs.getString(KEY_MODE, null))
        set(value) { prefs.edit().putString(KEY_MODE, value.name).apply() }

    companion object {
        const val PREFS_NAME = "alarm_boss_settings"
        const val KEY_MODE = "theme_mode"
    }
}
