package com.example.alarmboss.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import com.example.alarmboss.AlarmBossApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reschedules all enabled alarms whenever the system's idea of "when" changes under them: after
 * a reboot (alarms don't survive one) and when the time zone or clock is changed (each alarm is
 * stored as an absolute moment, so without this a 7:00 alarm would ring at the old zone's 7:00).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val pendingResult = goAsync()
        val app = context.applicationContext as AlarmBossApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlarmRescheduler(context, app.repository).rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED
        )
    }
}
