package com.artemidius.bloodpressure

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.artemidius.bloodpressure.compose.screens.ListScreen
import com.artemidius.bloodpressure.data.PressureData
import com.artemidius.bloodpressure.ui.theme.BloodPressureTheme
import com.artemidius.bloodpressure.viewmodel.BloodPressureScreenState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ListScreenTest {
    @get:Rule val compose = createComposeRule()

    private val reading = PressureData(126, 82, 1704067200000L, "record")

    @Test fun emptyHistoryExplainsHowToAddReadings() {
        compose.setContent { BloodPressureTheme {
            ListScreen(BloodPressureScreenState.Data(list = emptyList(), loading = false), {}, {})
        } }
        compose.onNodeWithText("No readings yet. Add your first reading from the main screen.").assertExists()
    }

    @Test fun editPassesTheSelectedReadingBack() {
        var edited: PressureData? = null
        compose.setContent { BloodPressureTheme {
            ListScreen(BloodPressureScreenState.Data(list = listOf(reading), loading = false), { edited = it }, {})
        } }
        compose.onNodeWithText("Edit").performClick()
        assertEquals(reading, edited)
    }

    @Test fun confirmingDeletionNotifiesTheReading() {
        var deleted: PressureData? = null
        compose.setContent { BloodPressureTheme {
            ListScreen(BloodPressureScreenState.Data(list = listOf(reading), loading = false), {}, { deleted = it })
        } }
        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("Delete reading?").assertExists()
        assertNull(deleted)
        compose.onAllNodesWithText("Delete").onLast().performClick()
        assertEquals(reading, deleted)
    }

    @Test fun actionsAreDisabledWhileSaving() {
        compose.setContent { BloodPressureTheme {
            ListScreen(BloodPressureScreenState.Data(list = listOf(reading), loading = false, saving = true), {}, {})
        } }
        compose.onNodeWithText("Edit").assertIsNotEnabled()
        compose.onNodeWithText("Delete").assertIsNotEnabled()
    }

    @Test fun storageMessageIsShown() {
        compose.setContent { BloodPressureTheme {
            ListScreen(BloodPressureScreenState.Data(list = emptyList(), loading = false, message = "Reading deleted."), {}, {})
        } }
        compose.onNodeWithText("Reading deleted.").assertExists()
    }
}
