package com.artemidius.bloodpressure.compose.screens

import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.yml.charts.ui.linechart.LineChart
import com.artemidius.bloodpressure.viewmodel.BloodPressureScreenState
import com.artemidius.bloodpressure.viewmodel.BloodPressureViewModel

@Composable
fun GraphScreen(viewModel: BloodPressureViewModel) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    val state = viewModel.state.collectAsState().value

    (state as? BloodPressureScreenState.Data)?.let { data ->
        Column(
            modifier = Modifier.fillMaxSize().background(Color.White)
        ) {
            LineChart(
                modifier = Modifier.fillMaxWidth().padding(16.dp).background(Color.White),
                lineChartData = data.chartData
            )
        }
    }
}
