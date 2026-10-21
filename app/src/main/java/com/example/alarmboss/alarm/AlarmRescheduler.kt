package com.example.alarmboss.alarm

import android.content.Context
import com.example.alarmboss.data.AlarmRepository

/**
 * Re-registers every enabled alarm with the system. AlarmManager holds each alarm as an absolute
 * moment, so anything that makes that moment stale needs a fresh pass: a reboot (alarms don't
 * survive one), a time-zone or clock change (7:00 must stay 7:00 local), or data that arrived
 * without ever being scheduled (a restored backup). Scheduling is idempotent, so over-running
 * this is harmless.
 */
class AlarmRescheduler(context: Context, private val repository: AlarmRepository) {
    private val scheduler = AlarmScheduler(context)

    suspend fun rescheduleAll() {
        repository.getEnabledAlarms().forEach { scheduler.schedule(it) }
    }
}
