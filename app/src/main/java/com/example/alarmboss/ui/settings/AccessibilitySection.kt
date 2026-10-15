package com.example.alarmboss.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.alarmboss.R

/** Ways to wake without relying on sound. */
@Composable
fun AccessibilitySection(viewModel: SettingsViewModel) {
    val silent by viewModel.silentMode.collectAsState()
    val flash by viewModel.flash.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.settings_access_title), style = MaterialTheme.typography.titleMedium)
        SettingSwitchRow(
            title = stringResource(R.string.settings_access_silent),
            body = stringResource(R.string.settings_access_silent_body),
            checked = silent,
            onCheckedChange = viewModel::setSilentMode
        )
        SettingSwitchRow(
            title = stringResource(R.string.settings_access_flash),
            body = stringResource(R.string.settings_access_flash_body),
            checked = flash,
            onCheckedChange = viewModel::setFlash
        )
    }
}
