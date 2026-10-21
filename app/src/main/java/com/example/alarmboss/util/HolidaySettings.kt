package com.example.alarmboss.util

import android.content.Context
import com.example.alarmboss.engine.HolidayRegion
import java.time.LocalDate
import java.util.Locale

/** Settings and decisions for skipping repeating alarms on holidays. */
class HolidaySettings(context: Context) {
    private val prefs = context.getSharedPreferences("alarm_boss_settings", Context.MODE_PRIVATE)

    /** Off by default: nothing is asked and nothing is skipped until the user opts in. */
    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) { prefs.edit().putBoolean(KEY_ENABLED, value).apply() }

    var region: HolidayRegion
        get() = parseRegion(prefs.getString(KEY_REGION, null), Locale.getDefault().country)
        set(value) { prefs.edit().putString(KEY_REGION, value.name).apply() }

    /** Days the user marked themselves, for holidays no rule can predict. */
    val customDates: Set<LocalDate> get() = read(KEY_CUSTOM)

    /** Dates the user confirmed skipping. Only repeating alarms are affected. */
    val skippedDates: Set<LocalDate> get() = read(KEY_SKIPPED)

    /** Dates already asked about, whichever way the answer went, so each is asked once. */
    val answeredDates: Set<LocalDate> get() = read(KEY_ANSWERED)

    /** The dates the scheduler should step over: none while the feature is off. */
    val activeSkipDates: Set<LocalDate> get() = if (enabled) skippedDates else emptySet()

    fun addCustomDate(date: LocalDate) = write(KEY_CUSTOM, customDates + date)

    fun removeCustomDate(date: LocalDate) = write(KEY_CUSTOM, customDates - date)

    /** The user said yes: skip repeating alarms on [date]. */
    fun skip(date: LocalDate) {
        write(KEY_SKIPPED, skippedDates + date)
        write(KEY_ANSWERED, answeredDates + date)
    }

    /** The user said no: keep ringing on [date], and don't ask again. */
    fun keepRinging(date: LocalDate) {
        write(KEY_SKIPPED, skippedDates - date)
        write(KEY_ANSWERED, answeredDates + date)
    }

    /** Drops decisions about days that are over, so the stored sets don't grow forever. */
    fun prune(today: LocalDate) {
        // Only write when something is actually dropped; every write wakes the preference listeners.
        listOf(KEY_SKIPPED, KEY_ANSWERED).forEach { key ->
            val stored = read(key)
            val kept = pruneBefore(stored, today)
            if (kept != stored) write(key, kept)
        }
    }

    private fun read(key: String): Set<LocalDate> = parseDates(prefs.getStringSet(key, null))

    private fun write(key: String, dates: Set<LocalDate>) {
        prefs.edit().putStringSet(key, dates.map { it.toString() }.toSet()).apply()
    }

    companion object {
        private const val KEY_ENABLED = "holiday_enabled"
        private const val KEY_REGION = "holiday_region"
        private const val KEY_CUSTOM = "holiday_custom_dates"
        private const val KEY_SKIPPED = "holiday_skipped_dates"
        private const val KEY_ANSWERED = "holiday_answered_dates"

        /** Stored ISO dates as dates, ignoring anything unreadable rather than failing. */
        fun parseDates(raw: Set<String>?): Set<LocalDate> =
            raw.orEmpty().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()

        fun pruneBefore(dates: Set<LocalDate>, today: LocalDate): Set<LocalDate> =
            dates.filterNot { it.isBefore(today) }.toSet()

        /** The saved region, or the one that suits [countryCode] when nothing valid is saved. */
        fun parseRegion(saved: String?, countryCode: String?): HolidayRegion =
            HolidayRegion.values().firstOrNull { it.name == saved } ?: HolidayRegion.forCountry(countryCode)
    }
}
