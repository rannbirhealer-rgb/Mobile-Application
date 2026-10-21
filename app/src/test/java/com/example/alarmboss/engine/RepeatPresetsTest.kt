package com.example.alarmboss.engine

import com.example.alarmboss.engine.RepeatPresets.Preset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RepeatPresetsTest {

    @Test
    fun `weekdays are Monday to Friday and weekends are Saturday and Sunday`() {
        assertEquals(setOf(1, 2, 3, 4, 5), RepeatPresets.WEEKDAYS)
        assertEquals(setOf(6, 7), RepeatPresets.WEEKENDS)
        assertEquals(7, RepeatPresets.EVERY_DAY.size)
    }

    @Test
    fun `an exact match is recognised whatever the set's order`() {
        assertEquals(Preset.WEEKDAYS, RepeatPresets.matching(setOf(5, 4, 3, 2, 1)))
        assertEquals(Preset.WEEKENDS, RepeatPresets.matching(setOf(7, 6)))
        assertEquals(Preset.EVERY_DAY, RepeatPresets.matching((1..7).toSet()))
    }

    @Test
    fun `a custom pattern matches no preset`() {
        assertNull(RepeatPresets.matching(setOf(1, 3, 5)))
        assertNull(RepeatPresets.matching(emptySet()))
    }
}
