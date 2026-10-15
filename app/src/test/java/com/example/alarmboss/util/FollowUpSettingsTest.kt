package com.example.alarmboss.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowUpSettingsTest {

    @Test
    fun `every offered wake-check delay is kept`() {
        FollowUpSettings.WAKE_CHECK_OPTIONS.forEach {
            assertEquals(it, FollowUpSettings.sanitizeWakeCheckMinutes(it))
        }
    }

    @Test
    fun `anything else falls back to the default delay`() {
        assertEquals(FollowUpSettings.DEFAULT_WAKE_CHECK_MINUTES, FollowUpSettings.sanitizeWakeCheckMinutes(0))
        assertEquals(FollowUpSettings.DEFAULT_WAKE_CHECK_MINUTES, FollowUpSettings.sanitizeWakeCheckMinutes(7))
        assertEquals(FollowUpSettings.DEFAULT_WAKE_CHECK_MINUTES, FollowUpSettings.sanitizeWakeCheckMinutes(-3))
    }

    @Test
    fun `the default delay is one of the options`() {
        assertTrue(FollowUpSettings.DEFAULT_WAKE_CHECK_MINUTES in FollowUpSettings.WAKE_CHECK_OPTIONS)
    }
}
