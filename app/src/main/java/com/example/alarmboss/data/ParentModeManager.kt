package com.example.alarmboss.data

import android.content.Context
import android.os.SystemClock
import com.example.alarmboss.util.PinSecurity

class ParentModeManager(context: Context) {
    private val prefs = context.getSharedPreferences("alarmboss_parent_mode_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACTIVE = "parent_mode_active"
        private const val KEY_PARENT_NAME = "parent_name"
        private const val KEY_PARENT_EMAIL = "parent_email"

        /** The PIN as first stored, in the clear. Read once to upgrade it, then deleted. */
        private const val KEY_LEGACY_PIN = "parent_pin"
        private const val KEY_PIN_HASH = "parent_pin_hash"
        private const val KEY_PIN_SALT = "parent_pin_salt"
        private const val KEY_FAILED_ATTEMPTS = "pin_failed_attempts"
        private const val KEY_LOCKED_UNTIL = "pin_locked_until_elapsed"
        private const val KEY_CHILD_NAME = "child_name"
        private const val KEY_CHILD_AGE = "child_age"
        private const val KEY_STRICT_LOCK = "parent_strict_lock"
        private const val KEY_SCHOOL_WAKE_HOUR = "school_wake_hour"
        private const val KEY_SCHOOL_WAKE_MINUTE = "school_wake_minute"
        private const val KEY_BEDTIME_HOUR = "bedtime_hour"
        private const val KEY_BEDTIME_MINUTE = "bedtime_minute"
    }

    fun isParentModeActive(): Boolean = prefs.getBoolean(KEY_ACTIVE, false)

    /** True when alarms are guarded: Parent Mode is on and its strict lock is enabled. */
    fun isLockActive(): Boolean = isParentModeActive() && isStrictLockEnabled()

    /**
     * Checks a PIN. Wrong guesses are counted, and after a few the PIN is refused outright for a
     * growing time (see [lockedOutForMillis]), so the four digits cannot simply be tried in turn.
     */
    fun verifyPin(enteredPin: String): Boolean {
        if (lockedOutForMillis() > 0L) return false
        val ok = pinMatches(enteredPin.trim())
        if (ok) {
            prefs.edit().remove(KEY_FAILED_ATTEMPTS).remove(KEY_LOCKED_UNTIL).apply()
        } else {
            val failed = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            val lock = PinSecurity.lockoutMillis(failed)
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, failed)
                .putLong(KEY_LOCKED_UNTIL, if (lock > 0L) SystemClock.elapsedRealtime() + lock else 0L)
                .apply()
        }
        return ok
    }

    /**
     * Milliseconds until PIN entry is allowed again, or 0. Measured on the elapsed-time clock, which
     * changing the date or time cannot move; a reboot resets that clock, so a remaining time longer
     * than any lockout can be is read as stale and ignored.
     */
    fun lockedOutForMillis(): Long {
        val remaining = prefs.getLong(KEY_LOCKED_UNTIL, 0L) - SystemClock.elapsedRealtime()
        return if (remaining in 1..PinSecurity.lockoutMillis(Int.MAX_VALUE)) remaining else 0L
    }

    private fun pinMatches(pin: String): Boolean {
        val hash = prefs.getString(KEY_PIN_HASH, null)
        val salt = prefs.getString(KEY_PIN_SALT, null)
        if (hash != null && salt != null) return PinSecurity.matches(pin, salt, hash)

        // A PIN stored by an earlier version. Accept it once, and replace it with a hash.
        val legacy = prefs.getString(KEY_LEGACY_PIN, "").orEmpty()
        if (legacy.isEmpty() || legacy != pin) return false
        storePin(pin)
        return true
    }

    private fun storePin(pin: String) {
        val salt = PinSecurity.newSalt()
        prefs.edit()
            .putString(KEY_PIN_SALT, salt)
            .putString(KEY_PIN_HASH, PinSecurity.hash(pin, salt))
            .remove(KEY_LEGACY_PIN)
            .apply()
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

    /** Turns Parent Mode on. Returns false, changing nothing, if the PIN is not exactly four digits. */
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
    ): Boolean {
        val cleanPin = pin.trim()
        if (!PinSecurity.isValidPin(cleanPin)) return false
        storePin(cleanPin)
        prefs.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_PARENT_NAME, parentName.trim())
            .putString(KEY_PARENT_EMAIL, parentEmail.trim())
            .putString(KEY_CHILD_NAME, childName.trim())
            .putInt(KEY_CHILD_AGE, childAge.coerceIn(9, 12))
            .putInt(KEY_SCHOOL_WAKE_HOUR, schoolWakeHour)
            .putInt(KEY_SCHOOL_WAKE_MINUTE, schoolWakeMinute)
            .putInt(KEY_BEDTIME_HOUR, bedtimeHour)
            .putInt(KEY_BEDTIME_MINUTE, bedtimeMinute)
            .putBoolean(KEY_STRICT_LOCK, strictLock)
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKED_UNTIL)
            .apply()
        return true
    }

    /** Turns the strict lock on. Always allowed; only turning it off is guarded. */
    fun enableStrictLock() {
        prefs.edit().putBoolean(KEY_STRICT_LOCK, true).apply()
    }

    /**
     * Turns the strict lock off. The caller must already have checked the PIN (see ParentPinDialog):
     * the lock exists to stop exactly this, so it is never done without one.
     */
    fun disableStrictLockAfterPinCheck() {
        prefs.edit().putBoolean(KEY_STRICT_LOCK, false).apply()
    }

    /** Leaves Parent Mode. The caller must already have checked the PIN. */
    fun disableParentModeAfterPinCheck() {
        prefs.edit().putBoolean(KEY_ACTIVE, false).apply()
    }
}
