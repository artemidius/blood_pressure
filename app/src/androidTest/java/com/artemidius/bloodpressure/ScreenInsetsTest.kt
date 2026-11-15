package com.artemidius.bloodpressure

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.artemidius.bloodpressure.compose.screens.ScreenScaffold
import com.artemidius.bloodpressure.ui.theme.BloodPressureTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ScreenInsetsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cutoutAndNavigationInsetsAreAppliedOnlyOnce() {
        compose.setContent {
            BloodPressureTheme {
                val insets = WindowInsets(left = 24.dp, top = 32.dp, right = 40.dp, bottom = 48.dp)
                ScreenScaffold(Modifier.testTag("window"), contentWindowInsets = insets) {
                    // A nested inset-aware component must not add the same padding again.
                    Box(Modifier.fillMaxSize().windowInsetsPadding(insets).testTag("content"))
                }
            }
        }
        assertContentInsets(top = 32f, bottom = 48f)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test fun appBarOwnsTopInsetAndBodyConsumesItsPadding() {
        compose.setContent {
            BloodPressureTheme {
                val insets = WindowInsets(left = 24.dp, top = 32.dp, right = 40.dp, bottom = 48.dp)
                ScreenScaffold(Modifier.testTag("window"), contentWindowInsets = insets, topBar = {
                    TopAppBar(title = { Text("Blood Pressure") }, expandedHeight = 64.dp,
                        windowInsets = insets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                }) {
                    Box(Modifier.fillMaxSize().windowInsetsPadding(insets).testTag("content"))
                }
            }
        }
        assertContentInsets(top = 96f, bottom = 48f)
    }

    @Test fun keyboardAndNavigationInsetsUseTheLargerInsetAndRestoreAfterHiding() {
        var keyboardHeight by mutableStateOf(0.dp)
        compose.setContent {
            BloodPressureTheme {
                val bars = WindowInsets(left = 24.dp, top = 32.dp, right = 40.dp, bottom = 48.dp)
                val safeDrawing = bars.union(WindowInsets(bottom = keyboardHeight))
                ScreenScaffold(Modifier.testTag("window"), contentWindowInsets = safeDrawing) {
                    Box(Modifier.fillMaxSize().windowInsetsPadding(safeDrawing).testTag("content"))
                }
            }
        }
        assertContentInsets(top = 32f, bottom = 48f)
        compose.runOnIdle { keyboardHeight = 260.dp }
        assertContentInsets(top = 32f, bottom = 260f)
        compose.runOnIdle { keyboardHeight = 0.dp }
        assertContentInsets(top = 32f, bottom = 48f)
    }

    private fun assertContentInsets(top: Float, bottom: Float) {
        val window = compose.onNodeWithTag("window").getUnclippedBoundsInRoot()
        val content = compose.onNodeWithTag("content").getUnclippedBoundsInRoot()
        assertEquals(24f, (content.left - window.left).value, .5f)
        assertEquals(40f, (window.right - content.right).value, .5f)
        assertEquals(top, (content.top - window.top).value, .5f)
        assertEquals(bottom, (window.bottom - content.bottom).value, .5f)
    }
}
