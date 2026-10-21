package com.example.alarmboss.engine

import java.time.LocalDate
import java.time.LocalDateTime

object AlarmTimeCalculator {

    /** What should happen to an alarm once it has rung. */
    enum class AfterRing { RESCHEDULE, DISABLE }

    /**
     * Repeating alarms need their next occurrence lined up; a one-off alarm has done its job and
     * must switch itself off, or it sits in the list looking active with nothing scheduled.
     */
    fun afterRing(repeatDays: Set<Int>): AfterRing =
        if (repeatDays.isEmpty()) AfterRing.DISABLE else AfterRing.RESCHEDULE

    /**
     * The next moment an alarm should ring, strictly after [now]. [repeatDays] holds ISO weekday
     * numbers (1 = Monday .. 7 = Sunday); empty means a one-off, which rings today if its time is
     * still ahead and tomorrow otherwise.
     *
     * An alarm pinned to a [specificDate] rings once, at that date and time, and returns null once
     * that moment has passed: there is no "next" occurrence to wait for.
     *
     * A repeating alarm steps over any day in [skipDates] (days the user chose to sleep in on, such
     * as a holiday). One-off and pinned-date alarms ignore it: the user asked for those days by name.
     */
    fun nextTrigger(
        hour: Int,
        minute: Int,
        repeatDays: Set<Int>,
        now: LocalDateTime,
        specificDate: LocalDate? = null,
        skipDates: Set<LocalDate> = emptySet()
    ): LocalDateTime? {
        if (specificDate != null) {
            val moment = specificDate.atTime(hour, minute)
            return if (moment.isAfter(now)) moment else null
        }

        val today = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

        if (repeatDays.isEmpty()) {
            return if (today.isAfter(now)) today else today.plusDays(1)
        }

        // Offset 7 is the same weekday next week, needed when today's slot has already passed. Each
        // skipped date can remove one occurrence, so look a further week ahead per skipped date.
        val lastOffset = 7 * (skipDates.size + 1)
        for (offset in 0..lastOffset) {
            val day = today.plusDays(offset.toLong())
            if (day.dayOfWeek.value in repeatDays && day.isAfter(now) && day.toLocalDate() !in skipDates) {
                return day
            }
        }
        // Only reachable if repeatDays holds no valid weekday number; ring tomorrow rather than never.
        return today.plusDays(1)
    }
}
