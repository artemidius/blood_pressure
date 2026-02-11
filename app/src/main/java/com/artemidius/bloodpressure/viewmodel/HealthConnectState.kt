package com.artemidius.bloodpressure.viewmodel

import androidx.compose.runtime.Stable

@Stable
data class HealthConnectState(
    val syncSwitch: SyncSwitch?,
    val launchPermissionDialog: Boolean = false,
)