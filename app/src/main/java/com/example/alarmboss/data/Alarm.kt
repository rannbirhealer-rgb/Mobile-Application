package com.example.alarmboss.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.alarmboss.engine.AlarmTimeCalculator
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

enum class AlarmMode { EASY, MEDIUM, STRICT }

enum class MediumTaskType { MATH, READING, LYRICS, BARCODE, PATTERN_LOCK, WORD_SCRAMBLE }

// Add these enums for Strict Mode categorization
enum class StrictExerciseCategory { PHYSICAL, MENTAL }

enum class MentalExerciseType { MEMORY_GRID, MAZE}

enum class ExerciseType { SQUATS, JUMPING_JACKS }

@Entity(tableName = "alarms")
data class Alarm @JvmOverloads constructor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val isEnabled: Boolean = true,
    val repeatDays: Set<Int> = emptySet(),
    val mode: AlarmMode = AlarmMode.EASY,
    val soundUri: String? = null,
    val vibrate: Boolean = true,
    val enabledMediumTasks: Set<MediumTaskType> = MediumTaskType.values().toSet(),
    val exerciseDurationSeconds: Int = 300,
    val exerciseType: ExerciseType = ExerciseType.SQUATS,

    // Add these fields referenced in AlarmRingingActivity
    val strictCategory: StrictExerciseCategory = StrictExerciseCategory.PHYSICAL,
    val mentalExerciseType: MentalExerciseType = MentalExerciseType.MEMORY_GRID,

    /** ISO date (yyyy-MM-dd) for an alarm that rings once on that day; null for ordinary alarms. */
    val specificDate: String? = null
)

/** [Alarm.specificDate] as a date, or null if absent or unreadable. */
fun Alarm.specificLocalDate(): LocalDate? =
    specificDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

/**
 * An enabled alarm is locked (cannot be edited, switched off or deleted) once its next ring is
 * within two hours: the anti-cheat commitment that stops you talking yourself out of it. "Next ring"
 * is the alarm's real next occurrence, not just its time of day, so a weekday alarm isn't locked on
 * a Saturday evening and an alarm set for next month isn't locked every night.
 */
fun isAlarmLocked(
    alarm: Alarm,
    now: LocalDateTime = LocalDateTime.now(),
    skipDates: Set<LocalDate> = emptySet()
): Boolean {
    if (!alarm.isEnabled) return false
    val next = AlarmTimeCalculator.nextTrigger(
        alarm.hour, alarm.minute, alarm.repeatDays, now, alarm.specificLocalDate(), skipDates
    ) ?: return false
    return !Duration.between(now, next).isNegative && Duration.between(now, next) <= LOCK_WINDOW
}

/** How close to ringing an alarm must be before it is locked against changes. */
val LOCK_WINDOW: Duration = Duration.ofHours(2)