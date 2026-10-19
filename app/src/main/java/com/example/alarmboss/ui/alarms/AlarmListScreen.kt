package com.example.alarmboss.ui.alarms

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alarmboss.R
import com.example.alarmboss.data.Alarm
import com.example.alarmboss.data.AlarmMode
import com.example.alarmboss.data.isAlarmLocked
import com.example.alarmboss.ui.common.HoldToConfirmButton
import com.example.alarmboss.data.specificLocalDate
import com.example.alarmboss.ui.theme.FlameGradientEnd
import com.example.alarmboss.ui.theme.FlameGradientStart
import com.example.alarmboss.util.formatTime
import android.widget.Toast
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.alarmboss.data.ParentModeManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onOpenParentMode: () -> Unit = {},
    viewModel: AlarmViewModel = viewModel()
) {
    val context = LocalContext.current
    val parentManager = remember { ParentModeManager(context) }
    val alarms by viewModel.alarms.collectAsState()
    val currentStreak by viewModel.streak.collectAsState(initial = 0)
    val skipDates by viewModel.skipDates.collectAsState()
    val holidayPrompt by viewModel.holidayPrompt.collectAsState()

    var showTemplatesSheet by remember { mutableStateOf(false) }

    // Parent PIN verification dialog state
    var showPinDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun runProtectedAction(action: () -> Unit) {
        if (parentManager.isParentModeActive() && parentManager.isStrictLockEnabled()) {
            pendingAction = action
            enteredPin = ""
            pinError = false
            showPinDialog = true
        } else {
            action()
        }
    }
    // Reload the streak whenever the screen is composed or resumed
    LaunchedEffect(Unit) {
        viewModel.loadStreak()
        viewModel.refreshHolidays()
    }

    // Closing the question without answering hides it for now; it returns on the next launch.
    var dismissedHoliday by remember { mutableStateOf<LocalDate?>(null) }
    holidayPrompt?.takeIf { it.holiday.date != dismissedHoliday }?.let { prompt ->
        HolidayPromptDialog(
            prompt = prompt,
            onSkip = { viewModel.answerHoliday(prompt, skip = true) },
            onKeep = { viewModel.answerHoliday(prompt, skip = false) },
            onDismiss = { dismissedHoliday = prompt.holiday.date }
        )
    }
    if (showTemplatesSheet) {
        AlarmTemplatesBottomSheet(
            onDismiss = { showTemplatesSheet = false },
            onSelectTemplate = { templateAlarm ->
                viewModel.save(templateAlarm)
                Toast.makeText(context, "Applied template: ${templateAlarm.label}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                pendingAction = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Parent PIN Required", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Parent Mode is active. Enter your 4-digit PIN to modify or delete this alarm:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            enteredPin = it
                            pinError = false
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        isError = pinError,
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError) {
                        Text("Incorrect PIN. Please try again.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (parentManager.verifyPin(enteredPin)) {
                        showPinDialog = false
                        pendingAction?.invoke()
                        pendingAction = null
                    } else {
                        pinError = true
                    }
                }) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialog = false
                    pendingAction = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Conquer your mornings",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Quick Templates Button
                    FilledTonalButton(
                        onClick = { showTemplatesSheet = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Templates", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Parent Mode Button
                    IconButton(onClick = onOpenParentMode) {
                        Icon(
                            Icons.Default.FamilyRestroom,
                            contentDescription = "Parent Mode",
                            tint = if (parentManager.isParentModeActive()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { runProtectedAction(onAddAlarm) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.alarms_add_description))
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ExactAlarmPermissionBanner()
            BatteryOptimizationBanner()

            // Parent Mode Status Banner if active
            if (parentManager.isParentModeActive()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Parent Mode Active: ${parentManager.getChildName()} (Age ${parentManager.getChildAge()})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        TextButton(
                            onClick = onOpenParentMode,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Manage", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Hero Wake-Up Streak Card (Morning Flame with Strict 5 AM - 10 AM rule)
            if (currentStreak > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(FlameGradientStart, FlameGradientEnd)
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🔥", fontSize = 26.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = pluralStringResource(R.plurals.alarms_streak_banner, currentStreak, currentStreak),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Strict Morning Window: 5:00 AM – 10:00 AM",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Only wake-ups completed within this morning window count toward your streak!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.90f)
                                )
                            }
                        }
                    }
                }
            }
            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(90.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = "No Alarms Scheduled",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.alarms_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        FilledTonalButton(
                            onClick = onAddAlarm,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create New Alarm")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmRow(
                            alarm = alarm,
                            skipDates = skipDates,
                            onToggle = { enabled -> runProtectedAction { viewModel.setEnabled(alarm, enabled) } },
                            onClick = { runProtectedAction { onEditAlarm(alarm.id) } },
                            onDelete = { runProtectedAction { viewModel.delete(alarm) } },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.exact_alarm_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
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
                }) { Text(stringResource(R.string.exact_alarm_enable), fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: Alarm,
    skipDates: Set<LocalDate>,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLocked = isAlarmLocked(alarm, skipDates = skipDates)
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
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    var showEmergencyUnlock by remember { mutableStateOf(false) }
    if (showEmergencyUnlock) {
        // The two-hour lock stops you disabling an alarm to dodge it, but a lock with no way out is
        // unsafe (illness, a mistaken time, a real emergency). So there is one, made deliberately
        // slow: a long press to find it, then a held button, never a single tap.
        AlertDialog(
            onDismissRequest = { showEmergencyUnlock = false },
            title = { Text(stringResource(R.string.emergency_unlock_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.emergency_unlock_body))
                    HoldToConfirmButton(
                        label = stringResource(R.string.emergency_unlock_hold, EMERGENCY_UNLOCK_SECONDS),
                        holdMillis = EMERGENCY_UNLOCK_SECONDS * 1000L,
                        onConfirmed = {
                            showEmergencyUnlock = false
                            onToggle(false)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showEmergencyUnlock = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    // Never delete on the swipe itself: it only asks, and the row springs back meanwhile.
    SwipeToDeleteRow(
        enabled = !isLocked,
        onRequestDelete = { showDeleteConfirm = true },
        modifier = modifier
    ) {
        AlarmCardContent(
            alarm = alarm,
            isLocked = isLocked,
            onToggle = onToggle,
            onClick = onClick,
            onRequestDelete = { showDeleteConfirm = true },
            onRequestEmergencyUnlock = { showEmergencyUnlock = true }
        )
    }
}

private const val EMERGENCY_UNLOCK_SECONDS = 5

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlarmCardContent(
    alarm: Alarm,
    isLocked: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onRequestDelete: () -> Unit,
    onRequestEmergencyUnlock: () -> Unit
) {
    val cardBackground = if (isLocked) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (alarm.isEnabled) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (alarm.isEnabled) 2.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (!isLocked) onClick() },
                // A locked alarm can't be edited, but it must never be a trap: long-press finds the way out.
                onLongClick = if (isLocked) onRequestEmergencyUnlock else null
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Time, Label & Main Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatTime(alarm.hour, alarm.minute, rememberIs24Hour()),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    if (alarm.label.isNotBlank()) {
                        Text(
                            text = alarm.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLocked) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = stringResource(R.string.alarms_locked_description),
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Locked",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = onRequestDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.alarms_delete_description),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = onToggle,
                        enabled = !isLocked
                    )
                }
            }

            // Days of the week row
            DayChipsRow(repeatDays = alarm.repeatDays, isEnabled = alarm.isEnabled)

            alarm.specificLocalDate()?.let { date ->
                val locale = LocalConfiguration.current.locales[0]
                Text(
                    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isLocked) {
                Text(
                    stringResource(R.string.alarms_locked_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Badges row: Mode + Superpower badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode Badge
                val (modeColor, modeOnColor, modeIcon) = when (alarm.mode) {
                    AlarmMode.STRICT -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, "⚔️")
                    AlarmMode.MEDIUM -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, "⚡")
                    AlarmMode.EASY -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, "🌱")
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (alarm.isEnabled) modeColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "$modeIcon ${modeSummary(alarm)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (alarm.isEnabled) modeOnColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Superpower indicators
                if (alarm.gradualVolume) FeaturePill(label = "📶 Crescendo", isEnabled = alarm.isEnabled)
                if (alarm.sunriseMode) FeaturePill(label = "🌅 Sunrise", isEnabled = alarm.isEnabled)
                if (alarm.wakeCheck) FeaturePill(label = "🛡️ Check", isEnabled = alarm.isEnabled)
                if (alarm.morningBriefing) FeaturePill(label = "🗣️ Voice", isEnabled = alarm.isEnabled)
                if (alarm.dynamicBrightness) FeaturePill(label = "☀️ Soft-Wake", isEnabled = alarm.isEnabled)
                if (alarm.flashLightBlink) FeaturePill(label = "🔦 Strobe", isEnabled = alarm.isEnabled)
            }
        }
    }
}

@Composable
private fun FeaturePill(label: String, isEnabled: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isEnabled) 0.6f else 0.3f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun DayChipsRow(repeatDays: Set<Int>, isEnabled: Boolean) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dayLabels.forEachIndexed { index, label ->
            val dayNumber = index + 1
            val isActive = repeatDays.contains(dayNumber)

            val bg = when {
                isActive && isEnabled -> MaterialTheme.colorScheme.primary
                isActive && !isEnabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }

            val textColor = when {
                isActive -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(bg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    }
}
