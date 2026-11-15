package com.artemidius.bloodpressure

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.artemidius.bloodpressure.ui.theme.DarkColorScheme
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class DarkThemeContrastTest {
    @Test fun textAndChartColorsRemainReadableOnDarkSurfaces() {
        with(DarkColorScheme) {
            for ((foreground, background) in listOf(
                onBackground to background, onSurface to surfaceContainerHigh,
                onSurfaceVariant to surfaceContainerHigh, onPrimary to primary,
                onPrimaryContainer to primaryContainer, onSecondaryContainer to secondaryContainer,
                onTertiaryContainer to tertiaryContainer, error to surface,
                primary to background, tertiary to background
            )) assertTrue("Text contrast must be at least 4.5:1", contrast(foreground, background) >= 4.5f)
        }
    }

    @Test fun outlinedControlsRemainDistinctFromBackground() {
        with(DarkColorScheme) {
            assertTrue(contrast(outlineVariant, background) >= 3f)
            assertTrue(contrast(outline, surfaceContainerHigh) >= 3f)
        }
    }

    private fun contrast(a: Color, b: Color): Float =
        (max(a.luminance(), b.luminance()) + .05f) / (min(a.luminance(), b.luminance()) + .05f)
}
