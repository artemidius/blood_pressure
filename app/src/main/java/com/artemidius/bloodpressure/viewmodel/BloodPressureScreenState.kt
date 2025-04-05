package com.artemidius.bloodpressure.viewmodel

import androidx.compose.runtime.Stable
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import com.artemidius.bloodpressure.data.PressureUiItem
import com.google.common.collect.ImmutableList

sealed interface BloodPressureScreenState {
    data object Loading : BloodPressureScreenState
    data object Unauthorized : BloodPressureScreenState
    @Stable
    data class Data(
        val systolic: Int,
        val diastolic: Int,
        val showLogoutDialog: Boolean = false,
        val list: ImmutableList<PressureUiItem> = ImmutableList.of(),
        val chartData: LineChartData = LineChartData(LinePlotData(lines = emptyList()))
    ) : BloodPressureScreenState {
        companion object {
            fun getDefault() = Data(120, 80)
        }
    }
}

@Stable
data class SyncSwitch(
    val isChecked: Boolean
)