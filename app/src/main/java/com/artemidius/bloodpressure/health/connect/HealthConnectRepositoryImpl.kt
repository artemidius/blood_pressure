package com.artemidius.bloodpressure.health.connect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Pressure
import com.artemidius.bloodpressure.data.PressureData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class HealthConnectRepositoryImpl @Inject constructor(@param:ApplicationContext private val context: Context) : HealthConnectRepository {
    override fun isHealthConnectAvailable() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    private fun client(): HealthConnectClient {
        check(isHealthConnectAvailable()) { "Health Connect is unavailable" }
        return HealthConnectClient.getOrCreate(context)
    }
    override suspend fun hasPermission(): Boolean = isHealthConnectAvailable() &&
        HealthPermission.getWritePermission(BloodPressureRecord::class) in client().permissionController.getGrantedPermissions()

    override suspend fun writeBloodPressure(data: PressureData) {
        check(hasPermission()) { "Health Connect permission required" }
        val time = Instant.ofEpochMilli(data.timestamp)
        client().insertRecords(listOf(BloodPressureRecord(
            systolic = Pressure.millimetersOfMercury(data.systolic.toDouble()),
            diastolic = Pressure.millimetersOfMercury(data.diastolic.toDouble()),
            time = time,
            zoneOffset = ZoneId.systemDefault().rules.getOffset(time),
            metadata = Metadata.manualEntry(clientRecordId = data.id, clientRecordVersion = System.currentTimeMillis())
        )))
    }
    override suspend fun deleteBloodPressure(id: String) {
        check(hasPermission())
        client().deleteRecords(BloodPressureRecord::class, recordIdsList = emptyList(), clientRecordIdsList = listOf(id))
    }
}
