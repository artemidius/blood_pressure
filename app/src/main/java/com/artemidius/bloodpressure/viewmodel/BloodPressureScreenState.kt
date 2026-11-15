package com.artemidius.bloodpressure.viewmodel

import com.artemidius.bloodpressure.data.PressureData

sealed interface BloodPressureScreenState {
    data object Loading : BloodPressureScreenState
    data object Unauthorized : BloodPressureScreenState
    data class Data(
        val systolic: String = "120",
        val diastolic: String = "80",
        val timestamp: Long = System.currentTimeMillis(),
        val editingId: String? = null,
        val list: List<PressureData> = emptyList(),
        val saving: Boolean = false,
        val pending: Boolean = false,
        val cached: Boolean = false,
        val loading: Boolean = true,
        val error: String? = null,
        val message: String? = null
    ) : BloodPressureScreenState
}

data class SyncSwitch(val isChecked: Boolean)
