package com.example.alarmboss.util

import android.content.Context

/** Options for users who can't rely on hearing the alarm, or who share a room with someone asleep. */
class AccessibilitySettings(context: Context) {
    private val prefs = context.getSharedPreferences("alarm_boss_settings", Context.MODE_PRIVATE)

    /** No sound: wake by vibration (and flash) only. */
    var silentMode: Boolean
        get() = prefs.getBoolean(KEY_SILENT, false)
        set(value) { prefs.edit().putBoolean(KEY_SILENT, value).apply() }

    /** Blink the camera light while the alarm rings. */
    var flash: Boolean
        get() = prefs.getBoolean(KEY_FLASH, false)
        set(value) { prefs.edit().putBoolean(KEY_FLASH, value).apply() }

    private companion object {
        const val KEY_SILENT = "access_silent_mode"
        const val KEY_FLASH = "access_flash"
    }
}
