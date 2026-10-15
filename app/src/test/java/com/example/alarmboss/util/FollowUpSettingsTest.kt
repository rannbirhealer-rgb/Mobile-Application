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
    fun `every offered backup delay is kept, including off`() {
        FollowUpSettings.BACKUP_OPTIONS.forEach { assertEquals(it, FollowUpSettings.sanitizeBackupMinutes(it)) }
        assertTrue(0 in FollowUpSettings.BACKUP_OPTIONS)
    }

    @Test
    fun `an unknown backup delay turns the backup off rather than guessing`() {
        assertEquals(0, FollowUpSettings.sanitizeBackupMinutes(7))
        assertEquals(0, FollowUpSettings.sanitizeBackupMinutes(-5))
        assertEquals(0, FollowUpSettings.sanitizeBackupMinutes(600))
    }

    @Test
    fun `the default delay is one of the options`() {
        assertTrue(FollowUpSettings.DEFAULT_WAKE_CHECK_MINUTES in FollowUpSettings.WAKE_CHECK_OPTIONS)
    }
}
