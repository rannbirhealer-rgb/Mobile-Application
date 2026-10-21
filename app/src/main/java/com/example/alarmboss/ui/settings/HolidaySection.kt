package com.example.alarmboss.ui.settings

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.alarmboss.R
import com.example.alarmboss.engine.HolidayRegion
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Opt-in holiday skipping: which holidays to watch, the user's own days off, and skips agreed so far. */
@Composable
fun HolidaySection(viewModel: SettingsViewModel) {
    val enabled by viewModel.holidayEnabled.collectAsState()
    val region by viewModel.holidayRegion.collectAsState()
    val customDays by viewModel.customDays.collectAsState()
    val skippedDays by viewModel.skippedDays.collectAsState()

    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.settings_holiday_title), style = MaterialTheme.typography.titleMedium)

        SettingSwitchRow(
            title = stringResource(R.string.settings_holiday_switch),
            body = stringResource(R.string.settings_holiday_body),
            checked = enabled,
            onCheckedChange = viewModel::setHolidayEnabled
        )
        if (!enabled) return@Column

        Text(stringResource(R.string.settings_holiday_region), style = MaterialTheme.typography.titleSmall)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HolidayRegion.values().forEach { option ->
                FilterChip(
                    selected = region == option,
                    onClick = { viewModel.setHolidayRegion(option) },
                    label = { Text(stringResource(regionLabel(option))) }
                )
            }
        }

        Text(stringResource(R.string.settings_holiday_custom_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.settings_holiday_custom_body), style = MaterialTheme.typography.bodySmall)
        if (customDays.isNotEmpty()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                customDays.forEach { day ->
                    val text = day.format(formatter)
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.removeCustomDay(day) },
                        label = { Text(text) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.settings_holiday_custom_remove, text)
                            )
                        }
                    )
                }
            }
        }
        OutlinedButton(onClick = {
            val today = LocalDate.now()
            DatePickerDialog(
                context,
                { _, year, month, day -> viewModel.addCustomDay(LocalDate.of(year, month + 1, day)) },
                today.year, today.monthValue - 1, today.dayOfMonth
            ).apply { datePicker.minDate = System.currentTimeMillis() - 1000 }.show()
        }) {
            Text(stringResource(R.string.settings_holiday_custom_add))
        }

        if (skippedDays.isNotEmpty()) {
            Text(stringResource(R.string.settings_holiday_skips_title), style = MaterialTheme.typography.titleSmall)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                skippedDays.forEach { day ->
                    val text = day.format(formatter)
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.cancelSkip(day) },
                        label = { Text(text) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.settings_holiday_skip_cancel, text)
                            )
                        }
                    )
                }
            }
        }
    }
}

private fun regionLabel(region: HolidayRegion): Int = when (region) {
    HolidayRegion.WORLDWIDE -> R.string.holiday_region_worldwide
    HolidayRegion.INDIA -> R.string.holiday_region_india
    HolidayRegion.US -> R.string.holiday_region_us
    HolidayRegion.UK -> R.string.holiday_region_uk
}
