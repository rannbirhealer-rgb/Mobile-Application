package com.example.alarmboss.ui.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.alarmboss.R
import com.example.alarmboss.data.ParentModeManager
import com.example.alarmboss.util.PinSecurity
import kotlinx.coroutines.delay

/**
 * The one place a parent PIN is asked for, so every guarded action behaves the same: digits only,
 * a clear message for a wrong PIN, a visible countdown while guessing is locked out, and a note on
 * what to do if the PIN has been forgotten.
 */
@Composable
fun ParentPinDialog(
    manager: ParentModeManager,
    message: String,
    confirmLabel: String,
    onVerified: () -> Unit,
    onDismiss: () -> Unit
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var lockedSeconds by remember { mutableIntStateOf(secondsLeft(manager)) }

    // Counts the lockout down on screen, and clears it when it ends.
    LaunchedEffect(lockedSeconds > 0) {
        while (manager.lockedOutForMillis() > 0L) {
            lockedSeconds = secondsLeft(manager)
            delay(500)
        }
        lockedSeconds = 0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.parent_pin_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(message, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { input ->
                        pin = input.filter { it in '0'..'9' }.take(PinSecurity.PIN_LENGTH)
                        wrong = false
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = wrong,
                    singleLine = true,
                    enabled = lockedSeconds == 0,
                    modifier = Modifier.fillMaxWidth()
                )
                when {
                    lockedSeconds > 0 -> Text(
                        pluralStringResource(R.plurals.parent_pin_locked, lockedSeconds, lockedSeconds),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    wrong -> Text(
                        stringResource(R.string.parent_pin_wrong),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    stringResource(R.string.parent_pin_forgot),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                enabled = lockedSeconds == 0 && pin.length == PinSecurity.PIN_LENGTH,
                onClick = {
                    if (manager.verifyPin(pin)) {
                        onVerified()
                    } else {
                        wrong = true
                        pin = ""
                        lockedSeconds = secondsLeft(manager)
                    }
                }
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

private fun secondsLeft(manager: ParentModeManager): Int =
    ((manager.lockedOutForMillis() + 999) / 1000).toInt()
