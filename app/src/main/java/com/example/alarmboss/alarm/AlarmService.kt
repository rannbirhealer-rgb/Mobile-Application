package com.example.alarmboss.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.alarmboss.AlarmBossApp
import com.example.alarmboss.R
import com.example.alarmboss.ui.ringing.AlarmRingingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that owns the alarm's audio/vibration for as long as it's ringing.
 * The actual dismiss/task UI lives in AlarmRingingActivity; this service just keeps the
 * sound going reliably (Activities alone can get killed) and stops on ACTION_STOP.
 */
class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var volumeJob: Job? = null
    private var flashlightJob: Job? = null
    private var cameraManager: CameraManager? = null
    private var torchCameraId: String? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopRinging()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L) ?: -1L
                if (alarmId == -1L) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startRinging(alarmId)
            }
        }
        return START_STICKY
    }

    private fun startRinging(alarmId: Long) {
        val app = application as AlarmBossApp
        scope.launch {
            val alarm = app.repository.getAlarm(alarmId)
            if (alarm == null) {
                stopSelf()
                return@launch
            }

            val fullScreenIntent = Intent(this@AlarmService, AlarmRingingActivity::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmRingingActivity.EXTRA_DYNAMIC_BRIGHTNESS, alarm.dynamicBrightness)
                putExtra(AlarmRingingActivity.EXTRA_SUNRISE_MODE, alarm.sunriseMode)
                putExtra(AlarmRingingActivity.EXTRA_MORNING_BRIEFING, alarm.morningBriefing)
                putExtra(AlarmRingingActivity.EXTRA_WAKE_CHECK, alarm.wakeCheck)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                this@AlarmService, alarmId.toInt(), fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification: Notification = NotificationCompat.Builder(this@AlarmService, AlarmBossApp.CHANNEL_ALARM)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(alarm.label.ifBlank { getString(R.string.ringing_default_label) })
                .setContentText(getString(R.string.notification_tap_to_open))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .setOngoing(true)
                .build()

            startForeground(NOTIFICATION_ID, notification)
            startActivity(fullScreenIntent)

            playSound(alarm.soundUri, alarm.gradualVolume)
            if (alarm.vibrate) startVibration()
            if (alarm.flashLightBlink) startFlashlightBlink()
        }
    }

    private fun startFlashlightBlink() {
        stopFlashlightBlink()
        val manager = getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        cameraManager = manager
        try {
            val cameraIds = manager.cameraIdList
            for (id in cameraIds) {
                val chars = manager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    torchCameraId = id
                    break
                }
            }
            if (torchCameraId == null) {
                for (id in cameraIds) {
                    val chars = manager.getCameraCharacteristics(id)
                    if (chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                        torchCameraId = id
                        break
                    }
                }
            }
        } catch (_: Exception) {
            return
        }

        val targetId = torchCameraId ?: return

        flashlightJob = scope.launch(Dispatchers.Default) {
            var torchOn = false
            try {
                while (isActive) {
                    torchOn = !torchOn
                    try {
                        manager.setTorchMode(targetId, torchOn)
                    } catch (_: Exception) {
                        // In case camera is temporarily occupied by an exercise or barcode scanning task
                    }
                    delay(400)
                }
            } finally {
                try {
                    manager.setTorchMode(targetId, false)
                } catch (_: Exception) { }
            }
        }
    }

    private fun stopFlashlightBlink() {
        flashlightJob?.cancel()
        flashlightJob = null
        val targetId = torchCameraId
        if (targetId != null) {
            try {
                cameraManager?.setTorchMode(targetId, false)
            } catch (_: Exception) { }
        }
    }

    private fun playSound(soundUri: String?, gradualVolume: Boolean) {
        stopMediaPlayer()
        try {
            val uri = soundUri?.let { Uri.parse(it) }
                ?: android.media.RingtoneManager.getActualDefaultRingtoneUri(
                    this, android.media.RingtoneManager.TYPE_ALARM
                )
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmService, uri!!)
                isLooping = true
                prepare()
                if (gradualVolume) {
                    setVolume(0.05f, 0.05f)
                } else {
                    setVolume(1.0f, 1.0f)
                }
                start()
            }

            if (gradualVolume) {
                volumeJob = scope.launch(Dispatchers.Default) {
                    val steps = 25
                    for (i in 1..steps) {
                        delay(1000)
                        val vol = (0.05f + (0.95f * (i.toFloat() / steps))).coerceIn(0.05f, 1.0f)
                        try {
                            mediaPlayer?.setVolume(vol, vol)
                        } catch (_: Exception) { }
                    }
                }
            }
        } catch (e: Exception) {
            // If the custom sound URI can no longer be resolved (e.g. permission revoked),
            // fall back silently to vibration only so the alarm still wakes the user.
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 800, 400)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    fun stopRinging() {
        stopMediaPlayer()
        vibrator?.cancel()
        stopFlashlightBlink()
    }

    private fun stopMediaPlayer() {
        volumeJob?.cancel()
        volumeJob = null
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.alarmboss.ACTION_START"
        const val ACTION_STOP = "com.example.alarmboss.ACTION_STOP"
        const val NOTIFICATION_ID = 42

        fun stop(context: Context) {
            val intent = Intent(context, AlarmService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }
}
