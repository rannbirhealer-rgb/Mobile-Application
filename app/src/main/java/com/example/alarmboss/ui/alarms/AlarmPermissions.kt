package com.example.alarmboss.ui.alarms

import android.Manifest
import com.example.alarmboss.data.AlarmMode
import com.example.alarmboss.data.MediumTaskType
import com.example.alarmboss.data.StrictExerciseCategory

/**
 * Runtime permissions an alarm's challenges can need. Asking when the alarm is saved avoids a
 * permission dialog at ring time, when the user is half asleep and the screen can't be backed out of.
 *
 * Camera has no fallback (barcode scanning and the physical Strict workout), so it matters most.
 * The microphone only powers voice answers, and the reading and lyrics tasks accept typing instead.
 * Step counting needs activity recognition for the hardware step sensor, but falls back to the motion
 * sensor without it, so a refusal never traps anyone.
 */
fun requiredPermissions(
    mode: AlarmMode,
    strictCategory: StrictExerciseCategory,
    enabledTasks: Set<MediumTaskType>
): Set<String> = when (mode) {
    AlarmMode.EASY -> emptySet()
    AlarmMode.MEDIUM -> buildSet {
        if (MediumTaskType.BARCODE in enabledTasks) add(Manifest.permission.CAMERA)
        if (MediumTaskType.STEPS in enabledTasks) add(Manifest.permission.ACTIVITY_RECOGNITION)
        if (MediumTaskType.READING in enabledTasks || MediumTaskType.LYRICS in enabledTasks) {
            add(Manifest.permission.RECORD_AUDIO)
        }
    }
    AlarmMode.STRICT ->
        if (strictCategory == StrictExerciseCategory.PHYSICAL) setOf(Manifest.permission.CAMERA) else emptySet()
}
