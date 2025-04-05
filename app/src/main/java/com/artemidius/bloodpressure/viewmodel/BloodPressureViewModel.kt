package com.artemidius.bloodpressure.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.artemidius.bloodpressure.auth.AuthRepository
import com.artemidius.bloodpressure.data.FileRepository
import com.artemidius.bloodpressure.data.PressureData
import com.artemidius.bloodpressure.data.PressureUiItem
import com.artemidius.bloodpressure.data.StorageRepository
import com.artemidius.bloodpressure.data.toUiItem
import com.artemidius.bloodpressure.health.connect.HealthConnectRepository
import com.artemidius.bloodpressure.ml.ImageData
import com.google.common.collect.ImmutableList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BloodPressureViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storageRepository: StorageRepository,
    private val fileRepository: FileRepository,
    private val healthConnectRepository: HealthConnectRepository
) : ViewModel() {

    private val stateFlow: MutableStateFlow<BloodPressureScreenState> =
        MutableStateFlow(BloodPressureScreenState.Loading)
    val state: StateFlow<BloodPressureScreenState> = stateFlow

    val fileName: String
        get() = "blood_pressure_${
            UUID.randomUUID().toString().take(4)
        }.csv"

    init {
        refreshData()
    }

    fun setSys(sys: Int) {
        (stateFlow.value as? BloodPressureScreenState.Data)?.let { currentState ->
            val newState = currentState.copy(systolic = sys)
            stateFlow.value = newState
        }
    }

    fun setDia(dia: Int) {
        (stateFlow.value as? BloodPressureScreenState.Data)?.let { currentState ->
            val newState = currentState.copy(diastolic = dia)
            stateFlow.value = newState
        }
    }

    fun submitData() {
        (stateFlow.value as? BloodPressureScreenState.Data)?.let { currentState ->
            val data = PressureData(
                systolic = currentState.systolic,
                diastolic = currentState.diastolic,
                timestamp = System.currentTimeMillis()
            )
            storageRepository.saveData(
                data = data,
                userId = authRepository.getUserId().orEmpty()
            )
            healthConnectRepository.writeBloodPressure(data)
        }
    }

    fun refresh() {
        refreshData()
    }

    fun setInput(imageData: ImageData) {
        (stateFlow.value as? BloodPressureScreenState.Data)?.let { currentState ->
            val newState =
                currentState.copy(systolic = imageData.systolic, diastolic = imageData.diastolic)
            stateFlow.value = newState
        }
    }

    fun showLogoutDialog(status: Boolean) {
        (stateFlow.value as? BloodPressureScreenState.Data)?.let { currentState ->
            val newState = currentState.copy(showLogoutDialog = status)
            stateFlow.value = newState
        }
    }

    fun saveFile(uri: Uri?, contentResolver: ContentResolver) {
        viewModelScope.launch {
            val userId = authRepository.getUserId().orEmpty()
            storageRepository.retrieveData(userId).collectLatest { pressureData ->
                fileRepository.saveFile(pressureData, uri, contentResolver)
            }
        }
    }

    private fun refreshData() {
        stateFlow.value = BloodPressureScreenState.Loading
        if (authRepository.isUserAuthorised() && authRepository.getUserId() != null) {
            val userId = authRepository.getUserId().orEmpty()
            viewModelScope.launch {
                storageRepository.retrieveData(userId).collectLatest { pressureData ->
                    val latestData = pressureData.maxByOrNull { it.timestamp }
                    latestData?.let { data ->
                        stateFlow.value = BloodPressureScreenState.Data(
                            systolic = data.systolic,
                            diastolic = data.diastolic,
                            list = ImmutableList.copyOf(pressureData.map { it.toUiItem() }),
                            chartData = mapGraphData(pressureData.map { it.toUiItem() })
                        )
                    } ?: run { stateFlow.value = BloodPressureScreenState.Data.getDefault() }
                }
            }

        } else {
            stateFlow.value = BloodPressureScreenState.Unauthorized
        }
    }

    private fun mapGraphData(list: List<PressureUiItem>): LineChartData {
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
