package com.example.alarmboss.ui.alarms

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.alarmboss.R
import com.example.alarmboss.data.Alarm
import com.example.alarmboss.data.AlarmMode
import com.example.alarmboss.data.ExerciseType
import com.example.alarmboss.data.MediumTaskType
import com.example.alarmboss.data.MentalExerciseType
import com.example.alarmboss.data.StrictExerciseCategory

// User-facing names for the alarm model's enums, shared by the list and the editor.

@Composable
fun modeName(mode: AlarmMode): String = stringResource(
    when (mode) {
        AlarmMode.EASY -> R.string.alarm_mode_easy
        AlarmMode.MEDIUM -> R.string.alarm_mode_medium
        AlarmMode.STRICT -> R.string.alarm_mode_strict
    }
)

@Composable
fun exerciseName(type: ExerciseType): String = stringResource(
    if (type == ExerciseType.SQUATS) R.string.exercise_squats else R.string.exercise_jumping_jacks
)

@Composable
fun mentalName(type: MentalExerciseType): String = stringResource(
    if (type == MentalExerciseType.MEMORY_GRID) R.string.mental_memory_grid else R.string.mental_maze
)

@Composable
fun taskName(task: MediumTaskType): String = stringResource(
    when (task) {
        MediumTaskType.MATH -> R.string.medium_task_math
        MediumTaskType.READING -> R.string.medium_task_reading
        MediumTaskType.LYRICS -> R.string.medium_task_lyrics
        MediumTaskType.BARCODE -> R.string.medium_task_barcode
        MediumTaskType.PATTERN_LOCK -> R.string.medium_task_pattern_lock
        MediumTaskType.WORD_SCRAMBLE -> R.string.medium_task_word_scramble
        MediumTaskType.STEPS -> R.string.medium_task_steps
    }
)

/** One-line description of an alarm's mode for the list, e.g. "Strict · Physical (Squats)". */
@Composable
fun modeSummary(alarm: Alarm): String = when (alarm.mode) {
    AlarmMode.EASY -> stringResource(R.string.alarm_mode_easy)
    AlarmMode.MEDIUM -> stringResource(R.string.alarm_summary_medium)
    AlarmMode.STRICT ->
        if (alarm.strictCategory == StrictExerciseCategory.PHYSICAL) {
            stringResource(R.string.alarm_summary_strict_physical, exerciseName(alarm.exerciseType))
        } else {
            stringResource(R.string.alarm_summary_strict_mental, mentalName(alarm.mentalExerciseType))
        }
}
