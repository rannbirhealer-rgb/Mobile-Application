package com.example.alarmboss.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `every medium task type survives a save and load round trip`() {
        val all = MediumTaskType.values().toSet()
        assertEquals(all, converters.toTaskSet(converters.fromTaskSet(all)))
    }

    @Test
    fun `new task types are part of the stored set`() {
        val stored = converters.fromTaskSet(setOf(MediumTaskType.PATTERN_LOCK, MediumTaskType.WORD_SCRAMBLE, MediumTaskType.STEPS))
        assertEquals(setOf(MediumTaskType.PATTERN_LOCK, MediumTaskType.WORD_SCRAMBLE, MediumTaskType.STEPS), converters.toTaskSet(stored))
    }

    @Test
    fun `alarms saved before the new tasks existed still load`() {
        // The exact string an older build wrote for the four original tasks.
        val legacy = "MATH,READING,LYRICS,BARCODE"
        assertEquals(
            setOf(MediumTaskType.MATH, MediumTaskType.READING, MediumTaskType.LYRICS, MediumTaskType.BARCODE),
            converters.toTaskSet(legacy)
        )
    }

    @Test
    fun `blank string loads as an empty set`() {
        assertTrue(converters.toTaskSet("").isEmpty())
    }
}
