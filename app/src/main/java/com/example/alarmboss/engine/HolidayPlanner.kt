package com.example.alarmboss.engine

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Decides which upcoming holidays are worth asking about. Nothing is ever skipped silently: this
 * only produces the questions, and an alarm is skipped only after the user answers yes.
 */
object HolidayPlanner {

    /** How far ahead to ask, so a decision is made well before the day rather than at the last moment. */
    const val LOOKAHEAD_DAYS = 7L

    /** The part of an alarm that decides whether a holiday touches it. */
    data class Slot(val hour: Int, val minute: Int, val repeatDays: Set<Int>)

    data class Prompt(val holiday: Holiday, val affectedAlarms: Int)

    /**
     * The holidays to ask about, soonest first. A holiday qualifies when it has not been answered
     * yet and at least one repeating alarm would ring on it more than [minLead] from [now]. The
     * lead requirement keeps this from becoming a way round the lock on an alarm about to ring:
     * "skip" is only on offer while the alarm is still freely editable.
     */
    fun pending(
        holidays: List<Holiday>,
        slots: List<Slot>,
        answered: Set<LocalDate>,
        now: LocalDateTime,
        minLead: Duration
    ): List<Prompt> = holidays
        .filter { it.date !in answered }
        .mapNotNull { holiday ->
            val affected = slots.count { slot ->
                slot.repeatDays.isNotEmpty() &&
                    holiday.date.dayOfWeek.value in slot.repeatDays &&
                    holiday.date.atTime(slot.hour, slot.minute).isAfter(now.plus(minLead))
            }
            if (affected > 0) Prompt(holiday, affected) else null
        }
        .sortedBy { it.holiday.date }
}
