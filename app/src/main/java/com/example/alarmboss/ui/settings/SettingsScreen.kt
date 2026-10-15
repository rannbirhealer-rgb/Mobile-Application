package com.example.alarmboss.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alarmboss.R
import com.example.alarmboss.util.SnoozeSettings

/**
 * Where the user supplies their own content for Medium-mode tasks: reading passages and
 * lyric lines (we never ship copyrighted text), plus app behavior like the snooze length.
 * One passage/line per row of the text box.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val snoozeMinutes by viewModel.snoozeMinutes.collectAsState()
    val passagesText by viewModel.passagesText.collectAsState()
    val lyricsText by viewModel.lyricsText.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.settings_snooze_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.settings_snooze_body),
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SnoozeSettings.OPTIONS.forEach { option ->
                    FilterChip(
                        selected = snoozeMinutes == option,
                        onClick = { viewModel.setSnoozeMinutes(option) },
                        label = { Text(stringResource(R.string.settings_snooze_option, option)) }
                    )
                }
            }

            HorizontalDivider()

            SoundSection(viewModel)

            HorizontalDivider()

            AccessibilitySection(viewModel)

            HorizontalDivider()

            FollowUpSection(viewModel)

            HorizontalDivider()

            Text(stringResource(R.string.settings_content_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.settings_content_body),
                style = MaterialTheme.typography.bodySmall
            )

            Text(stringResource(R.string.settings_passages_title), style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = passagesText,
                onValueChange = viewModel::onPassagesChange,
                modifier = Modifier.fillMaxWidth().height(140.dp)
            )
            Button(onClick = viewModel::savePassages) { Text(stringResource(R.string.settings_save_passages)) }

            Text(stringResource(R.string.settings_lyrics_title), style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = lyricsText,
                onValueChange = viewModel::onLyricsChange,
                modifier = Modifier.fillMaxWidth().height(140.dp)
            )
            Button(onClick = viewModel::saveLyrics) { Text(stringResource(R.string.settings_save_lyrics)) }
        }
    }
}
