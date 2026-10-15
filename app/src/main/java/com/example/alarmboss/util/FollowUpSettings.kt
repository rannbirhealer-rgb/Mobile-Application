package com.example.alarmboss.util

import android.content.Context

/** Settings for the extra rings that follow a main alarm. */
class FollowUpSettings(context: Context) {
    private val prefs = context.getSharedPreferences("alarm_boss_settings", Context.MODE_PRIVATE)

    /**
     * After a Medium or Strict alarm is completed, ring again with a quick challenge. The commonest way
     * a strict alarm fails is that you pass the task and go straight back to sleep.
     */
    var wakeCheckEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_CHECK, false)
        set(value) { prefs.edit().putBoolean(KEY_WAKE_CHECK, value).apply() }

    var wakeCheckMinutes: Int
        get() = sanitizeWakeCheckMinutes(prefs.getInt(KEY_WAKE_CHECK_MINUTES, DEFAULT_WAKE_CHECK_MINUTES))
        set(value) { prefs.edit().putInt(KEY_WAKE_CHECK_MINUTES, sanitizeWakeCheckMinutes(value)).apply() }

    companion object {
        private const val KEY_WAKE_CHECK = "follow_up_wake_check"
        private const val KEY_WAKE_CHECK_MINUTES = "follow_up_wake_check_minutes"
        const val DEFAULT_WAKE_CHECK_MINUTES = 10
        val WAKE_CHECK_OPTIONS = listOf(5, 10, 15)

        fun sanitizeWakeCheckMinutes(value: Int): Int =
            if (value in WAKE_CHECK_OPTIONS) value else DEFAULT_WAKE_CHECK_MINUTES
    }
}
