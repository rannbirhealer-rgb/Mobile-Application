package com.example.alarmboss.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmModelTest {

    @Test
    fun `default alarm has new features off by default`() {
        val alarm = Alarm(
            hour = 7,
            minute = 0
        )
        assertFalse(alarm.dynamicBrightness)
        assertFalse(alarm.flashLightBlink)
        assertFalse(alarm.gradualVolume)
        assertFalse(alarm.sunriseMode)
        assertFalse(alarm.wakeCheck)
        assertFalse(alarm.morningBriefing)
    }

    @Test
    fun `alarm with all advanced features enabled preserves state`() {
        val alarm = Alarm(
            hour = 8,
            minute = 30,
            dynamicBrightness = true,
            flashLightBlink = true,
            gradualVolume = true,
            sunriseMode = true,
            wakeCheck = true,
            morningBriefing = true
        )
        assertTrue(alarm.dynamicBrightness)
        assertTrue(alarm.flashLightBlink)
        assertTrue(alarm.gradualVolume)
        assertTrue(alarm.sunriseMode)
        assertTrue(alarm.wakeCheck)
        assertTrue(alarm.morningBriefing)
    }
}
