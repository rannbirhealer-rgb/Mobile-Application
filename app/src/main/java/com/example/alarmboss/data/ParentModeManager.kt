package com.example.alarmboss.data

import android.content.Context

class ParentModeManager(context: Context) {
    private val prefs = context.getSharedPreferences("alarmboss_parent_mode_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACTIVE = "parent_mode_active"
        private const val KEY_PARENT_NAME = "parent_name"
        private const val KEY_PARENT_EMAIL = "parent_email"
        private const val KEY_PARENT_PIN = "parent_pin"
        private const val KEY_CHILD_NAME = "child_name"
        private const val KEY_CHILD_AGE = "child_age"
        private const val KEY_STRICT_LOCK = "parent_strict_lock"
        private const val KEY_SCHOOL_WAKE_HOUR = "school_wake_hour"
        private const val KEY_SCHOOL_WAKE_MINUTE = "school_wake_minute"
        private const val KEY_BEDTIME_HOUR = "bedtime_hour"
        private const val KEY_BEDTIME_MINUTE = "bedtime_minute"
    }

    fun isParentModeActive(): Boolean = prefs.getBoolean(KEY_ACTIVE, false)

    fun verifyPin(enteredPin: String): Boolean {
        val stored = prefs.getString(KEY_PARENT_PIN, "") ?: ""
        return stored.isNotEmpty() && stored == enteredPin.trim()
    }

    fun getParentName(): String = prefs.getString(KEY_PARENT_NAME, "") ?: ""
    fun getParentEmail(): String = prefs.getString(KEY_PARENT_EMAIL, "") ?: ""
    fun getChildName(): String = prefs.getString(KEY_CHILD_NAME, "My Child") ?: "My Child"
    fun getChildAge(): Int = prefs.getInt(KEY_CHILD_AGE, 10)
    fun isStrictLockEnabled(): Boolean = prefs.getBoolean(KEY_STRICT_LOCK, true)

    fun getSchoolWakeHour(): Int = prefs.getInt(KEY_SCHOOL_WAKE_HOUR, 6)
    fun getSchoolWakeMinute(): Int = prefs.getInt(KEY_SCHOOL_WAKE_MINUTE, 45)
    fun getBedtimeHour(): Int = prefs.getInt(KEY_BEDTIME_HOUR, 20)
    fun getBedtimeMinute(): Int = prefs.getInt(KEY_BEDTIME_MINUTE, 30)

    fun setupParentMode(
        parentName: String,
        parentEmail: String,
        pin: String,
        childName: String,
        childAge: Int,
        schoolWakeHour: Int = 6,
        schoolWakeMinute: Int = 45,
        bedtimeHour: Int = 20,
        bedtimeMinute: Int = 30,
        strictLock: Boolean = true
    ) {
        prefs.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_PARENT_NAME, parentName.trim())
            .putString(KEY_PARENT_EMAIL, parentEmail.trim())
            .putString(KEY_PARENT_PIN, pin.trim())
            .putString(KEY_CHILD_NAME, childName.trim())
            .putInt(KEY_CHILD_AGE, childAge.coerceIn(9, 12))
            .putInt(KEY_SCHOOL_WAKE_HOUR, schoolWakeHour)
            .putInt(KEY_SCHOOL_WAKE_MINUTE, schoolWakeMinute)
            .putInt(KEY_BEDTIME_HOUR, bedtimeHour)
            .putInt(KEY_BEDTIME_MINUTE, bedtimeMinute)
            .putBoolean(KEY_STRICT_LOCK, strictLock)
            .apply()
    }

    fun updateStrictLock(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STRICT_LOCK, enabled).apply()
    }

    fun disableParentMode(pin: String): Boolean {
        if (!verifyPin(pin)) return false
        prefs.edit().putBoolean(KEY_ACTIVE, false).apply()
        return true
    }
}
