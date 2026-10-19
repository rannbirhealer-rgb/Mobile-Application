package com.example.alarmboss.ui.ringing

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.alarmboss.R
import com.example.alarmboss.data.ExerciseType
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

/**
 * Camera-based exercise check for Strict mode.
 *
 * Honest limitation: this counts reasonably real reps from body landmark positions
 * (ML Kit Pose Detection) -- squats via hip-knee-ankle vertical distance crossing a
 * threshold, jumping jacks via wrist-vs-shoulder height alternating -- but it is NOT a
 * form-correctness checker, and lighting/camera angle affect reliability. Every landmark
 * used is checked against ML Kit's own confidence score (inFrameLikelihood) before it's
 * trusted; low-confidence detections (e.g. an empty frame, a shadow, nobody actually in
 * view) are treated as "not visible" rather than fed into the rep counter, which is what
 * previously let reps get counted with no one there.
 */
@Composable
fun StrictExerciseScreen(
    initialExercise: ExerciseType,
    durationSeconds: Int,
    onComplete: () -> Unit,
    onUsePuzzleInstead: () -> Unit
) {
    val context = LocalContext.current
    var exercise by remember { mutableStateOf(initialExercise) }
    var remaining by remember { mutableIntStateOf(durationSeconds) }
    var reps by remember { mutableIntStateOf(0) }
    var personVisible by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permanentlyDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        val activity = context as? Activity
        if (!granted && activity != null) {
            permanentlyDenied = !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
        }
    }
    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    // Countdown only ticks down while the camera can see the person moving; this is what
    // prevents someone from propping the phone up and going back to sleep.
    LaunchedEffect(personVisible, remaining) {
        if (remaining <= 0) { onComplete(); return@LaunchedEffect }
        delay(1000)
        if (personVisible) remaining -= 1
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(stringResource(R.string.strict_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(
                if (exercise == ExerciseType.SQUATS) R.string.strict_exercise_squats
                else R.string.strict_exercise_jumping_jacks
            ),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(stringResource(R.string.strict_time_remaining, remaining / 60, remaining % 60))
        Text(stringResource(R.string.strict_reps_detected, reps), style = MaterialTheme.typography.bodySmall)
        if (!personVisible) {
            Text(
                stringResource(R.string.strict_not_visible),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(12.dp))
        if (hasPermission) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                PoseCameraPreview(
                    exercise = exercise,
                    onRep = { reps++ },
                    onVisibilityChanged = { personVisible = it }
                )
            }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CameraPermissionRequired(
                    message = stringResource(R.string.strict_camera_required),
                    permanentlyDenied = permanentlyDenied,
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = {
            exercise = if (exercise == ExerciseType.SQUATS) ExerciseType.JUMPING_JACKS else ExerciseType.SQUATS
        }) { Text(stringResource(R.string.strict_change_exercise)) }
        // Not everyone can squat or jump (injury, mobility, a small space): offer a text-based
        // route that works with a screen reader and needs no camera or movement.
        TextButton(onClick = onUsePuzzleInstead) { Text(stringResource(R.string.strict_puzzle_instead)) }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun PoseCameraPreview(
    exercise: ExerciseType,
    onRep: () -> Unit,
    onVisibilityChanged: (Boolean) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val detector = remember {
        PoseDetection.getClient(
            AccuratePoseDetectorOptions.Builder()
                .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
                .build()
        )
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val repCounter = remember(exercise) { RepCounter(exercise) }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            detector.close()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        detector.process(image)
                            .addOnSuccessListener { pose ->
                                val confidentlyVisible = repCounter.isConfidentlyTracked(pose)
                                onVisibilityChanged(confidentlyVisible)
                                if (confidentlyVisible && repCounter.update(pose)) onRep()
                                if (!confidentlyVisible) repCounter.resetState()
                            }
                            .addOnFailureListener {
                                onVisibilityChanged(false)
                                repCounter.resetState()
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis
                    )
                } catch (_: Exception) { }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

/**
 * State-machine rep counter driven by landmark y-coordinates (image space, so smaller y =
 * higher up). Every landmark it reads is gated behind a minimum confidence score --
 * ML Kit still returns a Pose object with landmark positions even when nobody is in frame
 * (often stale/garbage coordinates with low confidence), so trusting positions blindly is
 * what previously caused reps to be counted with no one there.
 */
private class RepCounter(private val exercise: ExerciseType) {
    private var isDown = false

    companion object {
        private const val MIN_LIKELIHOOD = 0.6f
    }

    /** True only if every landmark this exercise needs was detected with real confidence. */
    fun isConfidentlyTracked(pose: Pose): Boolean {
        val needed = when (exercise) {
            ExerciseType.SQUATS -> listOf(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)
            ExerciseType.JUMPING_JACKS -> listOf(
                PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST,
                PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER
            )
        }
        return needed.all { id ->
            val landmark = pose.getPoseLandmark(id)
            landmark != null && landmark.inFrameLikelihood >= MIN_LIKELIHOOD
        }
    }

    /** Call when tracking is lost, so a rep isn't half-counted across a tracking gap. */
    fun resetState() {
        isDown = false
    }

    fun update(pose: Pose): Boolean {
        return when (exercise) {
            ExerciseType.SQUATS -> updateSquat(pose)
            ExerciseType.JUMPING_JACKS -> updateJumpingJack(pose)
        }
    }

    private fun updateSquat(pose: Pose): Boolean {
        val hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: return false
        val knee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE) ?: return false
        val ankle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE) ?: return false

        // Ratio of hip-to-knee vs knee-to-ankle vertical distance shrinks a lot when squatting.
        val hipKnee = kotlin.math.abs(hip.position.y - knee.position.y)
        val kneeAnkle = kotlin.math.abs(knee.position.y - ankle.position.y)
        if (kneeAnkle < 1f) return false
        val ratio = hipKnee / kneeAnkle

        var repCompleted = false
        if (!isDown && ratio < 0.55f) {
            isDown = true
        } else if (isDown && ratio > 0.85f) {
            isDown = false
            repCompleted = true
        }
        return repCompleted
    }

    private fun updateJumpingJack(pose: Pose): Boolean {
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST) ?: return false
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST) ?: return false
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: return false
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER) ?: return false

        val armsUp = leftWrist.position.y < leftShoulder.position.y &&
                rightWrist.position.y < rightShoulder.position.y

        var repCompleted = false
        if (!isDown && armsUp) {
            isDown = true // "down" is reused here to mean "arms currently up"
        } else if (isDown && !armsUp) {
            isDown = false
            repCompleted = true
        }
        return repCompleted
    }
}