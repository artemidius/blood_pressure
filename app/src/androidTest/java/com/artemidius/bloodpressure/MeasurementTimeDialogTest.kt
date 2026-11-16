package com.artemidius.bloodpressure

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.artemidius.bloodpressure.compose.screens.MeasurementTimeDialog
import com.artemidius.bloodpressure.ui.theme.BloodPressureTheme
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MeasurementTimeDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun opensOnDateStepBeforeTimeStep() {
        compose.setContent { BloodPressureTheme {
            MeasurementTimeDialog(System.currentTimeMillis(), {}, {})
        } }
        compose.onNodeWithText("Measurement time").assertDoesNotExist()
        compose.onNodeWithText("Next").assertExists()
        compose.onNodeWithText("Cancel").assertExists()
    }

    @Test fun cancellingOnDateStepDismissesWithoutSelection() {
        var selected: Long? = null
        var dismissed = false
        compose.setContent { BloodPressureTheme {
            MeasurementTimeDialog(System.currentTimeMillis(), { dismissed = true }, { selected = it })
        } }
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertNull(selected); assertTrue(dismissed) }
    }

    @Test fun timeCanBeTypedInsteadOfPicked() {
        var selected: Long? = null
        var dismissed = false
        compose.setContent { BloodPressureTheme {
            MeasurementTimeDialog(System.currentTimeMillis(), { dismissed = true }, { selected = it })
        } }
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Type time").performClick()
        compose.onNodeWithText("Clock").assertExists()
        compose.onNodeWithText("Set").performClick()
        compose.runOnIdle { assertNotNull(selected); assertTrue(dismissed) }
    }
}
