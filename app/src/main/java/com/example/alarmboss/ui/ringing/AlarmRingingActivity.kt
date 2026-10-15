package com.example.alarmboss.ui.ringing

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
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
import com.example.alarmboss.ui.common.HoldToConfirmButton
import com.example.alarmboss.ui.theme.AlarmBossTheme

/**
 * Full-screen activity shown over the lock screen when an alarm fires. This is the ONLY
 * place the alarm can be dismissed from -- back/home are suppressed where the platform
 * allows it, and the underlying AlarmService keeps sound/vibration going until dismiss()
 * is actually reached, so the mode's task genuinely has to be completed.
 */
class AlarmRingingActivity : ComponentActivity() {

    private val viewModel: AlarmRingingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        // Block the back gesture/button so the alarm can't be dismissed without completing its task.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* no-op by design */ }
        })

        val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
        viewModel.load(alarmId)

        setContent {
            AlarmBossTheme {
                val session by viewModel.session.collectAsState()
                Surface(
                    Modifier
                        .fillMaxSize()
                        // Observe only (Initial pass, nothing consumed): the first touch marks reaction time.
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                awaitPointerEvent(PointerEventPass.Initial)
                                viewModel.onFirstTouch()
                            }
                        }
                ) {
                    session?.let {
                        RingingContent(
                            it,
                            onDismiss = ::dismissAlarm,
                            onSnooze = ::snoozeAlarm,
                            onEmergencyStop = ::emergencyStop,
                            onUsePuzzleInstead = viewModel::useStrictPuzzleAlternative
                        )
                    } ?: LoadingContent()
                }
            }
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

    private fun dismissAlarm() {
        if (!viewModel.completeWakeUp()) return
        AlarmService.stop(this)
        finish()
    }

    private fun snoozeAlarm() {
        viewModel.snooze()
        AlarmService.stop(this)
        finish()
    }

    private fun emergencyStop() {
        if (!viewModel.emergencyStop()) return
        AlarmService.stop(this)
        finish()
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

    if (plan.isBossFight) {
        BossFightScreen(difficulty = plan.difficulty, onSolved = onDismiss)
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = pluralStringResource(R.plurals.ringing_streak, session.currentStreak, session.currentStreak),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
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
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            alarm.label.ifBlank { stringResource(R.string.ringing_default_label) },
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDismiss) { Text(stringResource(R.string.ringing_dismiss)) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onSnooze) { Text(stringResource(R.string.ringing_snooze, snoozeMinutes)) }
    }
}
