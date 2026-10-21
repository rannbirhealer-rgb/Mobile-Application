package com.example.alarmboss.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmFeedbackPlannerTest {

    @Test
    fun `normally the alarm always sounds and vibration follows the alarm setting`() {
        assertEquals(AlarmFeedbackPlanner.Feedback(sound = true, vibrate = true), AlarmFeedbackPlanner.plan(true, false))
        assertEquals(AlarmFeedbackPlanner.Feedback(sound = true, vibrate = false), AlarmFeedbackPlanner.plan(false, false))
    }

    @Test
    fun `silent mode drops the sound and always vibrates`() {
        assertEquals(AlarmFeedbackPlanner.Feedback(sound = false, vibrate = true), AlarmFeedbackPlanner.plan(true, true))
    }

    @Test
    fun `silent mode vibrates even when the alarm itself has vibration off`() {
        assertEquals(AlarmFeedbackPlanner.Feedback(sound = false, vibrate = true), AlarmFeedbackPlanner.plan(false, true))
    }
}
