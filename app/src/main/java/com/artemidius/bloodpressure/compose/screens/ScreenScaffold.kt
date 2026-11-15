package com.artemidius.bloodpressure.compose.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Backgrounds extend to the window edges; content owns and consumes its safe area once. */
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    contentWindowInsets: WindowInsets = WindowInsets.safeDrawing,
    content: @Composable BoxScope.() -> Unit
) {
    Scaffold(modifier = modifier, topBar = topBar, contentWindowInsets = contentWindowInsets) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding), content = content)
    }
}
