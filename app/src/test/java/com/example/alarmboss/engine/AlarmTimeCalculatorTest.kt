package com.example.alarmboss.engine

import com.example.alarmboss.engine.AlarmTimeCalculator.AfterRing
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmTimeCalculatorTest {

    // Thursday 15 October 2026, 08:30.
    private val now = LocalDateTime.of(2026, 10, 15, 8, 30)

    private val weekdays = setOf(1, 2, 3, 4, 5)

    @Test
    fun `a one-off whose time is still ahead rings today`() {
        assertEquals(
            LocalDateTime.of(2026, 10, 15, 9, 0),
            AlarmTimeCalculator.nextTrigger(9, 0, emptySet(), now)
        )
    }

    @Test
    fun `a one-off whose time has passed rings tomorrow`() {
        assertEquals(
            LocalDateTime.of(2026, 10, 16, 7, 0),
            AlarmTimeCalculator.nextTrigger(7, 0, emptySet(), now)
        )
    }

    @Test
    fun `a one-off at exactly the current minute does not ring immediately`() {
        assertEquals(
            LocalDateTime.of(2026, 10, 16, 8, 30),
            AlarmTimeCalculator.nextTrigger(8, 30, emptySet(), now)
        )
    }

    @Test
    fun `a repeating alarm still ahead today rings today`() {
        assertEquals(
            LocalDateTime.of(2026, 10, 15, 9, 0),
            AlarmTimeCalculator.nextTrigger(9, 0, weekdays, now)
        )
    }

    @Test
    fun `a weekday alarm that already rang today next rings on the following weekday`() {
        // Thursday 07:00 has passed, so Friday.
        assertEquals(
            LocalDateTime.of(2026, 10, 16, 7, 0),
            AlarmTimeCalculator.nextTrigger(7, 0, weekdays, now)
        )
    }

    @Test
    fun `a weekday alarm that rang on Friday skips the weekend`() {
        val friday = LocalDateTime.of(2026, 10, 16, 7, 0, 1)
        assertEquals(
            LocalDateTime.of(2026, 10, 19, 7, 0),
            AlarmTimeCalculator.nextTrigger(7, 0, weekdays, friday)
        )
    }

    @Test
    fun `a single-day alarm that already rang today waits a full week`() {
        // Thursday only (4); Thursday 07:00 has passed.
        assertEquals(
            LocalDateTime.of(2026, 10, 22, 7, 0),
            AlarmTimeCalculator.nextTrigger(7, 0, setOf(4), now)
        )
    }

    @Test
    fun `invalid weekday numbers fall back to tomorrow instead of never ringing`() {
        assertEquals(
            LocalDateTime.of(2026, 10, 16, 7, 0),
            AlarmTimeCalculator.nextTrigger(7, 0, setOf(0, 9), now)
        )
    }

    @Test
    fun `repeating alarms are rescheduled after ringing and one-offs are switched off`() {
        assertEquals(AfterRing.RESCHEDULE, AlarmTimeCalculator.afterRing(weekdays))
        assertEquals(AfterRing.DISABLE, AlarmTimeCalculator.afterRing(emptySet()))
    }
}
