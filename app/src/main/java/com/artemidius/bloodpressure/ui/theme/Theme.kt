package com.artemidius.bloodpressure.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFADC7FF), onPrimary = Color(0xFF152E58),
    primaryContainer = Color(0xFF293F62), onPrimaryContainer = Color(0xFFD9E6FF),
    inversePrimary = Color(0xFF405F91),
    secondary = Color(0xFFBAC9DD), onSecondary = Color(0xFF233244),
    secondaryContainer = Color(0xFF354458), onSecondaryContainer = Color(0xFFDDE8F6),
    tertiary = Color(0xFF91D5C1), onTertiary = Color(0xFF00382D),
    tertiaryContainer = Color(0xFF1F5144), onTertiaryContainer = Color(0xFFB8F2DD),
    background = Color(0xFF14181F), onBackground = Color(0xFFEDF2F8),
    surface = Color(0xFF14181F), onSurface = Color(0xFFEDF2F8),
    surfaceVariant = Color(0xFF35404E), onSurfaceVariant = Color(0xFFBEC9D8),
    surfaceDim = Color(0xFF14181F), surfaceBright = Color(0xFF35404C),
    surfaceContainerLowest = Color(0xFF0E1218), surfaceContainerLow = Color(0xFF1B222C),
    surfaceContainer = Color(0xFF202933), surfaceContainerHigh = Color(0xFF283340),
    surfaceContainerHighest = Color(0xFF323E4C),
    inverseSurface = Color(0xFFE1E8F1), inverseOnSurface = Color(0xFF29323E),
    outline = Color(0xFF8898AC), outlineVariant = Color(0xFF65758A),
    surfaceTint = Color(0xFFADC7FF)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun BloodPressureTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Retain wallpaper colors in light mode; dark mode uses a consistent contrast palette.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(LocalContext.current)
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            view.context.findActivity()?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        Surface(Modifier.fillMaxSize(), color = colorScheme.background, content = content)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
