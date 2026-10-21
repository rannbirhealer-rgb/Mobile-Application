package com.example.alarmboss.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromIntSet(value: Set<Int>): String = value.joinToString(",")

    /**
     * The converters below read what earlier or newer builds, or a damaged file, may have written. A
     * single value this build does not know must cost only that value, never the whole alarm list: an
     * exception here surfaces while the list loads and would leave the app unable to show, edit or
     * schedule any alarm at all.
     */
    @TypeConverter
    fun toIntSet(value: String): Set<Int> =
        value.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

    @TypeConverter
    fun fromMode(value: AlarmMode): String = value.name

    @TypeConverter
    fun toMode(value: String): AlarmMode =
        runCatching { AlarmMode.valueOf(value) }.getOrDefault(AlarmMode.EASY)

    @TypeConverter
    fun fromExerciseType(value: ExerciseType): String = value.name

    @TypeConverter
    fun toExerciseType(value: String): ExerciseType =
        runCatching { ExerciseType.valueOf(value) }.getOrDefault(ExerciseType.SQUATS)

    @TypeConverter
    fun fromTaskSet(value: Set<MediumTaskType>): String = value.joinToString(",") { it.name }

    @TypeConverter
    fun toTaskSet(value: String): Set<MediumTaskType> =
        value.split(",").mapNotNull { name -> runCatching { MediumTaskType.valueOf(name.trim()) }.getOrNull() }.toSet()
}
