package com.example.alarmboss.ui.alarms

import android.Manifest
import com.example.alarmboss.data.AlarmMode
import com.example.alarmboss.data.MediumTaskType
import com.example.alarmboss.data.StrictExerciseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmPermissionsTest {

    private val none = emptySet<MediumTaskType>()

    @Test
    fun `easy alarms need nothing`() {
        assertTrue(requiredPermissions(AlarmMode.EASY, StrictExerciseCategory.PHYSICAL, MediumTaskType.values().toSet()).isEmpty())
    }

    @Test
    fun `physical strict needs the camera`() {
        assertEquals(
            setOf(Manifest.permission.CAMERA),
            requiredPermissions(AlarmMode.STRICT, StrictExerciseCategory.PHYSICAL, none)
        )
    }

    @Test
    fun `mental strict needs nothing`() {
        assertTrue(requiredPermissions(AlarmMode.STRICT, StrictExerciseCategory.MENTAL, none).isEmpty())
    }

    @Test
    fun `medium barcode needs the camera`() {
        assertEquals(
            setOf(Manifest.permission.CAMERA),
            requiredPermissions(AlarmMode.MEDIUM, StrictExerciseCategory.PHYSICAL, setOf(MediumTaskType.BARCODE))
        )
    }

    @Test
    fun `medium steps asks for activity recognition`() {
        assertEquals(
            setOf(Manifest.permission.ACTIVITY_RECOGNITION),
            requiredPermissions(AlarmMode.MEDIUM, StrictExerciseCategory.PHYSICAL, setOf(MediumTaskType.STEPS))
        )
    }

    @Test
    fun `several tasks ask for each of their permissions`() {
        val tasks = setOf(MediumTaskType.BARCODE, MediumTaskType.STEPS, MediumTaskType.READING)
        assertEquals(
            setOf(Manifest.permission.CAMERA, Manifest.permission.ACTIVITY_RECOGNITION, Manifest.permission.RECORD_AUDIO),
            requiredPermissions(AlarmMode.MEDIUM, StrictExerciseCategory.PHYSICAL, tasks)
        )
    }

    @Test
    fun `medium reading or lyrics needs the microphone`() {
        val expected = setOf(Manifest.permission.RECORD_AUDIO)
        assertEquals(expected, requiredPermissions(AlarmMode.MEDIUM, StrictExerciseCategory.PHYSICAL, setOf(MediumTaskType.READING)))
        assertEquals(expected, requiredPermissions(AlarmMode.MEDIUM, StrictExerciseCategory.PHYSICAL, setOf(MediumTaskType.LYRICS)))
    }

    @Test
    fun `medium with only permission-free tasks needs nothing`() {
        val tasks = setOf(MediumTaskType.MATH, MediumTaskType.PATTERN_LOCK, MediumTaskType.WORD_SCRAMBLE)
        assertTrue(requiredPermissions(AlarmMode.MEDIUM, StrictExerciseCategory.PHYSICAL, tasks).isEmpty())
    }

    @Test
    fun `medium with every task needs all three`() {
        assertEquals(
            setOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.ACTIVITY_RECOGNITION),
            requiredPermissions(AlarmMode.MEDIUM, StrictExerciseCategory.PHYSICAL, MediumTaskType.values().toSet())
        )
    }
}
