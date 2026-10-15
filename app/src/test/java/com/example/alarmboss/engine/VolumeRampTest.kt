package com.example.alarmboss.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeRampTest {

    private val ramp = 30_000L

    @Test
    fun `starts at a soft fraction of the target`() {
        assertEquals(0.15f, VolumeRamp.volumeAt(0, ramp, 1f), 0.001f)
    }

    @Test
    fun `reaches the target at the end of the ramp and stays there`() {
        assertEquals(1f, VolumeRamp.volumeAt(ramp, ramp, 1f), 0.001f)
        assertEquals(1f, VolumeRamp.volumeAt(ramp * 3, ramp, 1f), 0.001f)
    }

    @Test
    fun `rises steadily in between`() {
        val early = VolumeRamp.volumeAt(5_000, ramp, 1f)
        val middle = VolumeRamp.volumeAt(15_000, ramp, 1f)
        val late = VolumeRamp.volumeAt(25_000, ramp, 1f)
        assertTrue(early < middle && middle < late)
        assertEquals(0.575f, middle, 0.001f)
    }

    @Test
    fun `scales with a lower loudness target`() {
        assertEquals(0.5f, VolumeRamp.volumeAt(ramp, ramp, 0.5f), 0.001f)
        assertEquals(0.075f, VolumeRamp.volumeAt(0, ramp, 0.5f), 0.001f)
    }

    @Test
    fun `no ramp means full target immediately`() {
        assertEquals(0.75f, VolumeRamp.volumeAt(0, 0, 0.75f), 0.001f)
    }

    @Test
    fun `a clock reading before the start never goes below the starting volume`() {
        assertEquals(0.15f, VolumeRamp.volumeAt(-500, ramp, 1f), 0.001f)
    }
}
