package com.example.alarmboss.ui.parent

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarmboss.AlarmBossApp
import com.example.alarmboss.data.Alarm
import com.example.alarmboss.data.AlarmMode
import com.example.alarmboss.data.MediumTaskType
import com.example.alarmboss.data.ParentModeManager
import com.example.alarmboss.R
import com.example.alarmboss.alarm.AlarmScheduler
import com.example.alarmboss.ui.alarms.rememberIs24Hour
import com.example.alarmboss.util.PinSecurity
import com.example.alarmboss.util.formatTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentModeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as AlarmBossApp
    val parentManager = remember { ParentModeManager(context) }
    val is24Hour = rememberIs24Hour()

    var isModeActive by remember { mutableStateOf(parentManager.isParentModeActive()) }

    // Form fields for activation
    var parentName by remember { mutableStateOf(parentManager.getParentName()) }
    var parentEmail by remember { mutableStateOf(parentManager.getParentEmail()) }
    var pin by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var childName by remember { mutableStateOf(parentManager.getChildName()) }
    var childAge by remember { mutableStateOf(parentManager.getChildAge()) }
    var schoolWakeHour by remember { mutableStateOf(parentManager.getSchoolWakeHour()) }
    var schoolWakeMinute by remember { mutableStateOf(parentManager.getSchoolWakeMinute()) }
    var bedtimeHour by remember { mutableStateOf(parentManager.getBedtimeHour()) }
    var bedtimeMinute by remember { mutableStateOf(parentManager.getBedtimeMinute()) }
    var strictLock by remember { mutableStateOf(parentManager.isStrictLockEnabled()) }

    // Dialogs for turning the lock off and for leaving parent mode; both need the PIN
    var showDisableDialog by remember { mutableStateOf(false) }
    var showLockOffDialog by remember { mutableStateOf(false) }

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
                                Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.parent_title), fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isModeActive) {
                // Active Dashboard
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.parent_active_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = stringResource(R.string.parent_supervised, parentManager.getParentName(), parentManager.getChildName(), parentManager.getChildAge()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(stringResource(R.string.parent_school_wake), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatTime(parentManager.getSchoolWakeHour(), parentManager.getSchoolWakeMinute(), is24Hour), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column {
                                Text(stringResource(R.string.parent_bedtime), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatTime(parentManager.getBedtimeHour(), parentManager.getBedtimeMinute(), is24Hour), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(stringResource(R.string.parent_lock_title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(stringResource(R.string.parent_lock_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = strictLock,
                                onCheckedChange = { wanted ->
                                    // Turning the lock off is what it exists to stop, so that needs the PIN.
                                    if (wanted) {
                                        parentManager.enableStrictLock()
                                        strictLock = true
                                    } else {
                                        showLockOffDialog = true
                                    }
                                }
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = { showDisableDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.parent_disable), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Setup & Verification Wizard
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.parent_details_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = parentName,
                            onValueChange = { parentName = it },
                            label = { Text(stringResource(R.string.parent_name_label)) },
                            placeholder = { Text(stringResource(R.string.parent_name_hint)) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = parentEmail,
                            onValueChange = { parentEmail = it },
                            label = { Text(stringResource(R.string.parent_email_label)) },
                            placeholder = { Text(stringResource(R.string.parent_email_hint)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = pin,
                                onValueChange = { input -> pin = input.filter { it in '0'..'9' }.take(PinSecurity.PIN_LENGTH) },
                                label = { Text(stringResource(R.string.parent_pin_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = pinConfirm,
                                onValueChange = { input -> pinConfirm = input.filter { it in '0'..'9' }.take(PinSecurity.PIN_LENGTH) },
                                label = { Text(stringResource(R.string.parent_pin_confirm_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Child/Ward Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ChildCare, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.parent_child_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = childName,
                            onValueChange = { childName = it },
                            label = { Text(stringResource(R.string.parent_child_name_label)) },
                            placeholder = { Text(stringResource(R.string.parent_child_name_hint)) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(stringResource(R.string.parent_age_prompt), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(9, 10, 11, 12).forEach { age ->
                                FilterChip(
                                    selected = childAge == age,
                                    onClick = { childAge = age },
                                    label = { Text(stringResource(R.string.parent_age_option, age), fontWeight = FontWeight.Bold) },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // School wake-up time picker
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    TimePickerDialog(context, { _, h, m -> schoolWakeHour = h; schoolWakeMinute = m }, schoolWakeHour, schoolWakeMinute, is24Hour).show()
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(stringResource(R.string.parent_school_morning), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(stringResource(R.string.parent_school_morning_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(6.dp))
                                    Text(formatTime(schoolWakeHour, schoolWakeMinute, is24Hour), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }

                val errorNames = stringResource(R.string.parent_error_names)
                val errorPin = stringResource(R.string.parent_error_pin)
                val enabledToast = stringResource(R.string.parent_enabled_toast)
                val schoolLabel = stringResource(R.string.parent_school_alarm_label, childName.trim())
                Button(
                    onClick = {
                        if (parentName.isBlank() || childName.isBlank()) {
                            Toast.makeText(context, errorNames, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (!PinSecurity.isValidPin(pin) || pin != pinConfirm) {
                            Toast.makeText(context, errorPin, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val enabled = parentManager.setupParentMode(
                            parentName = parentName,
                            parentEmail = parentEmail,
                            pin = pin,
                            childName = childName,
                            childAge = childAge,
                            schoolWakeHour = schoolWakeHour,
                            schoolWakeMinute = schoolWakeMinute,
                            strictLock = true
                        )
                        if (!enabled) {
                            Toast.makeText(context, errorPin, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        // Schedule the school alarm, unless a routine for this child is already there:
                        // turning Parent Mode off and on again must not pile up duplicate alarms.
                        scope.launch {
                            val weekdays = setOf(1, 2, 3, 4, 5)
                            val existing = app.repository.observeAlarms().first().firstOrNull {
                                it.label == schoolLabel && it.repeatDays == weekdays
                            }
                            val schoolAlarm = (existing ?: Alarm(
                                hour = schoolWakeHour,
                                minute = schoolWakeMinute,
                                label = schoolLabel,
                                repeatDays = weekdays,
                                mode = AlarmMode.MEDIUM,
                                enabledMediumTasks = setOf(MediumTaskType.READING, MediumTaskType.MATH),
                                wakeCheck = true,
                                morningBriefing = true
                            )).copy(hour = schoolWakeHour, minute = schoolWakeMinute, isEnabled = true)
                            val savedId = app.repository.save(schoolAlarm)
                            val saved = if (schoolAlarm.id == 0L) schoolAlarm.copy(id = savedId) else schoolAlarm
                            AlarmScheduler(context).schedule(saved)
                            isModeActive = true
                            Toast.makeText(context, enabledToast, Toast.LENGTH_LONG).show()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.parent_enable), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    if (showDisableDialog) {
        val disabledToast = stringResource(R.string.parent_disabled_toast)
        ParentPinDialog(
            manager = parentManager,
            message = stringResource(R.string.parent_disable_message),
            confirmLabel = stringResource(R.string.action_confirm),
            onVerified = {
                parentManager.disableParentModeAfterPinCheck()
                isModeActive = false
                showDisableDialog = false
                Toast.makeText(context, disabledToast, Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showDisableDialog = false }
        )
    }
    if (showLockOffDialog) {
        ParentPinDialog(
            manager = parentManager,
            message = stringResource(R.string.parent_lock_off_message),
            confirmLabel = stringResource(R.string.action_confirm),
            onVerified = {
                parentManager.disableStrictLockAfterPinCheck()
                strictLock = false
                showLockOffDialog = false
            },
            onDismiss = { showLockOffDialog = false }
        )
    }
}
