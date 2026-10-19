package com.example.alarmboss.ui.ringing

import android.animation.ValueAnimator
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.alarmboss.R
import com.example.alarmboss.alarm.AlarmReceiver
import com.example.alarmboss.alarm.AlarmService
import com.example.alarmboss.alarm.FollowUp
import com.example.alarmboss.data.Alarm
import com.example.alarmboss.data.AlarmMode
import com.example.alarmboss.data.MediumTaskType
import com.example.alarmboss.data.MentalExerciseType
import com.example.alarmboss.data.StrictExerciseCategory
import com.example.alarmboss.ui.ringing.tasks.BarcodeTaskScreen
import com.example.alarmboss.ui.ringing.tasks.LyricsTaskScreen
import com.example.alarmboss.ui.ringing.tasks.MathTaskScreen
import com.example.alarmboss.ui.ringing.tasks.MazeTaskScreen
import com.example.alarmboss.ui.ringing.tasks.MemoryTaskScreen
import com.example.alarmboss.ui.ringing.tasks.PatternLockTaskScreen
import com.example.alarmboss.ui.ringing.tasks.ReadingTaskScreen
import com.example.alarmboss.ui.ringing.tasks.WordScrambleTaskScreen
import com.example.alarmboss.data.ParentModeManager
import com.example.alarmboss.ui.common.HoldToConfirmButton
import com.example.alarmboss.ui.parent.ParentPinDialog
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.alarmboss.ui.ringing.tasks.StepCounterTaskScreen
import java.util.Locale
import com.example.alarmboss.ui.theme.AlarmBossTheme
import com.example.alarmboss.util.StreakManager
import com.example.alarmboss.util.formatTime
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Full-screen activity shown over the lock screen when an alarm fires. This is the ONLY
 * place the alarm can be dismissed from -- back/home are suppressed where the platform
 * allows it, and the underlying AlarmService keeps sound/vibration going until dismiss()
 * is actually reached, so the mode's task genuinely has to be completed.
 */
class AlarmRingingActivity : ComponentActivity() {

    private val viewModel: AlarmRingingViewModel by viewModels()
    private var brightnessAnimator: ValueAnimator? = null
    private var tts: TextToSpeech? = null
    private var isBatteryCritical by mutableStateOf(false)
    private var showMorningQuest by mutableStateOf(false)
    private var askParentPinToStop by mutableStateOf(false)
    private val parentMode by lazy { ParentModeManager(this) }
    private var batteryAlertJob: kotlinx.coroutines.Job? = null
    private var isAlarmActive = true

    private val batteryReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            when (intent?.action) {
                android.content.Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                    val pct = if (scale > 0) (level * 100) / scale else -1
                    val status = intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
                    val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == android.os.BatteryManager.BATTERY_STATUS_FULL

                    if (pct in 0..5 && !isCharging && isAlarmActive) {
                        if (!isBatteryCritical) {
                            isBatteryCritical = true
                            startBatteryDictationLoop()
                        }
                    } else if (isCharging) {
                        if (isBatteryCritical) {
                            isBatteryCritical = false
                            batteryAlertJob?.cancel()
                            tts?.speak(getString(R.string.ringing_tts_plugged_in), TextToSpeech.QUEUE_FLUSH, null, "plugged_in")
                        }
                    }
                }
                android.content.Intent.ACTION_POWER_CONNECTED -> {
                    if (isBatteryCritical) {
                        isBatteryCritical = false
                        batteryAlertJob?.cancel()
                        tts?.speak(getString(R.string.ringing_tts_plugged_in), TextToSpeech.QUEUE_FLUSH, null, "plugged_in")
                    }
                }
            }
        }
    }

    private fun startBatteryDictationLoop() {
        batteryAlertJob?.cancel()
        batteryAlertJob = lifecycleScope.launch {
            while (isActive && isBatteryCritical && isAlarmActive) {
                tts?.speak(getString(R.string.ringing_tts_battery_low), TextToSpeech.QUEUE_FLUSH, null, "battery_alert")
                kotlinx.coroutines.delay(3500)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        val filter = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_BATTERY_CHANGED)
            addAction(android.content.Intent.ACTION_POWER_CONNECTED)
            addAction(android.content.Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(batteryReceiver, filter)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }

        val dynamicBrightnessRequested = intent.getBooleanExtra(EXTRA_DYNAMIC_BRIGHTNESS, false)
        if (dynamicBrightnessRequested) {
            applyInitialDimBrightness()
        }

        // Block the back gesture/button so the alarm can't be dismissed without completing its task.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* no-op by design */ }
        })

        val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
        val isWakeCheck = intent.getStringExtra(AlarmReceiver.EXTRA_FOLLOW_UP) == FollowUp.WAKE_CHECK.name
        viewModel.load(alarmId, isWakeCheck)

        setContent {
            AlarmBossTheme {
                val session by viewModel.session.collectAsState()
                LaunchedEffect(session) {
                    if (session?.alarm?.dynamicBrightness == true) {
                        startDynamicBrightnessRamp()
                    }
                }

                val sunriseBrush = remember {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E1035),
                            Color(0xFF5A2A55),
                            Color(0xFFC04E38),
                            Color(0xFFE88A45),
                            Color(0xFFFFD56B)
                        )
                    )
                }

                val isSunrise = session?.alarm?.sunriseMode == true

                Surface(
                    Modifier
                        .fillMaxSize()
                        .then(if (isSunrise) Modifier.background(sunriseBrush) else Modifier)
                        // Observe only (Initial pass, nothing consumed): the first touch marks reaction time.
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                awaitPointerEvent(PointerEventPass.Initial)
                                viewModel.onFirstTouch()
                            }
                        }
                ) {
                    Box(Modifier.fillMaxSize()) {
                        session?.let {
                            RingingContent(
                                it,
                                onDismiss = { dismissAlarm(it) },
                                onSnooze = ::snoozeAlarm,
                                onEmergencyStop = ::requestEmergencyStop,
                                onUsePuzzleInstead = viewModel::useStrictPuzzleAlternative
                            )
                        } ?: LoadingContent()

                        // Critical battery warning banner
                        if (isBatteryCritical) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .align(Alignment.TopCenter),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.error,
                                tonalElevation = 8.dp
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        stringResource(R.string.ringing_battery_low_title),
                                        color = MaterialTheme.colorScheme.onError,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        stringResource(R.string.ringing_battery_low_body),
                                        color = MaterialTheme.colorScheme.onError,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        // With Parent Mode's lock on, ending an alarm without its challenge is the
                        // parent's decision, not the child's.
                        if (askParentPinToStop) {
                            ParentPinDialog(
                                manager = parentMode,
                                message = stringResource(R.string.parent_pin_stop_message),
                                confirmLabel = stringResource(R.string.action_confirm),
                                onVerified = {
                                    askParentPinToStop = false
                                    emergencyStop()
                                },
                                onDismiss = { askParentPinToStop = false }
                            )
                        }

                        if (showMorningQuest) {
                            MorningQuestDialog(
                                isMorningStreakAwarded = viewModel.lastWakeResult?.isMorningWindow ?: true,
                                streakCount = viewModel.lastWakeResult?.currentStreak ?: session?.currentStreak ?: 1,
                                onClaimBonus = { bonusXp ->
                                    viewModel.addQuestBonusXp(bonusXp)
                                    finish()
                                },
                                onSkip = { finish() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun applyInitialDimBrightness() {
        val lp = window.attributes
        lp.screenBrightness = 0.05f
        window.attributes = lp
    }

    private fun startDynamicBrightnessRamp() {
        if (brightnessAnimator != null) return
        applyInitialDimBrightness()

        brightnessAnimator = ValueAnimator.ofFloat(0.05f, 1.0f).apply {
            duration = 20_000L // Gradually increase to full brightness over 20 seconds
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val v = animator.animatedValue as Float
                val lp = window.attributes
                lp.screenBrightness = v
                window.attributes = lp
            }
            start()
        }
    }

    private fun showOverLockScreen() {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        // Keep the screen on for as long as the challenge takes, so it can't time out mid-puzzle.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun snoozeAlarm() {
        viewModel.snooze()
        AlarmService.stop(this)
        finish()
    }

    private fun dismissAlarm(session: RingSession) {
        if (!viewModel.completeWakeUp()) return
        isAlarmActive = false
        batteryAlertJob?.cancel()
        AlarmService.stop(this)
        // Passing the wake-up check is not a fresh wake-up, so it gets no briefing and no morning quest.
        if (session.isWakeCheck) {
            finish()
            return
        }
        if (session.alarm.morningBriefing) {
            val time = formatTime(session.alarm.hour, session.alarm.minute, DateFormat.is24HourFormat(this))
            tts?.speak(
                getString(R.string.ringing_briefing, time, session.currentStreak),
                TextToSpeech.QUEUE_FLUSH, null, "briefing"
            )
        }
        showMorningQuest = true
    }

    private fun requestEmergencyStop() {
        if (parentMode.isLockActive()) askParentPinToStop = true else emergencyStop()
    }

    private fun emergencyStop() {
        if (!viewModel.emergencyStop()) return
        AlarmService.stop(this)
        finish()
    }

    override fun onDestroy() {
        brightnessAnimator?.cancel()
        brightnessAnimator = null
        batteryAlertJob?.cancel()
        batteryAlertJob = null
        runCatching { unregisterReceiver(batteryReceiver) }
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_DYNAMIC_BRIGHTNESS = "extra_dynamic_brightness"
        const val EXTRA_SUNRISE_MODE = "extra_sunrise_mode"
        const val EXTRA_MORNING_BRIEFING = "extra_morning_briefing"
    }
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun RingingContent(
    session: RingSession,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    onEmergencyStop: () -> Unit,
    onUsePuzzleInstead: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { RingingBody(session, onDismiss, onSnooze, onUsePuzzleInstead) }
        // Easy alarms already have a one-tap Dismiss; every harder mode needs a way out too.
        if (session.alarm.mode != AlarmMode.EASY) EmergencyStopBar(onEmergencyStop)
    }
}

/** A deliberately slow way to end an alarm whose challenge can't be completed. */
@Composable
private fun EmergencyStopBar(onConfirmed: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.emergency_stop_prompt), style = MaterialTheme.typography.bodySmall)
        HoldToConfirmButton(
            label = stringResource(R.string.emergency_stop_hold, EMERGENCY_STOP_SECONDS),
            holdMillis = EMERGENCY_STOP_SECONDS * 1000L,
            onConfirmed = onConfirmed
        )
        Text(
            stringResource(R.string.emergency_stop_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private const val EMERGENCY_STOP_SECONDS = 10

@Composable
private fun RingingBody(
    session: RingSession,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    onUsePuzzleInstead: () -> Unit
) {
    val alarm = session.alarm
    val plan = session.plan

    if (session.isWakeCheck) {
        WakeCheckScreen(onSolved = onDismiss)
        return
    }

    if (plan.isBossFight) {
        BossFightScreen(difficulty = plan.difficulty, onSolved = onDismiss)
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
        ) {
            Text(
                text = "🔥 " + pluralStringResource(R.plurals.ringing_streak, session.currentStreak, session.currentStreak),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        when (alarm.mode) {
            AlarmMode.EASY -> EasyDismissScreen(
                alarm = alarm,
                snoozeMinutes = session.snoozeMinutes,
                onDismiss = onDismiss,
                onSnooze = onSnooze
            )
            AlarmMode.MEDIUM -> {
                val task = remember(alarm.id) { alarm.enabledMediumTasks.randomOrNull() ?: MediumTaskType.MATH }
                when (task) {
                    MediumTaskType.MATH -> MathTaskScreen(difficulty = plan.difficulty, onSolved = onDismiss)
                    MediumTaskType.READING -> ReadingTaskScreen(onDone = onDismiss)
                    MediumTaskType.LYRICS -> LyricsTaskScreen(onDone = onDismiss)
                    MediumTaskType.BARCODE -> BarcodeTaskScreen(onScanned = onDismiss)
                    MediumTaskType.PATTERN_LOCK -> PatternLockTaskScreen(difficulty = plan.difficulty, onSolved = onDismiss)
                    MediumTaskType.WORD_SCRAMBLE -> WordScrambleTaskScreen(difficulty = plan.difficulty, onSolved = onDismiss)
                    MediumTaskType.STEPS -> StepCounterTaskScreen(difficulty = plan.difficulty, onSolved = onDismiss)
                }
            }
            AlarmMode.STRICT -> {
                if (alarm.strictCategory == StrictExerciseCategory.MENTAL) {
                    when (alarm.mentalExerciseType) {
                        MentalExerciseType.MEMORY_GRID -> MemoryTaskScreen(onSolved = onDismiss)
                        MentalExerciseType.MAZE -> MazeTaskScreen(difficulty = plan.difficulty, onSolved = onDismiss)
                    }
                } else {
                    var puzzleInstead by remember(alarm.id) { mutableStateOf(false) }
                    if (puzzleInstead) {
                        MathTaskScreen(difficulty = plan.difficulty, onSolved = onDismiss)
                    } else {
                        StrictExerciseScreen(
                            initialExercise = alarm.exerciseType,
                            durationSeconds = alarm.exerciseDurationSeconds,
                            onComplete = onDismiss,
                            onUsePuzzleInstead = {
                                onUsePuzzleInstead()
                                puzzleInstead = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EasyDismissScreen(alarm: Alarm, snoozeMinutes: Int, onDismiss: () -> Unit, onSnooze: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("⏰", fontSize = 48.sp)
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            formatTime(alarm.hour, alarm.minute, DateFormat.is24HourFormat(LocalContext.current)),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Text(
                alarm.label.ifBlank { stringResource(R.string.ringing_default_label) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        Spacer(Modifier.height(36.dp))

        Button(
            onClick = onDismiss,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(
                stringResource(R.string.ringing_dismiss),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }

        Spacer(Modifier.height(14.dp))

        OutlinedButton(
            onClick = onSnooze,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(
                stringResource(R.string.ringing_snooze, snoozeMinutes),
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        ) {
            Text(
                stringResource(R.string.snooze_penalty_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun MorningQuestDialog(
    isMorningStreakAwarded: Boolean,
    streakCount: Int,
    onClaimBonus: (Int) -> Unit,
    onSkip: () -> Unit
) {
    var waterDone by remember { mutableStateOf(false) }
    var bedDone by remember { mutableStateOf(false) }
    var stretchDone by remember { mutableStateOf(false) }

    val bonusTotal = (if (waterDone) 25 else 0) + (if (bedDone) 25 else 0) + (if (stretchDone) 25 else 0)

    AlertDialog(
        onDismissRequest = onSkip,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isMorningStreakAwarded) "🔥" else "☀️", fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.morning_quest_title), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Morning Window Streak Rule Banner
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    color = if (isMorningStreakAwarded) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isMorningStreakAwarded) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (isMorningStreakAwarded) {
                                pluralStringResource(R.plurals.morning_quest_streak_awarded, streakCount, streakCount)
                            } else {
                                stringResource(R.string.morning_quest_no_streak)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = if (isMorningStreakAwarded) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (isMorningStreakAwarded) {
                                stringResource(R.string.morning_quest_window_hit, StreakManager.MORNING_WINDOW_START_HOUR, StreakManager.MORNING_WINDOW_END_HOUR)
                            } else {
                                stringResource(R.string.morning_quest_window_missed, StreakManager.MORNING_WINDOW_START_HOUR, StreakManager.MORNING_WINDOW_END_HOUR)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isMorningStreakAwarded) MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    stringResource(R.string.morning_quest_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))

                MorningQuestOption(
                    emoji = "💧",
                    text = stringResource(R.string.morning_quest_water),
                    checked = waterDone,
                    onCheckedChange = { waterDone = it }
                )
                MorningQuestOption(
                    emoji = "🛏️",
                    text = stringResource(R.string.morning_quest_bed),
                    checked = bedDone,
                    onCheckedChange = { bedDone = it }
                )
                MorningQuestOption(
                    emoji = "🧘",
                    text = stringResource(R.string.morning_quest_stretch),
                    checked = stretchDone,
                    onCheckedChange = { stretchDone = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onClaimBonus(bonusTotal) },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.morning_quest_claim, bonusTotal), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.morning_quest_skip))
            }
        }
    )
}

@Composable
private fun MorningQuestOption(
    emoji: String,
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = if (checked) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Spacer(Modifier.width(4.dp))
            Text(emoji, fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
        }
    }
}
