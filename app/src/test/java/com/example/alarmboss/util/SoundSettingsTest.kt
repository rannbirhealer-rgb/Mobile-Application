package com.example.alarmboss.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundSettingsTest {

    @Test
    fun `every offered loudness is kept`() {
        SoundSettings.LOUDNESS_OPTIONS.forEach { assertEquals(it, SoundSettings.sanitizeLoudness(it)) }
    }

    @Test
    fun `anything else falls back to full loudness`() {
        assertEquals(SoundSettings.DEFAULT_LOUDNESS, SoundSettings.sanitizeLoudness(0))
        assertEquals(SoundSettings.DEFAULT_LOUDNESS, SoundSettings.sanitizeLoudness(60))
        assertEquals(SoundSettings.DEFAULT_LOUDNESS, SoundSettings.sanitizeLoudness(500))
    }

    @Test
    fun `the default is one of the options`() {
        assertTrue(SoundSettings.DEFAULT_LOUDNESS in SoundSettings.LOUDNESS_OPTIONS)
    }
}
