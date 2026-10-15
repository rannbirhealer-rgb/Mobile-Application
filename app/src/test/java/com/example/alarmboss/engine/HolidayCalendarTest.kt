package com.example.alarmboss.engine

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HolidayCalendarTest {

    private fun kindsOn(region: HolidayRegion, from: LocalDate, to: LocalDate) =
        HolidayCalendar.between(region, from, to).associate { it.date to it.kind }

    @Test
    fun `easter is computed correctly`() {
        assertEquals(LocalDate.of(2025, 4, 20), HolidayCalendar.easterSunday(2025))
        assertEquals(LocalDate.of(2026, 4, 5), HolidayCalendar.easterSunday(2026))
        assertEquals(LocalDate.of(2027, 3, 28), HolidayCalendar.easterSunday(2027))
    }

    @Test
    fun `every region has new year and christmas`() {
        HolidayRegion.values().forEach { region ->
            val found = kindsOn(region, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)).values
            assertTrue(HolidayKind.NEW_YEAR in found)
            assertTrue(HolidayKind.CHRISTMAS in found)
        }
    }

    @Test
    fun `worldwide has nothing beyond those two`() {
        val found = HolidayCalendar.between(HolidayRegion.WORLDWIDE, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        assertEquals(2, found.size)
    }

    @Test
    fun `india has its national days`() {
        val found = kindsOn(HolidayRegion.INDIA, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        assertEquals(HolidayKind.REPUBLIC_DAY, found[LocalDate.of(2026, 1, 26)])
        assertEquals(HolidayKind.INDEPENDENCE_DAY_IN, found[LocalDate.of(2026, 8, 15)])
        assertEquals(HolidayKind.GANDHI_JAYANTI, found[LocalDate.of(2026, 10, 2)])
    }

    @Test
    fun `us holidays defined by a weekday rule land on the right date`() {
        val found = kindsOn(HolidayRegion.US, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        assertEquals(HolidayKind.MEMORIAL_DAY, found[LocalDate.of(2026, 5, 25)])
        assertEquals(HolidayKind.LABOR_DAY, found[LocalDate.of(2026, 9, 7)])
        assertEquals(HolidayKind.THANKSGIVING, found[LocalDate.of(2026, 11, 26)])
        assertEquals(HolidayKind.INDEPENDENCE_DAY_US, found[LocalDate.of(2026, 7, 4)])
    }

    @Test
    fun `uk easter holidays follow easter`() {
        val found = kindsOn(HolidayRegion.UK, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        assertEquals(HolidayKind.GOOD_FRIDAY, found[LocalDate.of(2026, 4, 3)])
        assertEquals(HolidayKind.EASTER_MONDAY, found[LocalDate.of(2026, 4, 6)])
        assertEquals(HolidayKind.BOXING_DAY, found[LocalDate.of(2026, 12, 26)])
    }

    @Test
    fun `a range crossing the new year includes both years`() {
        val found = kindsOn(HolidayRegion.WORLDWIDE, LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 5))
        assertEquals(setOf(LocalDate.of(2026, 12, 25), LocalDate.of(2027, 1, 1)), found.keys)
    }

    @Test
    fun `results are limited to the range and sorted`() {
        val found = HolidayCalendar.between(HolidayRegion.INDIA, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31))
        assertEquals(found.map { it.date }.sorted(), found.map { it.date })
        assertTrue(found.all { it.date >= LocalDate.of(2026, 8, 1) })
    }

    @Test
    fun `custom days are included and a duplicate keeps the real name`() {
        val christmas = LocalDate.of(2026, 12, 25)
        val diwali = LocalDate.of(2026, 11, 8)
        val found = HolidayCalendar.between(
            HolidayRegion.WORLDWIDE, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31), setOf(diwali, christmas)
        )
        assertEquals(listOf(HolidayKind.CUSTOM, HolidayKind.CHRISTMAS), found.map { it.kind })
    }

    @Test
    fun `an inverted range is empty`() {
        val found = HolidayCalendar.between(HolidayRegion.US, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 4, 1))
        assertTrue(found.isEmpty())
    }

    @Test
    fun `the device country picks a starting region`() {
        assertEquals(HolidayRegion.INDIA, HolidayRegion.forCountry("IN"))
        assertEquals(HolidayRegion.US, HolidayRegion.forCountry("us"))
        assertEquals(HolidayRegion.UK, HolidayRegion.forCountry("GB"))
        assertEquals(HolidayRegion.WORLDWIDE, HolidayRegion.forCountry("FR"))
        assertEquals(HolidayRegion.WORLDWIDE, HolidayRegion.forCountry(null))
    }
}
