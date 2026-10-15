package com.example.alarmboss.engine

/** One-tap repeat patterns, as ISO weekday sets (1 = Monday .. 7 = Sunday), so nobody ticks five boxes every time. */
object RepeatPresets {
    val WEEKDAYS: Set<Int> = setOf(1, 2, 3, 4, 5)
    val WEEKENDS: Set<Int> = setOf(6, 7)
    val EVERY_DAY: Set<Int> = (1..7).toSet()

    enum class Preset(val days: Set<Int>) {
        WEEKDAYS(RepeatPresets.WEEKDAYS),
        WEEKENDS(RepeatPresets.WEEKENDS),
        EVERY_DAY(RepeatPresets.EVERY_DAY)
    }

    /** The preset exactly matching [days], so its chip can show as selected, or null for a custom pattern. */
    fun matching(days: Set<Int>): Preset? = Preset.values().firstOrNull { it.days == days }
}
