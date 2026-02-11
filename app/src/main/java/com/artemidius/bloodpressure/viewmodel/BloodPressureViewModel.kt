package com.artemidius.bloodpressure.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artemidius.bloodpressure.auth.AuthRepository
import com.artemidius.bloodpressure.data.FileRepository
import com.artemidius.bloodpressure.data.PressureData
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
    private val healthConnectRepository: HealthConnectRepository,
    private val mapGraphDataUseCase: MapGraphDataUseCase
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

    /**
     * Updates the systolic blood pressure value in the current screen state.
     */
    fun setSys(sys: Int) {
        (stateFlow.value as? BloodPressureScreenState.Data)?.let { currentState ->
            val newState = currentState.copy(systolic = sys)
            stateFlow.value = newState
        }
    }

    /**
     * Updates the diastolic blood pressure value in the current state.
     */
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
                            chartData = mapGraphDataUseCase(pressureData.map { it.toUiItem() })
                        )
                    } ?: run { stateFlow.value = BloodPressureScreenState.Data.getDefault() }
                }
            }

        } else {
            stateFlow.value = BloodPressureScreenState.Unauthorized
        }
    }
}
