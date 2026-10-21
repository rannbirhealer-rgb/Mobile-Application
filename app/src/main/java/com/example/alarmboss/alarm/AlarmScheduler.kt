package com.example.alarmboss.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.alarmboss.data.Alarm
import com.example.alarmboss.data.specificLocalDate
import com.example.alarmboss.engine.AlarmTimeCalculator
import com.example.alarmboss.util.HolidaySettings
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val holidaySettings = HolidaySettings(context)

    fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled) return
        // A pinned-date alarm whose moment has passed has nothing left to ring for.
        val triggerAtMillis = nextTriggerTime(alarm) ?: return
        val pendingIntent = pendingIntentFor(alarm.id)

        setAlarm(triggerAtMillis, pendingIntent, showInSystemClock = true)
    }

    /** Used for snooze: re-fires this same alarm id after N minutes, ignoring repeatDays. */
    fun scheduleOneOffInMinutes(alarm: Alarm, minutes: Int) {
        val triggerAtMillis = System.currentTimeMillis() + minutes * 60_000L
        val pendingIntent = pendingIntentFor(alarm.id)
        setAlarm(triggerAtMillis, pendingIntent, showInSystemClock = false)
    }

    /** Rings this alarm again in [minutes] as a [FollowUp], without touching the alarm's own schedule. */
    fun scheduleFollowUp(alarmId: Long, kind: FollowUp, minutes: Int) {
        val triggerAtMillis = System.currentTimeMillis() + minutes * 60_000L
        val pendingIntent = pendingIntentFor(alarmId, kind)
        setAlarm(triggerAtMillis, pendingIntent, showInSystemClock = false)
    }

    fun cancelFollowUp(alarmId: Long, kind: FollowUp) {
        alarmManager.cancel(pendingIntentFor(alarmId, kind))
    }

    /** Cancels the alarm and anything queued behind it, so deleting an alarm can't leave a ghost ring. */
    fun cancel(alarmId: Long) {
        alarmManager.cancel(pendingIntentFor(alarmId))
        FollowUp.values().forEach { cancelFollowUp(alarmId, it) }
    }

    /**
     * Registers the wake-up as exactly as the system allows. Exact scheduling can be revoked at any
     * moment from system settings, even between the check and the call, so a refusal falls back to an
     * inexact alarm (late, but it rings) instead of crashing and leaving nothing scheduled.
     */
    private fun setAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent, showInSystemClock: Boolean) {
        if (alarmManager.canScheduleExactAlarms()) {
            try {
                if (showInSystemClock) {
                    alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent), pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
                return
            } catch (_: SecurityException) {
                // fall through to the inexact alarm
            }
        }
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    private fun pendingIntentFor(alarmId: Long, followUp: FollowUp? = null): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            followUp?.let { putExtra(AlarmReceiver.EXTRA_FOLLOW_UP, it.name) }
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId.toInt() + (followUp?.requestOffset ?: 0),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** The next epoch-millis trigger time, honoring repeatDays if set. */
    private fun nextTriggerTime(alarm: Alarm): Long? =
        AlarmTimeCalculator.nextTrigger(
            alarm.hour, alarm.minute, alarm.repeatDays, LocalDateTime.now(), alarm.specificLocalDate(),
            holidaySettings.activeSkipDates
        )?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
}
