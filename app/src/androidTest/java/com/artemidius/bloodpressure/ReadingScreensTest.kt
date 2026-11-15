package com.artemidius.bloodpressure

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.compose.runtime.*
import com.artemidius.bloodpressure.ml.*
import com.artemidius.bloodpressure.viewmodel.ScanState
import com.artemidius.bloodpressure.viewmodel.ScanViewModel
import kotlinx.coroutines.CompletableDeferred
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.artemidius.bloodpressure.compose.screens.*
import com.artemidius.bloodpressure.data.PressureData
import com.artemidius.bloodpressure.ui.theme.BloodPressureTheme
import com.artemidius.bloodpressure.viewmodel.BloodPressureScreenState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ReadingScreensTest {
    @get:Rule val compose = createComposeRule()
    @Test fun darkDateAndTimeDialogOnlyUpdatesAfterConfirmation() {
        val initial = java.time.Instant.parse("2026-07-15T12:34:00Z").toEpochMilli()
        var selected: Long? = null
        var dismissed = false
        compose.setContent { BloodPressureTheme(darkTheme = true) {
            MeasurementTimeDialog(initial, { dismissed = true }, { selected = it })
        } }
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Measurement time").assertExists()
        compose.runOnIdle { assertNull(selected); assertFalse(dismissed) }
        compose.onNodeWithText("Set").performClick()
        compose.runOnIdle { assertEquals(initial, selected); assertTrue(dismissed) }
    }

    @Test fun cancellingTimeDialogLeavesMeasurementUnchanged() {
        var selected: Long? = null
        var dismissed = false
        compose.setContent { BloodPressureTheme(darkTheme = true) {
            MeasurementTimeDialog(System.currentTimeMillis(), { dismissed = true }, { selected = it })
        } }
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertNull(selected); assertTrue(dismissed) }
    }

    @Test fun scannerOffersCameraAndGallery() {
        var camera = 0
        var gallery = 0
        compose.setContent { BloodPressureTheme { ScanContent(ScanState(), {}, { camera++ }, { gallery++ }) } }
        compose.onNodeWithText("Camera").performClick()
        compose.onNodeWithText("Gallery").performClick()
        assertEquals(1, camera); assertEquals(1, gallery)
    }

    @Test fun scannerShowsProgressThenRetryChoicesOnFailure() {
        var state by mutableStateOf(ScanState(processing = true))
        compose.setContent { BloodPressureTheme { ScanContent(state, {}, {}, {}) } }
        compose.onNodeWithContentDescription("Recognizing reading").assertExists()
        compose.onNodeWithText("Camera").assertDoesNotExist()
        compose.onNodeWithText("Gallery").assertDoesNotExist()
        compose.onNodeWithText("Cancel").assertExists()
        compose.runOnIdle { state = ScanState(error = "Reading not recognized.") }
        compose.onNodeWithContentDescription("Recognizing reading").assertDoesNotExist()
        compose.onNodeWithText("Reading not recognized.").assertExists()
        compose.onNodeWithText("Camera").assertIsEnabled()
        compose.onNodeWithText("Gallery").assertIsEnabled()
    }

    @Test fun recognizedPhotoReturnsToInputsWithoutConfirmation() {
        val pending = CompletableDeferred<ImageData?>()
        val images = object : ScanImageRepository {
            override fun createPhoto(): Uri = Uri.EMPTY
            override fun deletePhoto(uri: Uri) {}
            override suspend fun recognize(uri: Uri): ImageData? = pending.await()
        }
        val scanner = ScanViewModel(images, SavedStateHandle())
        var result by mutableStateOf<ImageData?>(null)
        var callbacks = 0
        compose.setContent { BloodPressureTheme {
            if (result == null) ScanScreen(scanner, {}, { callbacks++; result = it })
            else DataInputScreen(BloodPressureScreenState.Data(systolic = result!!.systolic.toString(),
                diastolic = result!!.diastolic.toString(), loading = false), {}, {}, {}, {}, {}, {}, {}, {})
        } }
        compose.runOnIdle { scanner.chooseGallery(); scanner.galleryReturned(Uri.EMPTY) }
        compose.onNodeWithContentDescription("Recognizing reading").assertExists()
        compose.runOnIdle { pending.complete(ImageData(126, 81)) }
        compose.onNodeWithText("126").assertExists()
        compose.onNodeWithText("81").assertExists()
        compose.onNodeWithText("Use these values").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, callbacks); scanner.cancel() }
    }

    @Test fun loadingHidesInputsUntilSavedValuesAreReady() {
        var state by mutableStateOf(BloodPressureScreenState.Data())
        compose.setContent { BloodPressureTheme {
            DataInputScreen(state, {}, {}, {}, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithContentDescription("Loading readings").assertExists()
        compose.onNodeWithContentDescription("Systolic").assertDoesNotExist()
        compose.onNodeWithContentDescription("Diastolic").assertDoesNotExist()
        compose.onNodeWithText("Save reading").assertDoesNotExist()
        compose.runOnIdle { state = state.copy(loading = false, systolic = "138", diastolic = "88") }
        compose.onNodeWithContentDescription("Loading readings").assertDoesNotExist()
        compose.onNodeWithContentDescription("Systolic").assertExists()
        compose.onNodeWithText("138").assertExists()
        compose.onNodeWithText("88").assertExists()
        compose.onNodeWithText("120").assertDoesNotExist()
    }

    @Test fun inputUsesOriginalSwipePickers() {
        var state by mutableStateOf(BloodPressureScreenState.Data(systolic = "120", diastolic = "80", loading = false))
        compose.setContent { BloodPressureTheme {
            DataInputScreen(state, { state = state.copy(systolic = it) }, { state = state.copy(diastolic = it) }, {}, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithText("Systolic").assertExists()
        compose.onNodeWithText("Diastolic").assertExists()
        compose.onNodeWithContentDescription("Systolic").performTouchInput { swipeUp() }
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(state.systolic.toInt() > 120); assertEquals("80", state.diastolic) }
        compose.onNodeWithText("Save reading").performScrollTo().assertIsEnabled()
    }
    @Test fun busyInputCannotSubmitAgain() {
        compose.setContent { BloodPressureTheme {
            DataInputScreen(BloodPressureScreenState.Data(saving = true, loading = false), {}, {}, {}, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithText("Saving…").performScrollTo().assertIsNotEnabled()
    }
    @Test fun deletionRequiresConfirmation() {
        var deleted = false
        val reading = PressureData(120, 80, 1704067200000, "record")
        compose.setContent { BloodPressureTheme {
            ListScreen(BloodPressureScreenState.Data(list = listOf(reading), loading = false), {}, { deleted = true })
        } }
        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("Delete reading?").assertExists()
        assertFalse(deleted)
        compose.onNodeWithText("Cancel").performClick()
        assertFalse(deleted)
    }
    @Test fun graphHandlesNoDataAndSingleReading() {
        var readings by mutableStateOf(emptyList<PressureData>())
        compose.setContent { BloodPressureTheme { GraphScreen(readings) } }
        compose.onNodeWithText("No readings in this period.").assertExists()
        compose.runOnIdle { readings = listOf(PressureData(120, 80, System.currentTimeMillis(), "one")) }
        compose.onNodeWithContentDescription("Blood pressure trend, 1 readings. Exact values follow below.").assertExists()
    }
}
