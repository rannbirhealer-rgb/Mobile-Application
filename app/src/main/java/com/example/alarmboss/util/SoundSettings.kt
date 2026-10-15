package com.example.alarmboss.util

import android.content.Context

/** How an alarm sounds when it rings. Kept in the same prefs file as the other behavior settings. */
class SoundSettings(context: Context) {
    private val prefs = context.getSharedPreferences("alarm_boss_settings", Context.MODE_PRIVATE)

    /** Player volume as a percentage of the system alarm volume. */
    var loudnessPercent: Int
        get() = sanitizeLoudness(prefs.getInt(KEY_LOUDNESS, DEFAULT_LOUDNESS))
        set(value) { prefs.edit().putInt(KEY_LOUDNESS, sanitizeLoudness(value)).apply() }

    /** Start quiet and fade up to the chosen loudness instead of blasting at once. */
    var gradual: Boolean
        get() = prefs.getBoolean(KEY_GRADUAL, false)
        set(value) { prefs.edit().putBoolean(KEY_GRADUAL, value).apply() }

    /** Lift the system alarm volume to maximum while ringing, then put it back. */
    var raiseSystemVolume: Boolean
        get() = prefs.getBoolean(KEY_RAISE, true)
        set(value) { prefs.edit().putBoolean(KEY_RAISE, value).apply() }

    /**
     * The system alarm volume to restore after we raised it, or -1 if we haven't. Stored rather than
     * held in the service so that if the service dies mid-ring the phone isn't left stuck at maximum:
     * the next ring keeps the original value, and the next stop puts it back.
     */
    var rememberedSystemVolume: Int
        get() = prefs.getInt(KEY_REMEMBERED, NONE)
        set(value) { prefs.edit().putInt(KEY_REMEMBERED, value).apply() }

    companion object {
        private const val KEY_LOUDNESS = "alarm_loudness_percent"
        private const val KEY_GRADUAL = "alarm_gradual_volume"
        private const val KEY_RAISE = "alarm_raise_system_volume"
        private const val KEY_REMEMBERED = "alarm_remembered_system_volume"
        const val NONE = -1
        const val DEFAULT_LOUDNESS = 100
        val LOUDNESS_OPTIONS = listOf(25, 50, 75, 100)

        fun sanitizeLoudness(value: Int): Int = if (value in LOUDNESS_OPTIONS) value else DEFAULT_LOUDNESS
    }
}
