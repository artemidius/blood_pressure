package com.artemidius.bloodpressure.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artemidius.bloodpressure.data.StorageRepository
import com.artemidius.bloodpressure.health.connect.HealthConnectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
data class HealthConnectState(
    val syncSwitch: SyncSwitch?,
    val launchPermissionDialog: Boolean = false,
)

@HiltViewModel
class HealthConnectViewModel @Inject constructor(
    private val storageRepository: StorageRepository,
    healthConnectRepository: HealthConnectRepository,
) : ViewModel() {

    private val stateFlow: MutableStateFlow<HealthConnectState> =
        MutableStateFlow(
            HealthConnectState(
                if (healthConnectRepository.isHealthConnectAvailable()) {
                    SyncSwitch(false)
                } else {
                    null
                }
            )
        )
    val state: StateFlow<HealthConnectState> = stateFlow
    private val healthPermissionFlow = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val syncEnabled = storageRepository.isSyncEnabled()
            stateFlow.value.syncSwitch?.let {
                stateFlow.value = stateFlow.value.copy(syncSwitch = SyncSwitch(syncEnabled))
            }
        }
    }

    fun enableDataSync(status: Boolean) {
        if (healthPermissionFlow.value) {
            stateFlow.value = HealthConnectState(SyncSwitch(status))
            storageRepository.setSyncEnabled(status)
        } else {
            stateFlow.value = stateFlow.value.copy(launchPermissionDialog = true)
        }
    }
    fun updatePermissionStatus(status: Boolean) {
        healthPermissionFlow.value = status
        stateFlow.value = stateFlow.value.copy(
            launchPermissionDialog = !status,
            syncSwitch = SyncSwitch(status)
        )
        storageRepository.setSyncEnabled(status)
    }
}
