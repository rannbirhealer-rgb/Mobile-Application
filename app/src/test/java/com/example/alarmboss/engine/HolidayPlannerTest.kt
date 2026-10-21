package com.example.alarmboss.engine

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HolidayPlannerTest {

    private val lead = Duration.ofHours(2)
    private val christmas = Holiday(HolidayKind.CHRISTMAS, LocalDate.of(2026, 12, 25)) // a Friday
    private val weekdayAlarm = HolidayPlanner.Slot(7, 0, setOf(1, 2, 3, 4, 5))
    private val now = LocalDateTime.of(2026, 12, 20, 12, 0)

    private fun pending(
        slots: List<HolidayPlanner.Slot>,
        answered: Set<LocalDate> = emptySet(),
        at: LocalDateTime = now,
        holidays: List<Holiday> = listOf(christmas)
    ) = HolidayPlanner.pending(holidays, slots, answered, at, lead)

    @Test
    fun `a holiday on a repeat day is offered with the number of alarms it affects`() {
        val prompts = pending(listOf(weekdayAlarm, HolidayPlanner.Slot(8, 0, setOf(5))))
        assertEquals(1, prompts.size)
        assertEquals(2, prompts[0].affectedAlarms)
    }

    @Test
    fun `a holiday on a day the alarm does not repeat is not offered`() {
        assertTrue(pending(listOf(HolidayPlanner.Slot(7, 0, setOf(6, 7)))).isEmpty())
    }

    @Test
    fun `one-off alarms are never offered a skip`() {
        assertTrue(pending(listOf(HolidayPlanner.Slot(7, 0, emptySet()))).isEmpty())
    }

    @Test
    fun `an answered holiday is not asked again`() {
        assertTrue(pending(listOf(weekdayAlarm), answered = setOf(christmas.date)).isEmpty())
    }

    @Test
    fun `no skip is offered for an alarm inside the lock window`() {
        val earlyMorning = LocalDateTime.of(2026, 12, 25, 5, 30) // the 7:00 alarm is 90 minutes away
        assertTrue(pending(listOf(weekdayAlarm), at = earlyMorning).isEmpty())
    }

    @Test
    fun `the skip is offered the evening before`() {
        val eve = LocalDateTime.of(2026, 12, 24, 21, 0)
        assertEquals(1, pending(listOf(weekdayAlarm), at = eve).size)
    }

    @Test
    fun `an alarm that already rang today is not counted`() {
        val afterRing = LocalDateTime.of(2026, 12, 25, 9, 0)
        assertTrue(pending(listOf(weekdayAlarm), at = afterRing).isEmpty())
    }

    @Test
    fun `prompts come soonest first`() {
        val later = Holiday(HolidayKind.NEW_YEAR, LocalDate.of(2027, 1, 1)) // a Friday
        val prompts = pending(listOf(weekdayAlarm), holidays = listOf(later, christmas))
        assertEquals(listOf(christmas.date, later.date), prompts.map { it.holiday.date })
    }

    @Test
    fun `no alarms means no questions`() {
        assertTrue(pending(emptyList()).isEmpty())
    }
}
