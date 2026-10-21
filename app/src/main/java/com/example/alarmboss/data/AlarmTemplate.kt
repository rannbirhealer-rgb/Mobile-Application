package com.example.alarmboss.data

import java.util.Calendar

data class AlarmTemplate(
    val id: String,
    val name: String,
    val emoji: String,
    val tag: String,
    val description: String,
    val hour: Int,
    val minute: Int,
    val mode: AlarmMode,
    val repeatDays: Set<Int> = emptySet(),
    val enabledMediumTasks: Set<MediumTaskType> = MediumTaskType.values().toSet(),
    val strictCategory: StrictExerciseCategory = StrictExerciseCategory.PHYSICAL,
    val exerciseType: ExerciseType = ExerciseType.SQUATS,
    val exerciseDurationSeconds: Int = 180,
    val mentalExerciseType: MentalExerciseType = MentalExerciseType.MEMORY_GRID,
    val dynamicBrightness: Boolean = false,
    val flashLightBlink: Boolean = false,
    val gradualVolume: Boolean = false,
    val sunriseMode: Boolean = false,
    val wakeCheck: Boolean = true,
    val morningBriefing: Boolean = false
) {
    fun toAlarm(): Alarm {
        return Alarm(
            hour = hour,
            minute = minute,
            label = name,
            isEnabled = true,
            repeatDays = repeatDays,
            mode = mode,
            enabledMediumTasks = enabledMediumTasks,
            strictCategory = strictCategory,
            exerciseType = exerciseType,
            exerciseDurationSeconds = exerciseDurationSeconds,
            mentalExerciseType = mentalExerciseType,
            dynamicBrightness = dynamicBrightness,
            flashLightBlink = flashLightBlink,
            gradualVolume = gradualVolume,
            sunriseMode = sunriseMode,
            wakeCheck = if (mode != AlarmMode.EASY) true else wakeCheck,
            morningBriefing = morningBriefing
        )
    }

    companion object {
        fun getAllTemplates(): List<AlarmTemplate> {
            val now = Calendar.getInstance()
            val napMinute = (now.get(Calendar.MINUTE) + 25) % 60
            val napHour = (now.get(Calendar.HOUR_OF_DAY) + if (now.get(Calendar.MINUTE) + 25 >= 60) 1 else 0) % 24

            return listOf(
                AlarmTemplate(
                    id = "heavy_sleeper",
                    name = "Heavy Sleeper Boss Fight",
                    emoji = "💥",
                    tag = "Heavy Sleeper",
                    description = "Designed for deep sleepers who snooze through normal alarms. Enforces physical squats, sunrise flash, gradual volume ramp, and locked awake safeguard.",
                    hour = 6,
                    minute = 30,
                    mode = AlarmMode.STRICT,
                    repeatDays = setOf(1, 2, 3, 4, 5),
                    strictCategory = StrictExerciseCategory.PHYSICAL,
                    exerciseType = ExerciseType.SQUATS,
                    exerciseDurationSeconds = 120,
                    flashLightBlink = true,
                    gradualVolume = true,
                    sunriseMode = true,
                    wakeCheck = true,
                    morningBriefing = true
                ),
                AlarmTemplate(
                    id = "early_bird",
                    name = "Early Bird 5:30 AM",
                    emoji = "🌅",
                    tag = "Streak Booster",
                    description = "Wake up at 5:30 AM strictly inside the morning streak window (5 AM - 10 AM). Solves math & reading challenges, followed by spoken morning briefing.",
                    hour = 5,
                    minute = 30,
                    mode = AlarmMode.MEDIUM,
                    repeatDays = setOf(1, 2, 3, 4, 5, 6, 7),
                    enabledMediumTasks = setOf(MediumTaskType.MATH, MediumTaskType.READING, MediumTaskType.STEPS),
                    wakeCheck = true,
                    morningBriefing = true,
                    sunriseMode = true
                ),
                AlarmTemplate(
                    id = "power_nap",
                    name = "NASA 25-Min Power Nap",
                    emoji = "⚡",
                    tag = "Power Nap",
                    description = "Quick 25-minute productivity nap timed from right now. Restores cognitive alertness without triggering deep sleep grogginess.",
                    hour = napHour,
                    minute = napMinute,
                    mode = AlarmMode.EASY,
                    repeatDays = emptySet(),
                    gradualVolume = true,
                    dynamicBrightness = true,
                    wakeCheck = false,
                    morningBriefing = false
                ),
                AlarmTemplate(
                    id = "student_routine",
                    name = "Student Focus Routine",
                    emoji = "📚",
                    tag = "Cognitive Challenge",
                    description = "Sharpens your memory and concentration before class with word scramble and memory grid challenges. Locked safeguard ensures you don't sleep in.",
                    hour = 6,
                    minute = 15,
                    mode = AlarmMode.MEDIUM,
                    repeatDays = setOf(1, 2, 3, 4, 5),
                    enabledMediumTasks = setOf(MediumTaskType.WORD_SCRAMBLE, MediumTaskType.PATTERN_LOCK, MediumTaskType.MATH),
                    wakeCheck = true,
                    morningBriefing = true
                ),
                AlarmTemplate(
                    id = "gentle_sunrise",
                    name = "Gentle Dawn Awakening",
                    emoji = "🌤️",
                    tag = "Gentle Awakening",
                    description = "Slow, gradual sunrise glow and whisper-to-crescendo volume ramp that mimics the natural sunrise for zero morning stress.",
                    hour = 7,
                    minute = 0,
                    mode = AlarmMode.EASY,
                    repeatDays = setOf(1, 2, 3, 4, 5),
                    dynamicBrightness = true,
                    gradualVolume = true,
                    sunriseMode = true,
                    wakeCheck = false,
                    morningBriefing = true
                ),
                AlarmTemplate(
                    id = "parent_kids",
                    name = "Kids School Routine (Ages 9-12)",
                    emoji = "🎒",
                    tag = "Parent Mode",
                    description = "Child-friendly wake-up routine with reading comprehension or math steps. Designed for parents to configure and protect with security PIN.",
                    hour = 6,
                    minute = 45,
                    mode = AlarmMode.MEDIUM,
                    repeatDays = setOf(1, 2, 3, 4, 5),
                    enabledMediumTasks = setOf(MediumTaskType.READING, MediumTaskType.MATH, MediumTaskType.STEPS),
                    wakeCheck = true,
                    morningBriefing = true
                )
            )
        }
    }
}
