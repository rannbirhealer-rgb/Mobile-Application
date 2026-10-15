package com.example.alarmboss.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.alarmboss.R
import com.example.alarmboss.util.FollowUpSettings

/** Extra rings that stop a finished alarm from being slept through. */
@Composable
fun FollowUpSection(viewModel: SettingsViewModel) {
    val wakeCheck by viewModel.wakeCheckEnabled.collectAsState()
    val wakeCheckMinutes by viewModel.wakeCheckMinutes.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.settings_followup_title), style = MaterialTheme.typography.titleMedium)

        SettingSwitchRow(
            title = stringResource(R.string.settings_wake_check),
            body = stringResource(R.string.settings_wake_check_body),
            checked = wakeCheck,
            onCheckedChange = viewModel::setWakeCheckEnabled
        )
        if (wakeCheck) {
            Text(stringResource(R.string.settings_wake_check_delay), style = MaterialTheme.typography.titleSmall)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FollowUpSettings.WAKE_CHECK_OPTIONS.forEach { option ->
                    FilterChip(
                        selected = wakeCheckMinutes == option,
                        onClick = { viewModel.setWakeCheckMinutes(option) },
                        label = { Text(stringResource(R.string.settings_minutes_option, option)) }
                    )
                }
            }
        }
    }
}
