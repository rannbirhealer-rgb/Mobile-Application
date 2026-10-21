package com.example.alarmboss.ui.theme

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.alarmboss.util.ThemeMode
import com.example.alarmboss.util.ThemeSettings

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/**
 * The stored theme choice, kept current: the Settings screen writes it from another place, so this
 * listens for changes and every screen using [AlarmBossTheme] switches the moment it is changed.
 */
@Composable
fun rememberThemeMode(): ThemeMode {
    val context = LocalContext.current
    val settings = remember(context) { ThemeSettings(context) }
    var mode by remember { mutableStateOf(settings.mode) }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(ThemeSettings.PREFS_NAME, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == ThemeSettings.KEY_MODE) mode = settings.mode
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return mode
}

@Composable
fun AlarmBossTheme(
    themeMode: ThemeMode = rememberThemeMode(),
    content: @Composable () -> Unit
) {
    val darkTheme = themeMode.isDark(isSystemInDarkTheme())

    // The status and navigation bar icons must be dark on a light screen and light on a dark one. The
    // system only knows its own mode, so when the app overrides it the bars are told explicitly.
    val activity = LocalContext.current.findComponentActivity()
    DisposableEffect(activity, darkTheme) {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { darkTheme },
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { darkTheme }
        )
        onDispose { }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
