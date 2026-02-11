package com.artemidius.bloodpressure.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.model.GridLines
import co.yml.charts.ui.linechart.model.IntersectionPoint
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.SelectionHighlightPoint
import co.yml.charts.ui.linechart.model.SelectionHighlightPopUp
import com.artemidius.bloodpressure.data.PressureUiItem
import javax.inject.Inject

class MapGraphDataUseCase @Inject constructor() {
    operator fun invoke(list: List<PressureUiItem>): LineChartData {
        val pointsDataSystolic = list.mapIndexed { index, pressureUiItem ->
            Point(index.toFloat(), pressureUiItem.systolic.toFloat())
        }
        val xAxisData = AxisData.Builder()
            .axisStepSize(100.dp)
            .steps(pointsDataSystolic.size - 1)
            .labelData { i ->
                list[i].date.split(" ")[0]
            }
            .backgroundColor(Color.White)
            .build()

        val yAxisData = AxisData.Builder()
            .steps(pointsDataSystolic.size - 1)
            .backgroundColor(Color.White)
            .labelData { i ->
                "${list[i].systolic}/${list[i].diastolic}"
            }.build()

        val lineChartData = LineChartData(
            linePlotData = LinePlotData(
                lines = listOf(
                    Line(
                        dataPoints = pointsDataSystolic,
                        lineStyle = LineStyle(),
                        intersectionPoint = IntersectionPoint(),
                        selectionHighlightPoint = SelectionHighlightPoint(),
                        shadowUnderLine = null,
                        selectionHighlightPopUp = SelectionHighlightPopUp()
                    ),
                ),
            ),
            xAxisData = xAxisData,
            yAxisData = yAxisData,
            gridLines = GridLines(),
            backgroundColor = Color.White
        )
        return lineChartData
    }
}