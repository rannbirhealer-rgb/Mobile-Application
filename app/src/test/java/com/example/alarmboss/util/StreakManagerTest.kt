package com.example.alarmboss.util

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreakManagerTest {

    private val today = LocalDate.of(2026, 9, 22)

    @Test
    fun `first ever wake-up starts streak at 1`() {
        assertEquals(1, StreakManager.nextStreak(0, null, today))
    }

    @Test
    fun `waking up the day after the last success extends the streak`() {
        assertEquals(6, StreakManager.nextStreak(5, today.minusDays(1), today))
    }

    @Test
    fun `waking up again the same day does not double-count`() {
        assertEquals(5, StreakManager.nextStreak(5, today, today))
    }

    @Test
    fun `a gap of a missed day restarts the streak at 1`() {
        assertEquals(1, StreakManager.nextStreak(9, today.minusDays(2), today))
    }

    @Test
    fun `a streak woken up today is still active`() {
        assertEquals(5, StreakManager.activeStreak(5, today, today))
    }

    @Test
    fun `a streak last extended yesterday is still active`() {
        assertEquals(5, StreakManager.activeStreak(5, today.minusDays(1), today))
    }

    @Test
    fun `a streak is over once a full day has been missed`() {
        assertEquals(0, StreakManager.activeStreak(5, today.minusDays(2), today))
        assertEquals(0, StreakManager.activeStreak(5, today.minusDays(30), today))
    }

    @Test
    fun `no recorded wake-up means no active streak`() {
        assertEquals(0, StreakManager.activeStreak(3, null, today))
    }

    @Test
    fun `an unreadable stored date is treated as missing`() {
        assertNull(StreakManager.parseDate("not-a-date"))
        assertNull(StreakManager.parseDate(null))
        assertEquals(today, StreakManager.parseDate("2026-09-22"))
    }
    @Test
    fun `morning window is strictly 5 AM to 10 AM`() {
        // Before 5 AM
        org.junit.Assert.assertFalse(StreakManager.isMorningWindow(java.time.LocalTime.of(4, 59, 59)))
        org.junit.Assert.assertFalse(StreakManager.isMorningWindow(java.time.LocalTime.of(3, 0, 0)))

        // Strictly within 5 AM to 10 AM
        org.junit.Assert.assertTrue(StreakManager.isMorningWindow(java.time.LocalTime.of(5, 0, 0)))
        org.junit.Assert.assertTrue(StreakManager.isMorningWindow(java.time.LocalTime.of(6, 30, 0)))
        org.junit.Assert.assertTrue(StreakManager.isMorningWindow(java.time.LocalTime.of(9, 59, 59)))
        org.junit.Assert.assertTrue(StreakManager.isMorningWindow(java.time.LocalTime.of(10, 0, 0)))

        // Late mornings (> 10 AM)
        org.junit.Assert.assertFalse(StreakManager.isMorningWindow(java.time.LocalTime.of(10, 0, 1)))
        org.junit.Assert.assertFalse(StreakManager.isMorningWindow(java.time.LocalTime.of(10, 30, 0)))
        org.junit.Assert.assertFalse(StreakManager.isMorningWindow(java.time.LocalTime.of(14, 0, 0)))
    }
}
