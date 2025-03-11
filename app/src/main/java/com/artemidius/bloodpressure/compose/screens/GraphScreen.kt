package com.artemidius.bloodpressure.compose.screens

import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import co.yml.charts.ui.linechart.LineChart
import com.artemidius.bloodpressure.viewmodel.BloodPressureScreenState
import com.artemidius.bloodpressure.viewmodel.BloodPressureViewModel

@Composable
fun GraphScreen(viewModel: BloodPressureViewModel) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    val state = viewModel.state.collectAsState().value

    (state as? BloodPressureScreenState.Data)?.let { data ->
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            LineChart(
                modifier = Modifier.fillMaxSize(),
                lineChartData = data.chartData
            )
        }
    }
}
