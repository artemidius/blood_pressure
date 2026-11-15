package com.artemidius.bloodpressure.viewmodel

data class HealthConnectState(
    val syncSwitch: SyncSwitch? = null,
    val launchPermissionDialog: Boolean = false,
    val showPermissionSettings: Boolean = false,
    val message: String? = null
)
