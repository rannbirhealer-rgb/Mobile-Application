package com.example.alarmboss.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.alarmboss.data.Alarm
import com.example.alarmboss.engine.AlarmTimeCalculator
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled) return
        val triggerAtMillis = nextTriggerTime(alarm)
        val pendingIntent = pendingIntentFor(alarm.id)

        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent),
                pendingIntent
            )
        } else {

            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    /** Used for snooze: re-fires this same alarm id after N minutes, ignoring repeatDays. */
    fun scheduleOneOffInMinutes(alarm: Alarm, minutes: Int) {
        val triggerAtMillis = System.currentTimeMillis() + minutes * 60_000L
        val pendingIntent = pendingIntentFor(alarm.id)
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancel(alarmId: Long) {
        alarmManager.cancel(pendingIntentFor(alarmId))
    }

    private fun pendingIntentFor(alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** The next epoch-millis trigger time, honoring repeatDays if set. */
    private fun nextTriggerTime(alarm: Alarm): Long =
        AlarmTimeCalculator.nextTrigger(alarm.hour, alarm.minute, alarm.repeatDays, LocalDateTime.now())
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
