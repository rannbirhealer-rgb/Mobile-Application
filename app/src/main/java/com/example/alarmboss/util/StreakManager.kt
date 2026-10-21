package com.example.alarmboss.util

import android.content.Context
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class StreakManager(context: Context) {
    private val prefs = context.getSharedPreferences("alarm_boss_prefs", Context.MODE_PRIVATE)

    private fun lastWakeDate(): LocalDate? = parseDate(prefs.getString(KEY_LAST_DATE, null))

    /** The streak that is still alive today: one broken by a missed day reads 0, not its old value. */
    fun getCurrentStreak(today: LocalDate = LocalDate.now()): Int =
        activeStreak(prefs.getInt(KEY_STREAK, 0), lastWakeDate(), today)

    /** The streak as it will stand once today's wake-up is recorded, without recording it. */
    fun projectedStreak(today: LocalDate = LocalDate.now()): Int =
        nextStreak(getCurrentStreak(today), lastWakeDate(), today)

    data class StreakResult(
        val currentStreak: Int,
        val isMorningWindow: Boolean,
        val streakIncremented: Boolean
    )

    /** Call once per alarm the user actually wakes up to and dismisses. Returns the StreakResult.
     * Streaks are strictly awarded for wake-ups completed within the morning window (5:00 AM - 10:00 AM).
     */
    fun recordSuccessfulWakeUpDetailed(
        today: LocalDate = LocalDate.now(),
        time: java.time.LocalTime = java.time.LocalTime.now()
    ): StreakResult {
        val current = getCurrentStreak(today)
        val inWindow = isMorningWindow(time)

        if (!inWindow) {
            // Late mornings (> 10 AM) or early hours (< 5 AM) do not award or advance morning streaks.
            return StreakResult(
                currentStreak = current,
                isMorningWindow = false,
                streakIncremented = false
            )
        }

        val newStreak = projectedStreak(today)
        prefs.edit()
            .putInt(KEY_STREAK, newStreak)
            .putString(KEY_LAST_DATE, today.toString())
            .apply()
        return StreakResult(
            currentStreak = newStreak,
            isMorningWindow = true,
            streakIncremented = newStreak > current
        )
    }

    /** Overload returning Int for backwards compatibility. */
    fun recordSuccessfulWakeUp(today: LocalDate = LocalDate.now()): Int {
        return recordSuccessfulWakeUpDetailed(today).currentStreak
    }

    companion object {
        private const val KEY_STREAK = "current_streak"
        private const val KEY_LAST_DATE = "last_success_date"

        fun parseDate(value: String?): LocalDate? =
            value?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        /**
         * Pure streak transition, kept separate from SharedPreferences so it's unit testable:
         * same day is a no-op, the next day extends the streak, any gap (or a first wake-up)
         * restarts it at 1.
         */
        fun nextStreak(currentStreak: Int, lastDate: LocalDate?, today: LocalDate): Int {
            if (lastDate == null) return 1
            val daysBetween = ChronoUnit.DAYS.between(lastDate, today)
            return when {
                daysBetween <= 0L -> currentStreak
                daysBetween == 1L -> currentStreak + 1
                else -> 1
            }
        }

        /**
         * What to display today. A stored streak is only kept alive by waking up today or
         * yesterday; once a full day has been missed it is over, even though nothing has
         * recorded that yet (the reset only happens at the next wake-up).
         */
        fun activeStreak(storedStreak: Int, lastDate: LocalDate?, today: LocalDate): Int {
            if (lastDate == null) return 0
            return if (ChronoUnit.DAYS.between(lastDate, today) > 1L) 0 else storedStreak
        }

        const val MORNING_WINDOW_START_HOUR = 5
        const val MORNING_WINDOW_END_HOUR = 10

        /**
         * Checks if the given time falls strictly within the morning window (5:00 AM - 10:00 AM).
         * Late mornings (> 10:00:00 AM) and early dawn (< 5:00:00 AM) return false.
         */
        fun isMorningWindow(time: java.time.LocalTime = java.time.LocalTime.now()): Boolean {
            val hour = time.hour
            return (hour in MORNING_WINDOW_START_HOUR until MORNING_WINDOW_END_HOUR) ||
                    (hour == MORNING_WINDOW_END_HOUR && time.minute == 0 && time.second == 0)
        }
    }
}
