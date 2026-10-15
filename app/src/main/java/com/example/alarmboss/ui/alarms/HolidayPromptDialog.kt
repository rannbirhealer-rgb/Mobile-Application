package com.example.alarmboss.ui.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.alarmboss.R
import com.example.alarmboss.engine.HolidayKind
import com.example.alarmboss.engine.HolidayPlanner
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Asks whether repeating alarms should stay quiet on an upcoming holiday. Skipping is always the
 * user's explicit choice: the default of closing the dialog, or tapping Keep ringing, leaves every
 * alarm exactly as it was.
 */
@Composable
fun HolidayPromptDialog(
    prompt: HolidayPlanner.Prompt,
    onSkip: () -> Unit,
    onKeep: () -> Unit,
    onDismiss: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val name = stringResource(holidayNameRes(prompt.holiday.kind))
    val date = prompt.holiday.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.holiday_prompt_title, name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    pluralStringResource(
                        R.plurals.holiday_prompt_body, prompt.affectedAlarms, name, date, prompt.affectedAlarms
                    )
                )
                Text(stringResource(R.string.holiday_prompt_note), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onSkip) { Text(stringResource(R.string.holiday_prompt_skip)) } },
        dismissButton = { TextButton(onClick = onKeep) { Text(stringResource(R.string.holiday_prompt_keep)) } }
    )
}

fun holidayNameRes(kind: HolidayKind): Int = when (kind) {
    HolidayKind.NEW_YEAR -> R.string.holiday_new_year
    HolidayKind.CHRISTMAS -> R.string.holiday_christmas
    HolidayKind.REPUBLIC_DAY -> R.string.holiday_republic_day
    HolidayKind.INDEPENDENCE_DAY_IN -> R.string.holiday_independence_day
    HolidayKind.GANDHI_JAYANTI -> R.string.holiday_gandhi_jayanti
    HolidayKind.MEMORIAL_DAY -> R.string.holiday_memorial_day
    HolidayKind.INDEPENDENCE_DAY_US -> R.string.holiday_independence_day
    HolidayKind.LABOR_DAY -> R.string.holiday_labor_day
    HolidayKind.THANKSGIVING -> R.string.holiday_thanksgiving
    HolidayKind.GOOD_FRIDAY -> R.string.holiday_good_friday
    HolidayKind.EASTER_MONDAY -> R.string.holiday_easter_monday
    HolidayKind.BOXING_DAY -> R.string.holiday_boxing_day
    HolidayKind.CUSTOM -> R.string.holiday_custom
}
