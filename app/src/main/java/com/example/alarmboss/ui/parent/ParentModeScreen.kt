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
import com.example.alarmboss.util.formatTime12h
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentModeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as AlarmBossApp
    val parentManager = remember { ParentModeManager(context) }

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

    // Dialog for disabling / exiting parent mode
    var showDisableDialog by remember { mutableStateOf(false) }
    var enteredPinForDisable by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

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
                        Text("Parent Mode (Ages 9-12)", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                            Text("Parent Mode Active & Protected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "Supervised by ${parentManager.getParentName()} for ${parentManager.getChildName()} (Age ${parentManager.getChildAge()})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("School Wake-Up", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatTime12h(parentManager.getSchoolWakeHour(), parentManager.getSchoolWakeMinute()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column {
                                Text("Bedtime Reminder", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatTime12h(parentManager.getBedtimeHour(), parentManager.getBedtimeMinute()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("Strict Child PIN Lock", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Prevents child from deleting or modifying alarms without your 4-digit PIN", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = strictLock,
                                onCheckedChange = {
                                    strictLock = it
                                    parentManager.updateStrictLock(it)
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
                            Text("Disable Parent Mode (Requires PIN)", fontWeight = FontWeight.Bold)
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
                            Text("Parent Details Verification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = parentName,
                            onValueChange = { parentName = it },
                            label = { Text("Parent / Guardian Name") },
                            placeholder = { Text("e.g. Sarah Jenkins") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = parentEmail,
                            onValueChange = { parentEmail = it },
                            label = { Text("Parent Email (for recovery)") },
                            placeholder = { Text("parent@example.com") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = pin,
                                onValueChange = { if (it.length <= 4) pin = it },
                                label = { Text("4-Digit PIN") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = pinConfirm,
                                onValueChange = { if (it.length <= 4) pinConfirm = it },
                                label = { Text("Confirm PIN") },
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
                            Text("Child / Ward Profile (Ages 9-12)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = childName,
                            onValueChange = { childName = it },
                            label = { Text("Child's First Name") },
                            placeholder = { Text("e.g. Leo") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Select Age (Curates educational challenge difficulty):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(9, 10, 11, 12).forEach { age ->
                                FilterChip(
                                    selected = childAge == age,
                                    onClick = { childAge = age },
                                    label = { Text("$age yrs", fontWeight = FontWeight.Bold) },
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
                                    TimePickerDialog(context, { _, h, m -> schoolWakeHour = h; schoolWakeMinute = m }, schoolWakeHour, schoolWakeMinute, false).show()
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("School Morning Wake-Up", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Mon - Fri routine with locked awake check", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(6.dp))
                                    Text(formatTime12h(schoolWakeHour, schoolWakeMinute), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (parentName.isBlank() || childName.isBlank()) {
                            Toast.makeText(context, "Please enter parent and child names", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (pin.length != 4 || pin != pinConfirm) {
                            Toast.makeText(context, "Please enter matching 4-digit PINs", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        parentManager.setupParentMode(
                            parentName = parentName,
                            parentEmail = parentEmail,
                            pin = pin,
                            childName = childName,
                            childAge = childAge,
                            schoolWakeHour = schoolWakeHour,
                            schoolWakeMinute = schoolWakeMinute,
                            strictLock = true
                        )

                        // Automatically schedule the school alarm for the child
                        scope.launch {
                            val schoolAlarm = Alarm(
                                hour = schoolWakeHour,
                                minute = schoolWakeMinute,
                                label = "🎒 School Routine for $childName",
                                isEnabled = true,
                                repeatDays = setOf(1, 2, 3, 4, 5),
                                mode = AlarmMode.MEDIUM,
                                enabledMediumTasks = setOf(MediumTaskType.READING, MediumTaskType.MATH),
                                wakeCheck = true,
                                morningBriefing = true
                            )
                            val savedId = app.repository.save(schoolAlarm)
                            com.example.alarmboss.alarm.AlarmScheduler(context).schedule(schoolAlarm.copy(id = savedId))
                            isModeActive = true
                            Toast.makeText(context, "Parent Mode activated & school alarm scheduled!", Toast.LENGTH_LONG).show()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Verify & Enable Parent Mode", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    if (showDisableDialog) {
        AlertDialog(
            onDismissRequest = {
                showDisableDialog = false
                enteredPinForDisable = ""
                pinError = false
            },
            title = { Text("Enter Parent PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your 4-digit security PIN to disable Parent Mode and unlock alarm controls:")
                    OutlinedTextField(
                        value = enteredPinForDisable,
                        onValueChange = {
                            enteredPinForDisable = it
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
                Button(
                    onClick = {
                        if (parentManager.disableParentMode(enteredPinForDisable)) {
                            isModeActive = false
                            showDisableDialog = false
                            enteredPinForDisable = ""
                            Toast.makeText(context, "Parent Mode disabled", Toast.LENGTH_SHORT).show()
                        } else {
                            pinError = true
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
