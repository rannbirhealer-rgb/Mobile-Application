package com.example.alarmboss.ui.alarms

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alarmboss.R
import com.example.alarmboss.data.Alarm
import com.example.alarmboss.data.isAlarmLocked
import com.example.alarmboss.ui.theme.StreakFlameDark
import com.example.alarmboss.ui.theme.StreakFlameLight
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    viewModel: AlarmViewModel = viewModel()
) {
    val alarms by viewModel.alarms.collectAsState()
    val currentStreak by viewModel.streak.collectAsState(initial = 0)

    // Reload the streak whenever the screen is composed or resumed
    LaunchedEffect(Unit) {
        viewModel.loadStreak()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_alarms)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAlarm) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.alarms_add_description))
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            ExactAlarmPermissionBanner()
            BatteryOptimizationBanner()

            // Wake-Up Streak UI
            if (currentStreak > 0) {
                val streakColor = if (isSystemInDarkTheme()) StreakFlameDark else StreakFlameLight
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = pluralStringResource(R.plurals.alarms_streak_banner, currentStreak, currentStreak),
                        fontWeight = FontWeight.Bold,
                        color = streakColor
                    )
                }
            }

            if (alarms.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.alarms_empty))
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmRow(
                            alarm = alarm,
                            onToggle = { enabled -> viewModel.setEnabled(alarm, enabled) },
                            onClick = { onEditAlarm(alarm.id) },
                            onDelete = { viewModel.delete(alarm) },
                            modifier = Modifier.animateItem()
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/**
 * On Android 12+, exact-alarm scheduling can be revoked by the user (or the OS) after the
 * app is installed. Without it, alarms silently fall back to inexact delivery, which can be
 * off by many minutes -- unacceptable for an alarm clock -- so surface it instead of letting
 * alarms quietly run late. Rechecked on resume since the grant happens in system Settings.
 */
@Composable
private fun ExactAlarmPermissionBanner() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val context = LocalContext.current
    val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    var canScheduleExact by remember { mutableStateOf(alarmManager.canScheduleExactAlarms()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canScheduleExact = alarmManager.canScheduleExactAlarms()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!canScheduleExact) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.exact_alarm_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.exact_alarm_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }) { Text(stringResource(R.string.exact_alarm_enable)) }
            }
        }
    }
}

/**
 * One alarm in the list. Swiping it to the right asks to delete it, same as the delete button,
 * which stays for anyone who can't or doesn't swipe (screen readers, switch access). Locked
 * alarms (inside the two-hour lockdown window) can't be swiped away, matching the button.
 *
 * The swipe runs left to right on purpose: this is the first page of a pager, whose only possible
 * swipe is leftward (to the next tab), so a rightward drag is the one gesture the pager never
 * needs. See [SwipeToDeleteRow] for why leftward drags are deliberately left unclaimed.
 */
@Composable
private fun AlarmRow(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLocked = isAlarmLocked(alarm.hour, alarm.minute, alarm.isEnabled)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.alarms_delete_title)) },
            text = {
                Text(stringResource(R.string.alarms_delete_message, alarmTimeText(alarm.hour, alarm.minute)))
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    // Never delete on the swipe itself: it only asks, and the row springs back meanwhile.
    SwipeToDeleteRow(
        enabled = !isLocked,
        onRequestDelete = { showDeleteConfirm = true },
        modifier = modifier
    ) {
        AlarmRowContent(
            alarm = alarm,
            isLocked = isLocked,
            onToggle = onToggle,
            onClick = onClick,
            onRequestDelete = { showDeleteConfirm = true }
        )
    }
}

@Composable
private fun AlarmRowContent(
    alarm: Alarm,
    isLocked: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onRequestDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            // Opaque, so the red delete background doesn't show through the row at rest.
            .background(MaterialTheme.colorScheme.background)
            .then(
                if (isLocked) Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                else Modifier
            )
            .clickable(enabled = !isLocked, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier.weight(1f)
        ) {
            Text(
                alarmTimeText(alarm.hour, alarm.minute),
                style = MaterialTheme.typography.headlineMedium,
                color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified
            )
            if (alarm.label.isNotBlank()) Text(alarm.label, style = MaterialTheme.typography.bodyMedium)
            Text(modeSummary(alarm), style = MaterialTheme.typography.bodySmall)
            if (alarm.repeatDays.isNotEmpty()) {
                Text(
                    daysLabel(alarm.repeatDays, LocalConfiguration.current.locales[0]),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (isLocked) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = stringResource(R.string.alarms_locked_description),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(12.dp)
            )
        } else {
            IconButton(onClick = onRequestDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.alarms_delete_description))
            }
        }

        Switch(
            checked = alarm.isEnabled,
            onCheckedChange = onToggle,
            enabled = !isLocked
        )
    }
}

private fun daysLabel(days: Set<Int>, locale: Locale): String =
    days.sorted().joinToString(", ") {
        DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, locale)
    }
