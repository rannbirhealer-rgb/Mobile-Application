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

    @Test
    fun `an unknown task name is dropped and the rest still load`() {
        // What a newer build's extra task looks like to an older one, or a hand-edited backup.
        assertEquals(
            setOf(MediumTaskType.MATH, MediumTaskType.READING),
            converters.toTaskSet("MATH,TELEPORT,READING")
        )
    }

    @Test
    fun `a stored set of only unknown tasks loads empty instead of crashing`() {
        assertTrue(converters.toTaskSet("TELEPORT,").isEmpty())
    }

    @Test
    fun `repeat days ignore anything that is not a number`() {
        assertEquals(setOf(1, 3, 5), converters.toIntSet("1, 3,x,,5"))
        assertTrue(converters.toIntSet("").isEmpty())
    }

    @Test
    fun `an unknown mode falls back to easy and an unknown exercise to squats`() {
        assertEquals(AlarmMode.EASY, converters.toMode("NIGHTMARE"))
        assertEquals(AlarmMode.STRICT, converters.toMode("STRICT"))
        assertEquals(ExerciseType.SQUATS, converters.toExerciseType("PUSHUPS"))
        assertEquals(ExerciseType.JUMPING_JACKS, converters.toExerciseType("JUMPING_JACKS"))
    }
}
