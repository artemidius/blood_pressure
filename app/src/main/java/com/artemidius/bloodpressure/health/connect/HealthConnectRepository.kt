package com.artemidius.bloodpressure.health.connect

import com.artemidius.bloodpressure.data.PressureData

interface HealthConnectRepository {
    fun isHealthConnectAvailable(): Boolean
    suspend fun hasPermission(): Boolean
    suspend fun writeBloodPressure(data: PressureData)
    suspend fun deleteBloodPressure(id: String)
}
