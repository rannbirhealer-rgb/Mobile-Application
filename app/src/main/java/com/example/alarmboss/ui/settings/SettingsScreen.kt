package com.example.alarmboss.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alarmboss.R
import com.example.alarmboss.util.SnoozeSettings
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Spa
import androidx.compose.ui.platform.LocalContext
import com.example.alarmboss.alarm.AlarmDeviceAdminReceiver
import com.example.alarmboss.data.ParentModeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenParentMode: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val parentManager = remember { ParentModeManager(context) }

    val snoozeMinutes by viewModel.snoozeMinutes.collectAsState()
    val passagesText by viewModel.passagesText.collectAsState()
    val lyricsText by viewModel.lyricsText.collectAsState()
    val barcodeText by viewModel.barcodeText.collectAsState()
    val isGeneratingPassages by viewModel.isGeneratingPassages.collectAsState()

    val dpm = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val adminComponent = remember { ComponentName(context, AlarmDeviceAdminReceiver::class.java) }
    var isAdminActive by remember { mutableStateOf(dpm.isAdminActive(adminComponent)) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Parent Mode Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Parent Mode (Ages 9-12)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (parentManager.isParentModeActive()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (parentManager.isParentModeActive()) "PROTECTED" else "SETUP",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (parentManager.isParentModeActive()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = if (parentManager.isParentModeActive()) {
                            "Active for ${parentManager.getChildName()} (Age ${parentManager.getChildAge()}). School wakeup set to ${com.example.alarmboss.util.formatTime12h(parentManager.getSchoolWakeHour(), parentManager.getSchoolWakeMinute())} with locked awake safeguard."
                        } else {
                            "Configure child-friendly morning routines (Ages 9-12) and lock alarms behind a 4-digit security PIN so your child cannot disable them."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onOpenParentMode,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (parentManager.isParentModeActive()) "Manage Parent Controls & Schedule" else "Setup Parent Mode", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Sleep & Wellness Counselor Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Spa, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sleep & Wellness Counselor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        "Provides humane, non-judgmental sleep hygiene counseling and bedtime wellness guidance. Open the Sleep AI tab to chat anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Privacy & Anti-Deepfake Guarantee Card (Contrasting Alarmy)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Privacy & Anti-Deepfake Guarantee", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        "Unlike other alarm apps (e.g. Alarmy) that demand photos of your home/bathroom and record ambient bedroom audio, AlarmBoss is 100% privacy-respecting:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🛡️ No Home Photos: Zero images of your private residence are captured or stored, eliminating deepfake and biometric risks.", style = MaterialTheme.typography.bodySmall)
                            Text("🎙️ Zero Nighttime Audio Snooping: No background voice or snoring eavesdropping.", style = MaterialTheme.typography.bodySmall)
                            Text("📱 On-Device Processing: Barcode scans and ML Kit pose gestures run purely on-device.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Snooze Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_snooze_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        stringResource(R.string.settings_snooze_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SnoozeSettings.OPTIONS.forEach { option ->
                            FilterChip(
                                selected = snoozeMinutes == option,
                                onClick = { viewModel.setSnoozeMinutes(option) },
                                label = { Text(stringResource(R.string.settings_snooze_option, option), fontWeight = FontWeight.SemiBold) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Sound, accessibility, follow-up rings and holidays
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SoundSection(viewModel)
                    HorizontalDivider()
                    AccessibilitySection(viewModel)
                    HorizontalDivider()
                    FollowUpSection(viewModel)
                    HorizontalDivider()
                    HolidaySection(viewModel)
                }
            }

            // Registered Barcode Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_registered_barcode_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        stringResource(R.string.settings_registered_barcode_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = barcodeText,
                        onValueChange = viewModel::onBarcodeChange,
                        singleLine = true,
                        placeholder = { Text("e.g. 8901030383742") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = viewModel::saveBarcode,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.settings_save_barcode), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Custom Content (Passages & Lyrics) Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_content_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        stringResource(R.string.settings_content_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Text(
                        stringResource(R.string.settings_passages_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = passagesText,
                        onValueChange = viewModel::onPassagesChange,
                        shape = RoundedCornerShape(12.dp),
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = viewModel::savePassages,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(stringResource(R.string.settings_save_passages), fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = viewModel::generatePassagesWithGemini,
                            enabled = !isGeneratingPassages,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isGeneratingPassages) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Generating...")
                            } else {
                                Icon(Icons.Default.Spa, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("✨ AI Generate")
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Text(
                        stringResource(R.string.settings_lyrics_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = lyricsText,
                        onValueChange = viewModel::onLyricsChange,
                        shape = RoundedCornerShape(12.dp),
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = viewModel::saveLyrics,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.settings_save_lyrics), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
