package com.example.alarmboss.ui.ringing

import android.animation.ValueAnimator
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.alarmboss.R
import com.example.alarmboss.alarm.AlarmReceiver
import com.example.alarmboss.alarm.AlarmService
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
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.alarmboss.ui.ringing.tasks.StepCounterTaskScreen
import java.util.Locale
import com.example.alarmboss.ui.theme.AlarmBossTheme

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

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
        viewModel.load(alarmId)

        setContent {
            AlarmBossTheme {
                val session by viewModel.session.collectAsState()
                var showMorningQuest by remember { mutableStateOf(false) }

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
                    session?.let {
                        RingingContent(it, onDismiss = {
                            if (viewModel.completeWakeUp()) {
                                AlarmService.stop(this@AlarmRingingActivity)
                                if (it.alarm.morningBriefing) {
                                    val timeStr = com.example.alarmboss.util.formatTime12h(it.alarm.hour, it.alarm.minute)
                                    val greeting = "Good morning! It is $timeStr. You are on a ${it.currentStreak} day streak. You have successfully conquered today's alarm!"
                                    tts?.speak(greeting, TextToSpeech.QUEUE_FLUSH, null, "briefing")
                                }
                                showMorningQuest = true
                            }
                        }, onSnooze = ::snoozeAlarm)
                    } ?: LoadingContent()

                    if (showMorningQuest) {
                        MorningQuestDialog(
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    private fun snoozeAlarm() {
        viewModel.snooze()
        AlarmService.stop(this)
        finish()
    }

    override fun onDestroy() {
        brightnessAnimator?.cancel()
        brightnessAnimator = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_DYNAMIC_BRIGHTNESS = "extra_dynamic_brightness"
        const val EXTRA_SUNRISE_MODE = "extra_sunrise_mode"
        const val EXTRA_MORNING_BRIEFING = "extra_morning_briefing"
        const val EXTRA_WAKE_CHECK = "extra_wake_check"
    }
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun RingingContent(session: RingSession, onDismiss: () -> Unit, onSnooze: () -> Unit) {
    val alarm = session.alarm
    val plan = session.plan

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
                    StrictExerciseScreen(
                        initialExercise = alarm.exerciseType,
                        durationSeconds = alarm.exerciseDurationSeconds,
                        onComplete = onDismiss
                    )
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
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            modifier = Modifier.size(130.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.app_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(116.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            com.example.alarmboss.util.formatTime12h(alarm.hour, alarm.minute),
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
private fun MorningQuestDialog(onClaimBonus: (Int) -> Unit, onSkip: () -> Unit) {
    var waterDone by remember { mutableStateOf(false) }
    var bedDone by remember { mutableStateOf(false) }
    var stretchDone by remember { mutableStateOf(false) }

    val bonusTotal = (if (waterDone) 25 else 0) + (if (bedDone) 25 else 0) + (if (stretchDone) 25 else 0)

    AlertDialog(
        onDismissRequest = onSkip,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("☀️", fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.morning_quest_title), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
