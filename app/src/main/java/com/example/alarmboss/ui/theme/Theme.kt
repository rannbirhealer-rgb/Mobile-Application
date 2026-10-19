package com.example.alarmboss.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyberViolet,
    onPrimary = Color.White,
    primaryContainer = CyberVioletContainer,
    onPrimaryContainer = OnCyberVioletContainer,

    secondary = LaserCyan,
    onSecondary = Color(0xFF00363F),
    secondaryContainer = LaserCyanContainer,
    onSecondaryContainer = OnLaserCyanContainer,

    tertiary = ElectricAmber,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = ElectricAmberContainer,
    onTertiaryContainer = OnElectricAmberContainer,

    error = CrimsonRage,
    onError = Color.White,
    errorContainer = CrimsonRageContainer,
    onErrorContainer = OnCrimsonRageContainer,

    background = DarkBackground,
    onBackground = Color(0xFFF1F5F9),

    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceTint = CyberViolet,

    outline = DarkBorder,
    outlineVariant = DarkDivider
)

private val LightColorScheme = lightColorScheme(
    primary = RoyalIndigoDark,
    onPrimary = Color.White,
    primaryContainer = RoyalIndigoContainer,
    onPrimaryContainer = OnRoyalIndigoContainer,

    secondary = LaserCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),

    tertiary = ElectricAmberDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),

    error = CrimsonRage,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF9F1239),

    background = LightBackground,
    onBackground = Color(0xFF0F172A),

    surface = LightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = Color(0xFF64748B),
    surfaceTint = RoyalIndigo,

    outline = LightBorder,
    outlineVariant = LightDivider
)

@Composable
fun AlarmBossTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamicColor false by default to ensure the signature premium RPG aesthetic
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}