package com.artemidius.bloodpressure.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artemidius.bloodpressure.data.StorageRepository
import com.artemidius.bloodpressure.health.connect.HealthConnectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthConnectViewModel @Inject constructor(
    private val storage: StorageRepository,
    private val health: HealthConnectRepository
) : ViewModel() {
    private val mutable = MutableStateFlow(HealthConnectState())
    val state = mutable.asStateFlow()
    private var enableAfterPermission = false
    init { refresh() }

    fun refresh() { viewModelScope.launch {
        try {
            if (!health.isHealthConnectAvailable()) {
                mutable.value = HealthConnectState(message = "Install or update Health Connect to enable copying readings.")
            } else {
                val granted = health.hasPermission()
                if (enableAfterPermission && granted) { storage.setSyncEnabled(true); enableAfterPermission = false }
                mutable.value = mutable.value.copy(
                    syncSwitch = SyncSwitch(storage.isSyncEnabled()),
                    message = if (storage.isSyncEnabled() && !granted) "Permission was removed. Enable Health Connect again to resume." else null
                )
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            mutable.value = mutable.value.copy(message = "Could not check Health Connect. Try again.")
        }
    } }
    fun enableDataSync(enabled: Boolean) {
        if (!enabled) {
            enableAfterPermission = false
            storage.setSyncEnabled(false)
            mutable.value = mutable.value.copy(syncSwitch = SyncSwitch(false), launchPermissionDialog = false)
            return
        }
        viewModelScope.launch {
            try {
                if (health.hasPermission()) { storage.setSyncEnabled(true); refresh() }
                else {
                    enableAfterPermission = true
                    mutable.value = mutable.value.copy(launchPermissionDialog = true)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                mutable.value = mutable.value.copy(message = "Health Connect is unavailable. Check settings.")
            }
        }
    }
    fun updatePermissionStatus(granted: Boolean) {
        mutable.value = mutable.value.copy(launchPermissionDialog = false,
            message = if (granted) null else "Permission was not granted. You can enable it in Health Connect settings.")
        if (granted && enableAfterPermission) storage.setSyncEnabled(true)
        // Retain intent while user visits settings; turning off cancels it explicitly.
        refresh()
    }
}
