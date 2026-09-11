package com.raebae.ytdl.ui.theme

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
import com.raebae.ytdl.data.SettingsState
import com.raebae.ytdl.data.ThemeMode

private val LightScheme = lightColorScheme(
    primary = Color(0xFF3560E0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF00174B),
    secondary = Color(0xFF595E71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDE1F9),
    onSecondaryContainer = Color(0xFF161B2C),
    tertiary = Color(0xFF75546F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD7F4),
    onTertiaryContainer = Color(0xFF2C1229),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1A1B21),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1A1B21),
    surfaceVariant = Color(0xFFE2E1EC),
    onSurfaceVariant = Color(0xFF45464F),
    outline = Color(0xFF767680)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFB5C3FF),
    onPrimary = Color(0xFF0B2B78),
    primaryContainer = Color(0xFF1B4592),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFFC1C5DD),
    onSecondary = Color(0xFF2B3042),
    secondaryContainer = Color(0xFF414659),
    onSecondaryContainer = Color(0xFFDDE1F9),
    tertiary = Color(0xFFE3BAD9),
    onTertiary = Color(0xFF432740),
    tertiaryContainer = Color(0xFF5B3D57),
    onTertiaryContainer = Color(0xFFFFD7F4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF121318),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF45464F),
    onSurfaceVariant = Color(0xFFC6C5D0),
    outline = Color(0xFF90909A)
)

@Composable
fun YtdlTheme(
    settings: SettingsState,
    content: @Composable () -> Unit
) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = if (settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) DarkScheme else LightScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
