package com.example.alarmboss.ui.alarms

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.alarmboss.R

/**
 * What an alarm needs to appear over the lock screen is a notification that is allowed to post and
 * to take over the screen. Turning either off is easy to do by accident (a swiped permission prompt,
 * a notification clean-up), and the alarm would then play its sound to a screen that never shows
 * the way to dismiss it. Say so, and take the user straight to the switch. Rechecked on resume,
 * since the fix happens in system settings.
 */
@Composable
fun NotificationAccessBanner() {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    var notificationsOn by remember { mutableStateOf(manager.areNotificationsEnabled()) }
    var fullScreenOn by remember { mutableStateOf(manager.canUseFullScreenIntent()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsOn = manager.areNotificationsEnabled()
                fullScreenOn = manager.canUseFullScreenIntent()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (notificationsOn && fullScreenOn) return

    val settings = if (!notificationsOn) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.notification_access_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(
                    if (!notificationsOn) R.string.notification_access_body_off else R.string.notification_access_body_fullscreen
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { runCatching { context.startActivity(settings) } }) {
                Text(stringResource(R.string.notification_access_fix), fontWeight = FontWeight.Bold)
            }
        }
    }
}
