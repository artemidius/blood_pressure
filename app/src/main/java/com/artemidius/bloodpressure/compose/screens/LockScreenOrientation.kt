package com.artemidius.bloodpressure.compose.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext


/**
 * Locks the screen orientation of the current activity to the specified orientation.
 *
 * This composable uses [DisposableEffect] to manage the lifecycle of the screen orientation lock.
 * It sets the requested orientation when the composable enters the composition and restores
 * the original orientation when the composable leaves the composition or the orientation parameter
 * changes.
 *
 * @param orientation The desired screen orientation, using constants from [ActivityInfo].
 *     Common values include:
 *     - [ActivityInfo.SCREEN_ORIENTATION_PORTRAIT]: Portrait orientation.
 *     - [ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE]: Landscape orientation.
 */
@Composable
fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current
    DisposableEffect(orientation) {
        val activity = context.findActivity() ?: return@DisposableEffect onDispose {}
        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = orientation
        onDispose {
            // restore original orientation when view disappears
            activity.requestedOrientation = originalOrientation
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}