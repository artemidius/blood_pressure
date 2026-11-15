package com.artemidius.bloodpressure.activity

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

internal fun ComponentActivity.configureEdgeToEdge() {
    enableEdgeToEdge()
    // The app draws a themed surface behind both gesture and three-button navigation.
    window.isNavigationBarContrastEnforced = false
}
