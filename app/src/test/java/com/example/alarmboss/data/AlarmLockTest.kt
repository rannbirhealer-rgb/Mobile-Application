package com.example.alarmboss.data

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmLockTest {

    // Thursday 15 October 2026.
    private fun at(hour: Int, minute: Int) = LocalDateTime.of(2026, 10, 15, hour, minute)

    private fun alarm(
        hour: Int,
        minute: Int,
        enabled: Boolean = true,
        repeatDays: Set<Int> = emptySet(),
        specificDate: String? = null
    ) = Alarm(hour = hour, minute = minute, isEnabled = enabled, repeatDays = repeatDays, specificDate = specificDate)

    @Test
    fun `disabled alarms are never locked`() {
        assertFalse(isAlarmLocked(alarm(7, 0, enabled = false), at(6, 0)))
    }

    @Test
    fun `alarm one hour away is locked`() {
        assertTrue(isAlarmLocked(alarm(7, 0), at(6, 0)))
    }

    @Test
    fun `alarm three hours away is not locked`() {
        assertFalse(isAlarmLocked(alarm(9, 0), at(6, 0)))
    }

    @Test
    fun `alarm exactly at the two hour boundary is locked`() {
        assertTrue(isAlarmLocked(alarm(8, 0), at(6, 0)))
    }

    @Test
    fun `alarm time already passed today is treated as tomorrow and not locked`() {
        assertFalse(isAlarmLocked(alarm(7, 0), at(20, 0)))
    }

    @Test
    fun `alarm just under two hours from firing tomorrow is locked`() {
        assertTrue(isAlarmLocked(alarm(1, 0), at(23, 15)))
    }

    @Test
    fun `a weekday alarm is not locked on a Friday night when its next ring is Monday`() {
        val friday = LocalDateTime.of(2026, 10, 16, 23, 0)
        assertFalse(isAlarmLocked(alarm(0, 30, repeatDays = setOf(1, 2, 3, 4, 5)), friday))
    }

    @Test
    fun `a weekday alarm is locked on the night before a weekday`() {
        val thursdayNight = LocalDateTime.of(2026, 10, 15, 23, 30)
        assertTrue(isAlarmLocked(alarm(0, 30, repeatDays = setOf(1, 2, 3, 4, 5)), thursdayNight))
    }

    @Test
    fun `an alarm set for a date weeks away is not locked at its time of day`() {
        assertFalse(isAlarmLocked(alarm(7, 0, specificDate = "2026-11-20"), at(6, 0)))
    }

    @Test
    fun `an alarm set for today is locked within two hours of its time`() {
        assertTrue(isAlarmLocked(alarm(7, 0, specificDate = LocalDate.of(2026, 10, 15).toString()), at(6, 0)))
    }

    @Test
    fun `an alarm whose date has passed is not locked`() {
        assertFalse(isAlarmLocked(alarm(7, 0, specificDate = "2026-10-01"), at(6, 0)))
    }
}
