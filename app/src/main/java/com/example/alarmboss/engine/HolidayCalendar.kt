package com.example.alarmboss.engine

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.TemporalAdjusters

enum class HolidayRegion {
    WORLDWIDE, INDIA, US, UK;

    companion object {
        /** A sensible starting region for a device in [countryCode] (ISO 3166, e.g. "IN"). */
        fun forCountry(countryCode: String?): HolidayRegion = when (countryCode?.uppercase()) {
            "IN" -> INDIA
            "US" -> US
            "GB" -> UK
            else -> WORLDWIDE
        }
    }
}

enum class HolidayKind {
    NEW_YEAR, CHRISTMAS,
    REPUBLIC_DAY, INDEPENDENCE_DAY_IN, GANDHI_JAYANTI,
    MEMORIAL_DAY, INDEPENDENCE_DAY_US, LABOR_DAY, THANKSGIVING,
    GOOD_FRIDAY, EASTER_MONDAY, BOXING_DAY,

    /** A day the user added themselves, for the holidays no fixed rule can predict (lunar festivals). */
    CUSTOM
}

data class Holiday(val kind: HolidayKind, val date: LocalDate)

/**
 * Public holidays worked out on the device from fixed dates and weekday rules, so there is no
 * network call and nothing to keep up to date. Movable lunar and religious festivals (Diwali, Eid,
 * Holi) cannot be computed this way, which is what the user's own [HolidayKind.CUSTOM] days are for.
 */
object HolidayCalendar {

    /** Holidays for [region] from [from] to [to] inclusive, merged with the user's [customDates]. */
    fun between(
        region: HolidayRegion,
        from: LocalDate,
        to: LocalDate,
        customDates: Set<LocalDate> = emptySet()
    ): List<Holiday> {
        if (to.isBefore(from)) return emptyList()
        val builtIn = (from.year..to.year).flatMap { forYear(region, it) }
        val builtInDates = builtIn.map { it.date }.toSet()
        // A custom day that is already a known holiday keeps its proper name.
        val custom = customDates.filter { it !in builtInDates }.map { Holiday(HolidayKind.CUSTOM, it) }
        return (builtIn + custom)
            .filter { !it.date.isBefore(from) && !it.date.isAfter(to) }
            .sortedBy { it.date }
    }

    private fun forYear(region: HolidayRegion, year: Int): List<Holiday> {
        fun at(kind: HolidayKind, month: Month, day: Int) = Holiday(kind, LocalDate.of(year, month, day))
        fun weekday(kind: HolidayKind, month: Month, adjuster: java.time.temporal.TemporalAdjuster) =
            Holiday(kind, LocalDate.of(year, month, 1).with(adjuster))

        val common = listOf(at(HolidayKind.NEW_YEAR, Month.JANUARY, 1), at(HolidayKind.CHRISTMAS, Month.DECEMBER, 25))
        val regional = when (region) {
            HolidayRegion.WORLDWIDE -> emptyList()
            HolidayRegion.INDIA -> listOf(
                at(HolidayKind.REPUBLIC_DAY, Month.JANUARY, 26),
                at(HolidayKind.INDEPENDENCE_DAY_IN, Month.AUGUST, 15),
                at(HolidayKind.GANDHI_JAYANTI, Month.OCTOBER, 2)
            )
            HolidayRegion.US -> listOf(
                weekday(HolidayKind.MEMORIAL_DAY, Month.MAY, TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY)),
                at(HolidayKind.INDEPENDENCE_DAY_US, Month.JULY, 4),
                weekday(HolidayKind.LABOR_DAY, Month.SEPTEMBER, TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY)),
                weekday(HolidayKind.THANKSGIVING, Month.NOVEMBER, TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY))
            )
            HolidayRegion.UK -> {
                val easter = easterSunday(year)
                listOf(
                    Holiday(HolidayKind.GOOD_FRIDAY, easter.minusDays(2)),
                    Holiday(HolidayKind.EASTER_MONDAY, easter.plusDays(1)),
                    at(HolidayKind.BOXING_DAY, Month.DECEMBER, 26)
                )
            }
        }
        return common + regional
    }

    /** Western (Gregorian) Easter Sunday, by the anonymous Gregorian algorithm. */
    fun easterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }
}
