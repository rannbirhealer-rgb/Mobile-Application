package com.example.alarmboss.engine

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
     */
    fun nextTrigger(hour: Int, minute: Int, repeatDays: Set<Int>, now: LocalDateTime): LocalDateTime {
        val today = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

        if (repeatDays.isEmpty()) {
            return if (today.isAfter(now)) today else today.plusDays(1)
        }

        // Offset 7 is the same weekday next week, needed when today's slot has already passed.
        for (offset in 0..7) {
            val day = today.plusDays(offset.toLong())
            if (day.dayOfWeek.value in repeatDays && day.isAfter(now)) return day
        }
        // Only reachable if repeatDays holds no valid weekday number; ring tomorrow rather than never.
        return today.plusDays(1)
    }
}
