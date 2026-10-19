package com.example.alarmboss.alarm

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * DeviceAdminReceiver providing enterprise-grade tamper prevention
 * (disallowing power menu abuse or task-killer uninstalls mid-alarm).
 */
class AlarmDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "AlarmBoss Anti-Tamper Protection Enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "AlarmBoss Anti-Tamper Protection Disabled", Toast.LENGTH_SHORT).show()
    }
}
