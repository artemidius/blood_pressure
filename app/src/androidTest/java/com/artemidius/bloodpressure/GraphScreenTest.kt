package com.artemidius.bloodpressure

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.artemidius.bloodpressure.compose.screens.GraphScreen
import com.artemidius.bloodpressure.data.PressureData
import com.artemidius.bloodpressure.ui.theme.BloodPressureTheme
import org.junit.Rule
import org.junit.Test

class GraphScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun reading(daysAgo: Long, systolic: Int, diastolic: Int, id: String) =
        PressureData(systolic, diastolic, System.currentTimeMillis() - daysAgo * 86_400_000L, id)

    private fun trendDescription(count: Int) =
        "Blood pressure trend, $count readings. Exact values follow below."

    @Test fun periodChipsFilterReadingsByAge() {
        val recent = reading(2, 130, 85, "recent")
        val old = reading(20, 118, 76, "old")
        compose.setContent { BloodPressureTheme { GraphScreen(listOf(recent, old)) } }
        compose.onNodeWithContentDescription(trendDescription(2)).assertExists()
        compose.onNodeWithText("7 days").performClick()
        compose.onNodeWithContentDescription(trendDescription(1)).assertExists()
        compose.onNodeWithText("130/85 mmHg", substring = true).assertExists()
        compose.onNodeWithText("118/76 mmHg", substring = true).assertDoesNotExist()
        compose.onNodeWithText("All").performClick()
        compose.onNodeWithContentDescription(trendDescription(2)).assertExists()
        compose.onNodeWithText("118/76 mmHg", substring = true).assertExists()
    }

    @Test fun emptyPeriodOffersWiderRange() {
        val old = reading(40, 118, 76, "old")
        compose.setContent { BloodPressureTheme { GraphScreen(listOf(old)) } }
        compose.onNodeWithText("No readings in this period.").assertExists()
        compose.onNodeWithContentDescription(trendDescription(1)).assertDoesNotExist()
        compose.onNodeWithText("All").performClick()
        compose.onNodeWithContentDescription(trendDescription(1)).assertExists()
        compose.onNodeWithText("Systolic — solid line").assertExists()
    }

    @Test fun legendAndRangeExplainTheChart() {
        val readings = listOf(reading(1, 130, 85, "a"), reading(3, 110, 70, "b"))
        compose.setContent { BloodPressureTheme { GraphScreen(readings) } }
        compose.onNodeWithText("Systolic — solid line").assertExists()
        compose.onNodeWithText("Diastolic — dashed line").assertExists()
        compose.onNodeWithText("Time runs left to right; gaps reflect elapsed time.").assertExists()
        compose.onNodeWithText("Readings in this period").assertExists()
    }
}
