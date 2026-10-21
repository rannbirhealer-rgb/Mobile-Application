package com.example.alarmboss.ui.alarms

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.alarmboss.util.formatTime

/** Whether the system clock is set to 24-hour time. Read from the system, not hard-coded, so the app follows it. */
@Composable
fun rememberIs24Hour(): Boolean = DateFormat.is24HourFormat(LocalContext.current)

/** An alarm's time as text, in the user's 12 or 24-hour preference. */
@Composable
fun alarmTimeText(hour: Int, minute: Int): String = formatTime(hour, minute, rememberIs24Hour())
