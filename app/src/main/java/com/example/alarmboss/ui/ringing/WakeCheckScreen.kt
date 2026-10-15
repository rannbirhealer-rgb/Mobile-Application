package com.example.alarmboss.ui.ringing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.alarmboss.R
import com.example.alarmboss.ui.ringing.tasks.MathTaskScreen

/**
 * The post-dismissal check: a single easy sum, just enough to show the person is still upright and
 * thinking rather than back under the duvet. Deliberately light, because they've already done the
 * real challenge once.
 */
@Composable
fun WakeCheckScreen(onSolved: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.wake_check_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.wake_check_body), style = MaterialTheme.typography.bodyMedium)
        Box(Modifier.weight(1f)) {
            MathTaskScreen(difficulty = 1, problemCount = 1, onSolved = onSolved)
        }
    }
}
