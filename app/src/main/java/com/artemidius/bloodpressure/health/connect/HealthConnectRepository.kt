package com.artemidius.bloodpressure.health.connect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Pressure
import com.artemidius.bloodpressure.data.PressureData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Repository for Health Connect data.
 * more here: https://developer.android.com/health-and-fitness/guides/health-connect
 */
interface HealthConnectRepository {
    fun isHealthConnectAvailable(): Boolean
    fun writeBloodPressure(data: PressureData)
}

class HealthConnectRepositoryImpl @Inject constructor(
    private val client: HealthConnectClient?
) : HealthConnectRepository {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun isHealthConnectAvailable(): Boolean {
        return client != null
    }

    override fun writeBloodPressure(data: PressureData) {
        val bloodPressureRecord = BloodPressureRecord(
            systolic = Pressure.millimetersOfMercury(data.systolic.toDouble()),
            diastolic = Pressure.millimetersOfMercury(data.diastolic.toDouble()),
            time = Instant.now(),
            zoneOffset = null,
            metadata = Metadata.manualEntry()
        )
        scope.launch {
            client?.insertRecords(listOf(bloodPressureRecord))
        }
    }
}