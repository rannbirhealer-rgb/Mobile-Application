package com.example.alarmboss.ui.alarms

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.alarmboss.R

private const val PREFS = "alarm_boss_banners"
private const val KEY_BATTERY_DISMISSED = "battery_banner_dismissed"

/**
 * Many phone makers aggressively stop background apps to save power, which can delay or silently
 * drop an alarm. Offers the system's "don't optimize" exemption, and can be dismissed for good
 * since some users deliberately keep optimization on. Rechecked on resume because the grant
 * happens in a system dialog.
 */
@Composable
fun BatteryOptimizationBanner() {
    val context = LocalContext.current
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    var restricted by remember { mutableStateOf(!powerManager.isIgnoringBatteryOptimizations(context.packageName)) }
    var dismissed by remember { mutableStateOf(prefs.getBoolean(KEY_BATTERY_DISMISSED, false)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                restricted = !powerManager.isIgnoringBatteryOptimizations(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!restricted || dismissed) return

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.battery_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.battery_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { requestExemption(context) }) { Text(stringResource(R.string.battery_allow)) }
                TextButton(onClick = {
                    prefs.edit().putBoolean(KEY_BATTERY_DISMISSED, true).apply()
                    dismissed = true
                }) { Text(stringResource(R.string.battery_not_now)) }
            }
        }
    }
}

private fun requestExemption(context: Context) {
    val direct = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${context.packageName}")
    )
    try {
        context.startActivity(direct)
    } catch (_: ActivityNotFoundException) {
        // Some OEM builds lack the direct dialog; the general list screen still lets the user opt out.
        runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
    }
}
