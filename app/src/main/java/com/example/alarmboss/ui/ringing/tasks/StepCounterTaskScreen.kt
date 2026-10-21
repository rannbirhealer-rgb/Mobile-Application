package com.example.alarmboss.ui.ringing.tasks

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.alarmboss.R
import kotlinx.coroutines.delay
import kotlin.math.sqrt

@Composable
fun StepCounterTaskScreen(difficulty: Int, onSolved: () -> Unit) {
    val context = LocalContext.current
    val targetSteps = 15 + (difficulty * 5).coerceIn(5, 25) // 20 to 40 steps
    var currentSteps by remember { mutableIntStateOf(0) }
    var solved by remember { mutableStateOf(false) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    // Re-registers when the permission answer arrives. The hardware step detector delivers nothing
    // until activity recognition is granted, so a refusal (or a slow answer) would leave the counter
    // stuck at zero; without it the motion sensor below counts instead.
    DisposableEffect(hasPermission) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepDetector = if (hasPermission) sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) else null
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var lastStepTime = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || solved) return
                val now = System.currentTimeMillis()

                if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                    if (now - lastStepTime > 250) {
                        lastStepTime = now
                        currentSteps++
                    }
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    // Fallback accelerometer step peak detection for devices/emulators lacking STEP_DETECTOR
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val mag = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                    if (mag > 12.5f && now - lastStepTime > 350) {
                        lastStepTime = now
                        currentSteps++
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (stepDetector != null) {
            sensorManager.registerListener(listener, stepDetector, SensorManager.SENSOR_DELAY_UI)
        } else if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    LaunchedEffect(currentSteps) {
        if (currentSteps >= targetSteps && !solved) {
            solved = true
            delay(400)
            onSolved()
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (currentSteps.toFloat() / targetSteps.toFloat()).coerceIn(0f, 1f),
        label = "stepProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.steps_title, targetSteps),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.steps_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(40.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 14.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "$currentSteps / $targetSteps",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        if (solved) {
            Text(
                text = stringResource(R.string.steps_completed),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else if (!hasPermission) {
            Text(
                text = stringResource(R.string.steps_permission_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }) {
                Text(stringResource(R.string.camera_grant))
            }
        }
    }
}
