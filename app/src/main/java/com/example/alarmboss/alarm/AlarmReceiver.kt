package com.example.alarmboss.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.alarmboss.AlarmBossApp
import com.example.alarmboss.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) return

        when (intent.action) {
            ACTION_CONFIRM_AWAKE -> {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(WAKE_CHECK_NOTIFICATION_ID)
                AlarmScheduler(context).cancelWakeCheck(alarmId)
            }
            ACTION_WAKE_CHECK -> {
                showWakeCheckNotification(context, alarmId)
            }
            else -> {
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_START
                    putExtra(EXTRA_ALARM_ID, alarmId)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }

    private fun showWakeCheckNotification(context: Context, alarmId: Long) {
        val confirmIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_CONFIRM_AWAKE
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val confirmPending = PendingIntent.getBroadcast(
            context,
            (alarmId + 200000).toInt(),
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, AlarmBossApp.CHANNEL_ALARM)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.wake_check_title))
            .setContentText(context.getString(R.string.wake_check_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(confirmPending)
            .setAutoCancel(true)
            .addAction(
                R.mipmap.ic_launcher,
                context.getString(R.string.wake_check_im_awake),
                confirmPending
            )
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(WAKE_CHECK_NOTIFICATION_ID, notification)
    }

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.example.alarmboss.ACTION_ALARM_TRIGGER"
        const val ACTION_WAKE_CHECK = "com.example.alarmboss.ACTION_WAKE_CHECK"
        const val ACTION_CONFIRM_AWAKE = "com.example.alarmboss.ACTION_CONFIRM_AWAKE"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val WAKE_CHECK_NOTIFICATION_ID = 999
    }
}
