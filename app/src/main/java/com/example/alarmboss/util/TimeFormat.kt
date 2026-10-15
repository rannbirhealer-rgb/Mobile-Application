package com.example.alarmboss.util

import java.util.Locale

/** Formats a 24-hour (hour, minute) pair as a 12-hour clock string with AM/PM, e.g. "7:05 PM". */
fun formatTime12h(hour: Int, minute: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, period)
}

/** Formats an alarm time to match the user's clock preference: "07:05" on a 24-hour clock, "7:05 AM" otherwise. */
fun formatTime(hour: Int, minute: Int, is24Hour: Boolean): String =
    if (is24Hour) String.format(Locale.getDefault(), "%02d:%02d", hour, minute) else formatTime12h(hour, minute)