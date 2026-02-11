package com.artemidius.bloodpressure.compose.extentions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

/**
 * Converts a pixel (px) value to density-independent pixels (dp) based on the current screen density.
 *
 * @param px The value in pixels to be converted.
 * @return The converted value in [androidx.compose.ui.unit.Dp].
 */
@Composable
fun Int.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp()}