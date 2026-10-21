package com.example.alarmboss.engine

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmSkipDatesTest {

    private val weekdays = setOf(1, 2, 3, 4, 5)

    @Test
    fun `a repeating alarm steps over a skipped day`() {
        val thursdayEvening = LocalDateTime.of(2026, 12, 24, 20, 0)
        assertEquals(
            LocalDateTime.of(2026, 12, 28, 7, 0),
            AlarmTimeCalculator.nextTrigger(
                7, 0, weekdays, thursdayEvening, skipDates = setOf(LocalDate.of(2026, 12, 25))
            )
        )
    }

    @Test
    fun `a day that is not skipped is unaffected`() {
        val thursdayEvening = LocalDateTime.of(2026, 12, 24, 20, 0)
        assertEquals(
            LocalDateTime.of(2026, 12, 25, 7, 0),
            AlarmTimeCalculator.nextTrigger(
                7, 0, weekdays, thursdayEvening, skipDates = setOf(LocalDate.of(2027, 1, 1))
            )
        )
    }

    @Test
    fun `a weekly alarm skips several weeks in a row`() {
        val thursday = LocalDateTime.of(2026, 10, 15, 8, 0)
        val skips = setOf(LocalDate.of(2026, 10, 22), LocalDate.of(2026, 10, 29))
        assertEquals(
            LocalDateTime.of(2026, 11, 5, 7, 0),
            AlarmTimeCalculator.nextTrigger(7, 0, setOf(4), thursday, skipDates = skips)
        )
    }

    @Test
    fun `a one-off alarm ignores skipped dates`() {
        val evening = LocalDateTime.of(2026, 12, 24, 20, 0)
        assertEquals(
            LocalDateTime.of(2026, 12, 25, 7, 0),
            AlarmTimeCalculator.nextTrigger(
                7, 0, emptySet(), evening, skipDates = setOf(LocalDate.of(2026, 12, 25))
            )
        )
    }

    @Test
    fun `a pinned-date alarm ignores skipped dates`() {
        val evening = LocalDateTime.of(2026, 12, 24, 20, 0)
        val day = LocalDate.of(2026, 12, 25)
        assertEquals(
            LocalDateTime.of(2026, 12, 25, 7, 0),
            AlarmTimeCalculator.nextTrigger(7, 0, emptySet(), evening, day, setOf(day))
        )
    }
}
