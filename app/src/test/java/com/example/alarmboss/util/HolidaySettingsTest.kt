package com.example.alarmboss.util

import com.example.alarmboss.engine.HolidayRegion
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HolidaySettingsTest {

    @Test
    fun `stored dates are read back and bad entries are ignored`() {
        val parsed = HolidaySettings.parseDates(setOf("2026-12-25", "not a date", "", "2027-01-01"))
        assertEquals(setOf(LocalDate.of(2026, 12, 25), LocalDate.of(2027, 1, 1)), parsed)
    }

    @Test
    fun `nothing stored means no dates`() {
        assertEquals(emptySet<LocalDate>(), HolidaySettings.parseDates(null))
    }

    @Test
    fun `pruning drops past days and keeps today onward`() {
        val today = LocalDate.of(2026, 12, 25)
        val dates = setOf(today.minusDays(1), today, today.plusDays(1))
        assertEquals(setOf(today, today.plusDays(1)), HolidaySettings.pruneBefore(dates, today))
    }

    @Test
    fun `a saved region wins over the device country`() {
        assertEquals(HolidayRegion.UK, HolidaySettings.parseRegion("UK", "IN"))
    }

    @Test
    fun `an unknown or missing region falls back to the device country`() {
        assertEquals(HolidayRegion.INDIA, HolidaySettings.parseRegion(null, "IN"))
        assertEquals(HolidayRegion.INDIA, HolidaySettings.parseRegion("MARS", "IN"))
        assertEquals(HolidayRegion.WORLDWIDE, HolidaySettings.parseRegion("MARS", "FR"))
    }
}
