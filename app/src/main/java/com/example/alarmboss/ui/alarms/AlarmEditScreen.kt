package com.example.alarmboss.ui.alarms

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alarmboss.R
import com.example.alarmboss.data.*
import com.example.alarmboss.engine.AlarmTimeCalculator
import java.time.LocalDate
import java.time.LocalDateTime
import com.example.alarmboss.util.formatTime
import java.time.DayOfWeek
import java.time.format.TextStyle

@Composable
fun AlarmEditScreen(
    alarmId: Long?,
    onDone: () -> Unit,
    viewModel: AlarmViewModel = viewModel()
) {
    if (alarmId == null) {
        AlarmEditForm(base = null, onDone = onDone, viewModel = viewModel)
        return
    }

    LaunchedEffect(alarmId) { viewModel.loadForEdit(alarmId) }
    when (val target = viewModel.editTarget.collectAsState().value) {
        EditTarget.Loading ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        EditTarget.Missing -> LaunchedEffect(Unit) { onDone() }
        is EditTarget.Existing -> AlarmEditForm(base = target.alarm, onDone = onDone, viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditForm(
    base: Alarm?,
    onDone: () -> Unit,
    viewModel: AlarmViewModel
) {
    val context = LocalContext.current
    val is24Hour = rememberIs24Hour()

    var hour by rememberSaveable(base) { mutableStateOf(base?.hour ?: 7) }
    var minute by rememberSaveable(base) { mutableStateOf(base?.minute ?: 0) }
    var label by rememberSaveable(base) { mutableStateOf(base?.label ?: "") }
    var mode by rememberSaveable(base) { mutableStateOf(base?.mode ?: AlarmMode.EASY) }
    var days by rememberSaveable(base) { mutableStateOf(base?.repeatDays ?: emptySet()) }
    // Kept as an ISO string so it fits the saved-state bundle; mutually exclusive with `days`.
    var specificDateIso by rememberSaveable(base) { mutableStateOf(base?.specificDate) }
    val specificDate = specificDateIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val dateHasPassed = specificDate != null &&
        AlarmTimeCalculator.nextTrigger(hour, minute, emptySet(), LocalDateTime.now(), specificDate) == null
    var soundUri by rememberSaveable(base) { mutableStateOf(base?.soundUri) }
    var vibrate by rememberSaveable(base) { mutableStateOf(base?.vibrate ?: true) }
    var enabledTasks by rememberSaveable(base) { mutableStateOf(base?.enabledMediumTasks ?: MediumTaskType.values().toSet()) }
    var exerciseType by rememberSaveable(base) { mutableStateOf(base?.exerciseType ?: ExerciseType.SQUATS) }
    var exerciseSeconds by rememberSaveable(base) { mutableStateOf((base?.exerciseDurationSeconds ?: 300).toFloat()) }

    var strictCategory by rememberSaveable(base) { mutableStateOf(base?.strictCategory ?: StrictExerciseCategory.PHYSICAL) }
    var mentalExerciseType by rememberSaveable(base) { mutableStateOf(base?.mentalExerciseType ?: MentalExerciseType.MEMORY_GRID) }

    var dynamicBrightness by rememberSaveable(base) { mutableStateOf(base?.dynamicBrightness ?: false) }
    var flashLightBlink by rememberSaveable(base) { mutableStateOf(base?.flashLightBlink ?: false) }
    var gradualVolume by rememberSaveable(base) { mutableStateOf(base?.gradualVolume ?: false) }
    var sunriseMode by rememberSaveable(base) { mutableStateOf(base?.sunriseMode ?: false) }
    var wakeCheck by rememberSaveable(base) { mutableStateOf(base?.wakeCheck ?: false) }
    var morningBriefing by rememberSaveable(base) { mutableStateOf(base?.morningBriefing ?: false) }

    val soundPicker = rememberSoundPickerLauncher { uri -> soundUri = uri }
    var showCameraWarning by rememberSaveable { mutableStateOf(false) }

    fun saveAlarm() {
        val effectiveWakeCheck = if (mode != AlarmMode.EASY) true else wakeCheck
        viewModel.save(
            Alarm(
                id = base?.id ?: 0L,
                hour = hour, minute = minute, label = label,
                isEnabled = base?.isEnabled ?: true,
                repeatDays = if (specificDate != null) emptySet() else days,
                specificDate = specificDate?.toString(),
                mode = mode, soundUri = soundUri,
                vibrate = vibrate, enabledMediumTasks = enabledTasks,
                exerciseDurationSeconds = exerciseSeconds.toInt(),
                exerciseType = exerciseType,
                strictCategory = strictCategory,
                mentalExerciseType = mentalExerciseType,
                dynamicBrightness = dynamicBrightness,
                flashLightBlink = flashLightBlink,
                gradualVolume = gradualVolume,
                sunriseMode = sunriseMode,
                wakeCheck = effectiveWakeCheck,
                morningBriefing = morningBriefing
            )
        )
        onDone()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.CAMERA] == false) showCameraWarning = true else saveAlarm()
    }

    fun onSaveClicked() {
        val missing = requiredPermissions(mode, strictCategory, enabledTasks).filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) saveAlarm() else permissionLauncher.launch(missing.toTypedArray())
    }

    if (showCameraWarning) {
        AlertDialog(
            onDismissRequest = { showCameraWarning = false },
            title = { Text(stringResource(R.string.edit_camera_warning_title)) },
            text = { Text(stringResource(R.string.edit_camera_warning_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showCameraWarning = false
                    saveAlarm()
                }) { Text(stringResource(R.string.edit_camera_warning_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showCameraWarning = false }) {
                    Text(stringResource(R.string.edit_camera_warning_keep_editing))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (base == null) R.string.edit_title_new else R.string.edit_title_edit),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
                actions = {
                    // Saving a pinned moment that has already gone would create an alarm that never rings.
                    Button(
                        onClick = ::onSaveClicked,
                        enabled = !dateHasPassed,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
                    }
                },
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
            // Hero Time Picker Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, is24Hour).show()
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = formatTime(hour, minute, is24Hour),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Tap to change alarm time",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Schedule & Repeat Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text(stringResource(R.string.edit_label)) },
                        placeholder = { Text("e.g. Morning Battle, Gym Wake-Up") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        stringResource(R.string.edit_repeat),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    DaySelector(
                        selected = days,
                        onChange = {
                            days = it
                            if (it.isNotEmpty()) specificDateIso = null
                        }
                    )
                    RepeatExtras(
                        days = days,
                        specificDate = specificDate,
                        dateHasPassed = dateHasPassed,
                        onDaysChange = {
                            days = it
                            specificDateIso = null
                        },
                        onDateChange = { picked ->
                            specificDateIso = picked?.toString()
                            if (picked != null) days = emptySet()
                        }
                    )
                }
            }

            // Challenge Mode & Tasks Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        stringResource(R.string.edit_mode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        AlarmMode.values().forEachIndexed { index, m ->
                            SegmentedButton(
                                selected = mode == m,
                                onClick = { mode = m },
                                shape = SegmentedButtonDefaults.itemShape(index, AlarmMode.values().size)
                            ) { Text(modeName(m), fontWeight = FontWeight.Bold) }
                        }
                    }

                    when (mode) {
                        AlarmMode.EASY -> Text(
                            stringResource(R.string.edit_easy_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AlarmMode.MEDIUM -> MediumTaskPicker(enabledTasks) { enabledTasks = it }
                        AlarmMode.STRICT -> StrictConfig(
                            strictCategory = strictCategory,
                            onStrictCategoryChange = { strictCategory = it },
                            exerciseType = exerciseType,
                            onExerciseTypeChange = { exerciseType = it },
                            mentalExerciseType = mentalExerciseType,
                            onMentalExerciseTypeChange = { mentalExerciseType = it },
                            seconds = exerciseSeconds,
                            onSecondsChange = { exerciseSeconds = it }
                        )
                    }
                }
            }

            // Sound & Sensory Effects Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Sound & Sensory Awakening",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Sound Picker Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.edit_sound), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = { soundPicker() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(stringResource(if (soundUri == null) R.string.edit_sound_default else R.string.edit_sound_custom))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Switches for features
                    FeatureSwitchRow(
                        title = stringResource(R.string.edit_vibrate),
                        subtitle = "Vibrate device while alarm is sounding",
                        checked = vibrate,
                        onCheckedChange = { vibrate = it }
                    )

                    FeatureSwitchRow(
                        title = stringResource(R.string.edit_gradual_volume),
                        subtitle = stringResource(R.string.edit_gradual_volume_description),
                        checked = gradualVolume,
                        onCheckedChange = { gradualVolume = it }
                    )

                    FeatureSwitchRow(
                        title = stringResource(R.string.edit_sunrise_mode),
                        subtitle = stringResource(R.string.edit_sunrise_mode_description),
                        checked = sunriseMode,
                        onCheckedChange = { sunriseMode = it }
                    )

                    FeatureSwitchRow(
                        title = stringResource(R.string.edit_dynamic_brightness),
                        subtitle = stringResource(R.string.edit_dynamic_brightness_description),
                        checked = dynamicBrightness,
                        onCheckedChange = { dynamicBrightness = it }
                    )

                    FeatureSwitchRow(
                        title = stringResource(R.string.edit_flashlight_blink),
                        subtitle = stringResource(R.string.edit_flashlight_blink_description),
                        checked = flashLightBlink,
                        onCheckedChange = { flashLightBlink = it }
                    )
                }
            }

            // Safeguards & Routine Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Safeguards & Morning Boost",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val isWakeCheckLocked = mode == AlarmMode.MEDIUM || mode == AlarmMode.STRICT
                    FeatureSwitchRow(
                        title = stringResource(R.string.edit_wake_check),
                        subtitle = if (isWakeCheckLocked) {
                            stringResource(R.string.edit_wake_check_locked, modeName(mode))
                        } else {
                            stringResource(R.string.edit_wake_check_description)
                        },
                        checked = if (isWakeCheckLocked) true else wakeCheck,
                        enabled = !isWakeCheckLocked,
                        lockedBadge = if (isWakeCheckLocked) "🔒 Locked ON" else null,
                        onCheckedChange = { if (!isWakeCheckLocked) wakeCheck = it }
                    )

                    FeatureSwitchRow(
                        title = stringResource(R.string.edit_morning_briefing),
                        subtitle = stringResource(R.string.edit_morning_briefing_description),
                        checked = morningBriefing,
                        onCheckedChange = { morningBriefing = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    lockedBadge: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (lockedBadge != null) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = lockedBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun DaySelector(selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        (1..7).forEach { iso ->
            val label = DayOfWeek.of(iso).getDisplayName(TextStyle.NARROW, locale)
            val isSelected = selected.contains(iso)

            Surface(
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .size(38.dp)
                    .clickable {
                        onChange(if (isSelected) selected - iso else selected + iso)
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MediumTaskPicker(enabled: Set<MediumTaskType>, onChange: (Set<MediumTaskType>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.edit_medium_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))

        val taskIcons = mapOf(
            MediumTaskType.MATH to "📐",
            MediumTaskType.READING to "📖",
            MediumTaskType.LYRICS to "🎵",
            MediumTaskType.BARCODE to "🏷️",
            MediumTaskType.PATTERN_LOCK to "🔒",
            MediumTaskType.WORD_SCRAMBLE to "🔤",
            MediumTaskType.STEPS to "👟"
        )

        MediumTaskType.values().forEach { task ->
            val isChecked = enabled.contains(task)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                border = if (isChecked) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val next = if (!isChecked) enabled + task else enabled - task
                        if (next.isNotEmpty()) onChange(next)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = {
                            val next = if (it) enabled + task else enabled - task
                            if (next.isNotEmpty()) onChange(next)
                        }
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(taskIcons[task] ?: "", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(taskName(task), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun StrictConfig(
    strictCategory: StrictExerciseCategory,
    onStrictCategoryChange: (StrictExerciseCategory) -> Unit,
    exerciseType: ExerciseType,
    onExerciseTypeChange: (ExerciseType) -> Unit,
    mentalExerciseType: MentalExerciseType,
    onMentalExerciseTypeChange: (MentalExerciseType) -> Unit,
    seconds: Float,
    onSecondsChange: (Float) -> Unit
) {
    Column {
        Text(
            stringResource(R.string.edit_strict_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        Text(stringResource(R.string.edit_strict_category), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            StrictExerciseCategory.values().forEachIndexed { index, cat ->
                SegmentedButton(
                    selected = strictCategory == cat,
                    onClick = { onStrictCategoryChange(cat) },
                    shape = SegmentedButtonDefaults.itemShape(index, StrictExerciseCategory.values().size)
                ) {
                    Text(
                        stringResource(
                            if (cat == StrictExerciseCategory.PHYSICAL) R.string.edit_strict_physical
                            else R.string.edit_strict_mental
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (strictCategory == StrictExerciseCategory.PHYSICAL) {
            Text(stringResource(R.string.edit_physical_type), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ExerciseType.values().forEachIndexed { index, e ->
                    SegmentedButton(
                        selected = exerciseType == e,
                        onClick = { onExerciseTypeChange(e) },
                        shape = SegmentedButtonDefaults.itemShape(index, ExerciseType.values().size)
                    ) { Text(exerciseName(e), fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.edit_duration, seconds.toInt() / 60, seconds.toInt() % 60),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Slider(value = seconds, onValueChange = onSecondsChange, valueRange = 60f..900f, steps = 13)
        } else {
            Text(stringResource(R.string.edit_mental_type), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                MentalExerciseType.values().forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = mentalExerciseType == type,
                        onClick = { onMentalExerciseTypeChange(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, MentalExerciseType.values().size)
                    ) {
                        Text(mentalName(type), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
