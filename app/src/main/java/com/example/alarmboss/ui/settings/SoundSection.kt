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
import com.example.alarmboss.util.SoundSettings

/** How loud and how gently an alarm rings. */
@Composable
fun SoundSection(viewModel: SettingsViewModel) {
    val loudness by viewModel.loudness.collectAsState()
    val gradual by viewModel.gradualVolume.collectAsState()
    val raise by viewModel.raiseSystemVolume.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.settings_sound_title), style = MaterialTheme.typography.titleMedium)

        Text(stringResource(R.string.settings_sound_loudness), style = MaterialTheme.typography.titleSmall)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SoundSettings.LOUDNESS_OPTIONS.forEach { option ->
                FilterChip(
                    selected = loudness == option,
                    onClick = { viewModel.setLoudness(option) },
                    label = { Text(stringResource(R.string.settings_sound_percent, option)) }
                )
            }
        }

        SettingSwitchRow(
            title = stringResource(R.string.settings_sound_gradual),
            body = stringResource(R.string.settings_sound_gradual_body),
            checked = gradual,
            onCheckedChange = viewModel::setGradualVolume
        )
        SettingSwitchRow(
            title = stringResource(R.string.settings_sound_raise),
            body = stringResource(R.string.settings_sound_raise_body),
            checked = raise,
            onCheckedChange = viewModel::setRaiseSystemVolume
        )
    }
}
