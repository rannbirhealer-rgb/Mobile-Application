package com.example.alarmboss.ui.alarms

import android.app.DatePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.alarmboss.R
import com.example.alarmboss.engine.RepeatPresets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The shortcuts under the weekday chips: one-tap repeat patterns, and an alternative of pinning the
 * alarm to a single date. A pinned date and a repeat pattern are mutually exclusive, so choosing
 * either one clears the other.
 */
@Composable
fun RepeatExtras(
    days: Set<Int>,
    specificDate: LocalDate?,
    dateHasPassed: Boolean,
    onDaysChange: (Set<Int>) -> Unit,
    onDateChange: (LocalDate?) -> Unit
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    val matching = RepeatPresets.matching(days)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RepeatPresets.Preset.values().forEach { preset ->
                FilterChip(
                    selected = matching == preset,
                    onClick = { onDaysChange(preset.days) },
                    label = { Text(stringResource(presetLabel(preset))) }
                )
            }
        }

        if (specificDate != null) {
            FilterChip(
                selected = true,
                onClick = { onDateChange(null) },
                label = { Text(stringResource(R.string.edit_date_set, specificDate.format(dateFormatter))) },
                trailingIcon = {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.edit_date_clear))
                }
            )
            if (dateHasPassed) {
                Text(
                    stringResource(R.string.edit_date_passed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            OutlinedButton(onClick = {
                val today = LocalDate.now()
                DatePickerDialog(
                    context,
                    { _, year, month, day -> onDateChange(LocalDate.of(year, month + 1, day)) },
                    today.year, today.monthValue - 1, today.dayOfMonth
                ).apply { datePicker.minDate = System.currentTimeMillis() - 1000 }.show()
            }) {
                Text(stringResource(R.string.edit_date_pick))
            }
        }
    }
}

private fun presetLabel(preset: RepeatPresets.Preset): Int = when (preset) {
    RepeatPresets.Preset.WEEKDAYS -> R.string.edit_preset_weekdays
    RepeatPresets.Preset.WEEKENDS -> R.string.edit_preset_weekends
    RepeatPresets.Preset.EVERY_DAY -> R.string.edit_preset_every_day
}
