package com.example.alarmboss.ui.ringing.tasks

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import com.example.alarmboss.R
import com.example.alarmboss.ui.ringing.CameraPermissionRequired
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Scans ANY real-world barcode/QR code to dismiss -- a cereal box, a book, a package, a
 * water bottle, anything with a barcode on it. The point isn't a specific code, it's
 * forcing the user to physically get up and find something with a barcode on it.
 */
@Composable
fun BarcodeTaskScreen(onScanned: () -> Unit) {
    val context = LocalContext.current
    val contentStore = remember { com.example.alarmboss.data.ContentStore(context) }
    val registeredBarcode by contentStore.registeredBarcode.collectAsState(initial = null)
    var lastSeen by remember { mutableStateOf("") }
    var mismatchError by remember { mutableStateOf(false) }
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

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        val hasRegistered = !registeredBarcode.isNullOrBlank()
        Text(
            stringResource(if (hasRegistered) R.string.barcode_title_registered else R.string.barcode_title),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (hasRegistered) stringResource(R.string.barcode_custom_hint, registeredBarcode.orEmpty())
            else stringResource(R.string.barcode_hint),
            style = MaterialTheme.typography.bodySmall
        )
        if (mismatchError) {
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.barcode_mismatch),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(12.dp))
        if (hasPermission) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                CameraPreviewWithBarcodeScan(
                    onBarcodeDetected = { value ->
                        lastSeen = value
                        if (hasRegistered) {
                            if (value.trim().equals(registeredBarcode?.trim(), ignoreCase = true)) {
                                mismatchError = false
                                onScanned()
                            } else {
                                mismatchError = true
                            }
                        } else {
                            onScanned()
                        }
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            if (lastSeen.isNotBlank()) {
                Text(stringResource(R.string.barcode_last_scanned, lastSeen), style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CameraPermissionRequired(
                    message = stringResource(R.string.barcode_camera_required),
                    permanentlyDenied = permanentlyDenied,
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreviewWithBarcodeScan(onBarcodeDetected: (String) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanner = remember { BarcodeScanning.getClient() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            scanner.close()
        }
    }

    // Prevents onBarcodeDetected firing repeatedly per frame once a code is found --
    // the caller's onScanned() will navigate away, but frames in flight could still land.
    var handled by remember { mutableStateOf(false) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null && !handled) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val value = barcodes.firstOrNull()?.rawValue
                                if (value != null && !handled) {
                                    handled = true
                                    onBarcodeDetected(value)
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                    )
                } catch (_: Exception) { }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}