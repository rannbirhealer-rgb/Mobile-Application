package com.example.alarmboss.data

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HolidaySkipLockTest {

    private val weekdayAlarm = Alarm(hour = 7, minute = 0, repeatDays = setOf(1, 2, 3, 4, 5))
    private val skipChristmas = setOf(LocalDate.of(2026, 12, 25))

    @Test
    fun `a skipped day leaves the alarm unlocked in the hours before it`() {
        val earlyMorning = LocalDateTime.of(2026, 12, 25, 5, 30)
        assertTrue(isAlarmLocked(weekdayAlarm, earlyMorning))
        assertFalse(isAlarmLocked(weekdayAlarm, earlyMorning, skipChristmas))
    }

    @Test
    fun `the lock uses the next ring that will really happen`() {
        // Friday is skipped, so the next ring is Monday: well outside the window on Thursday night.
        val thursdayNight = LocalDateTime.of(2026, 12, 24, 23, 0)
        assertFalse(isAlarmLocked(weekdayAlarm, thursdayNight, skipChristmas))
    }
}
