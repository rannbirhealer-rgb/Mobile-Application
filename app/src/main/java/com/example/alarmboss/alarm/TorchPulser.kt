package com.example.alarmboss.alarm

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Blinks the camera light so an alarm can be seen as well as heard or felt. Every torch call is
 * guarded: another app (or this one, during a camera task) may hold the camera, in which case the
 * blink simply skips a beat instead of crashing the alarm.
 */
class TorchPulser(context: Context, private val scope: CoroutineScope) {
    private val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var job: Job? = null

    private val torchCameraId: String? = runCatching {
        manager.cameraIdList.firstOrNull { id ->
            val characteristics = manager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    }.getOrNull()

    fun start() {
        val id = torchCameraId ?: return
        job?.cancel()
        job = scope.launch {
            var on = true
            while (isActive) {
                runCatching { manager.setTorchMode(id, on) }
                on = !on
                delay(BLINK_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        torchCameraId?.let { runCatching { manager.setTorchMode(it, false) } }
    }

    private companion object {
        const val BLINK_MS = 500L
    }
}
