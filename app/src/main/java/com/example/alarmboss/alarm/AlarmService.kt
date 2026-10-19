package com.example.alarmboss.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.alarmboss.AlarmBossApp
import com.example.alarmboss.R
import com.example.alarmboss.engine.AlarmFeedbackPlanner
import com.example.alarmboss.engine.AlarmTimeCalculator
import com.example.alarmboss.engine.VolumeRamp
import com.example.alarmboss.ui.ringing.AlarmRingingActivity
import com.example.alarmboss.util.AccessibilitySettings
import com.example.alarmboss.util.FollowUpSettings
import com.example.alarmboss.util.SoundSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service that owns the alarm's audio/vibration for as long as it's ringing.
 * The actual dismiss/task UI lives in AlarmRingingActivity; this service just keeps the
 * sound going reliably (Activities alone can get killed) and stops on ACTION_STOP.
 */
class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private var rampJob: Job? = null
    private val torch by lazy { TorchPulser(this, scope) }

    /** Alarms whose primary ring is under way; each may have queued a backup ring behind it. */
    private val primaryRings = mutableSetOf<Long>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                // The ring was ended on purpose (dismissed, snoozed or stopped). Two alarms can share a
                // minute but only one screen is shown, so clear the backup of every alarm that rang.
                cancelPendingBackups()
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
                val followUp = intent?.getStringExtra(AlarmReceiver.EXTRA_FOLLOW_UP)
                    ?.let { name -> FollowUp.values().firstOrNull { it.name == name } }
                startRinging(alarmId, followUp)
            }
        }
        return START_STICKY
    }

    private fun startRinging(alarmId: Long, followUp: FollowUp?) {
        val app = application as AlarmBossApp
        // startForegroundService() obliges us to call startForeground() within seconds or the system
        // kills the app, so promote before the database lookup, and even if the alarm is gone.
        startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.ringing_default_label), null))
        scope.launch {
            val alarm = app.repository.getAlarm(alarmId)
            if (alarm == null) {
                // Deleted between scheduling and ringing: nothing to ring for.
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }

            // A follow-up ring is an extra on top of an alarm whose own schedule was already settled
            // when it first rang, so only a primary ring decides what comes next.
            if (followUp == null) {
                primaryRings += alarm.id
                // Decide before anything else, so it still happens if the screen or process is killed
                // mid-ring: a repeating alarm needs its next occurrence lined up (nothing else schedules
                // it after it fires), and a one-off has done its job and must switch off.
                val scheduler = AlarmScheduler(this@AlarmService)
                when (AlarmTimeCalculator.afterRing(alarm.repeatDays)) {
                    AlarmTimeCalculator.AfterRing.RESCHEDULE -> scheduler.schedule(alarm)
                    AlarmTimeCalculator.AfterRing.DISABLE -> {
                        app.repository.setEnabled(alarm.id, false)
                        scheduler.cancel(alarm.id)
                    }
                }

                // Safety net: if this ring isn't completed in time, ring again. Cancelled on completion.
                val backupMinutes = FollowUpSettings(this@AlarmService).backupMinutes
                if (backupMinutes > 0) scheduler.scheduleFollowUp(alarm.id, FollowUp.BACKUP, backupMinutes)
            }

            val fullScreenIntent = Intent(this@AlarmService, AlarmRingingActivity::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmRingingActivity.EXTRA_DYNAMIC_BRIGHTNESS, alarm.dynamicBrightness)
                putExtra(AlarmRingingActivity.EXTRA_SUNRISE_MODE, alarm.sunriseMode)
                putExtra(AlarmRingingActivity.EXTRA_MORNING_BRIEFING, alarm.morningBriefing)
                followUp?.let { putExtra(AlarmReceiver.EXTRA_FOLLOW_UP, it.name) }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                this@AlarmService, alarmId.toInt(), fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = if (followUp == FollowUp.WAKE_CHECK) getString(R.string.wake_check_title)
            else alarm.label.ifBlank { getString(R.string.ringing_default_label) }
            val notification = buildNotification(title, fullScreenPendingIntent)

            startForeground(NOTIFICATION_ID, notification)
            startActivity(fullScreenIntent)

            val accessibility = AccessibilitySettings(this@AlarmService)
            val feedback = AlarmFeedbackPlanner.plan(alarm.vibrate, accessibility.silentMode)
            val soundPlayed = feedback.sound && playSound(alarm.soundUri, alarm.gradualVolume)
            // If a sound was wanted but none could play, vibrate whatever the vibration setting says.
            if (feedback.vibrate || (feedback.sound && !soundPlayed)) startVibration()
            if (accessibility.flash || alarm.flashLightBlink) torch.start()
        }
    }

    private fun buildNotification(title: String, fullScreen: PendingIntent?): Notification =
        NotificationCompat.Builder(this, AlarmBossApp.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(getString(R.string.notification_tap_to_open))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .apply {
                if (fullScreen != null) {
                    setFullScreenIntent(fullScreen, true)
                    setContentIntent(fullScreen)
                }
            }
            .build()

    /** Starts the alarm sound; returns false if nothing at all could be played. */
    private fun playSound(soundUri: String?, alarmFadeIn: Boolean): Boolean {
        stopMediaPlayer()
        val sound = SoundSettings(this)
        raiseSystemVolumeIfEnabled(sound)

        val gradual = sound.gradual || alarmFadeIn
        val target = sound.loudnessPercent / 100f
        val startVolume = if (gradual) VolumeRamp.volumeAt(0, VolumeRamp.RAMP_MS, target) else target

        // The chosen sound can vanish (file deleted, permission revoked, SD card removed). An alarm
        // that plays nothing is worse than one with the wrong tune, so fall back through the system
        // sounds before giving up.
        val candidates = buildList {
            soundUri?.let { add(Uri.parse(it)) }
            for (type in listOf(
                RingtoneManager.TYPE_ALARM, RingtoneManager.TYPE_RINGTONE, RingtoneManager.TYPE_NOTIFICATION
            )) {
                runCatching { RingtoneManager.getActualDefaultRingtoneUri(this@AlarmService, type) }
                    .getOrNull()?.let { add(it) }
            }
        }
        for (uri in candidates) {
            val player = MediaPlayer()
            try {
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                player.setDataSource(this, uri)
                player.isLooping = true
                player.setVolume(startVolume, startVolume)
                player.prepare()
                player.start()
                mediaPlayer = player
                if (gradual) startVolumeRamp(target)
                return true
            } catch (e: Exception) {
                runCatching { player.release() }
            }
        }
        return false
    }

    /** Fades the player up to [target] over [VolumeRamp.RAMP_MS], ending the loop once it gets there. */
    private fun startVolumeRamp(target: Float) {
        rampJob?.cancel()
        rampJob = scope.launch {
            val started = SystemClock.elapsedRealtime()
            while (true) {
                val volume = VolumeRamp.volumeAt(SystemClock.elapsedRealtime() - started, VolumeRamp.RAMP_MS, target)
                val player = mediaPlayer ?: break
                runCatching { player.setVolume(volume, volume) }
                if (volume >= target) break
                delay(RAMP_STEP_MS)
            }
        }
    }

    /**
     * A phone with its alarm volume turned down or muted is the commonest reason an alarm goes
     * unheard. Lift the system alarm stream for the duration of the ring and remember the original
     * (persistently) so it can be put back, even if this service is killed before it gets the chance.
     */
    private fun raiseSystemVolumeIfEnabled(sound: SoundSettings) {
        if (!sound.raiseSystemVolume) return
        runCatching {
            val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (sound.rememberedSystemVolume == SoundSettings.NONE) {
                sound.rememberedSystemVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
            }
            audio.setStreamVolume(AudioManager.STREAM_ALARM, audio.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
        }
    }

    private fun restoreSystemVolume() {
        val sound = SoundSettings(this)
        val remembered = sound.rememberedSystemVolume
        if (remembered == SoundSettings.NONE) return
        runCatching {
            (getSystemService(Context.AUDIO_SERVICE) as AudioManager)
                .setStreamVolume(AudioManager.STREAM_ALARM, remembered, 0)
        }
        sound.rememberedSystemVolume = SoundSettings.NONE
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

    private fun cancelPendingBackups() {
        val scheduler = AlarmScheduler(this)
        primaryRings.forEach { scheduler.cancelFollowUp(it, FollowUp.BACKUP) }
        primaryRings.clear()
    }

    fun stopRinging() {
        rampJob?.cancel()
        torch.stop()
        stopMediaPlayer()
        vibrator?.cancel()
        restoreSystemVolume()
    }

    private fun stopMediaPlayer() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    override fun onDestroy() {
        stopRinging()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.alarmboss.ACTION_START"
        const val ACTION_STOP = "com.example.alarmboss.ACTION_STOP"
        const val NOTIFICATION_ID = 42
        private const val RAMP_STEP_MS = 250L

        fun stop(context: Context) {
            val intent = Intent(context, AlarmService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }
}
