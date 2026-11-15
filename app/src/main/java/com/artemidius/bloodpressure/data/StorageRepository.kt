package com.artemidius.bloodpressure.data

import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

interface StorageRepository {
    suspend fun saveData(data: PressureData, userId: String)
    fun observeData(userId: String): Flow<ReadingSnapshot>
    suspend fun deleteData(id: String, userId: String)
    suspend fun deleteAllData(userId: String)
    suspend fun isSyncEnabled(): Boolean
    fun setSyncEnabled(status: Boolean)
}

data class ReadingSnapshot(val readings: List<PressureData>, val pending: Boolean, val cached: Boolean)

data class PressureData(
    val systolic: Int,
    val diastolic: Int,
    val timestamp: Long,
    val id: String = ""
)

// Technical input limits, not diagnostic categories. Never clamp a measurement.
fun validPressure(systolic: Int, diastolic: Int): Boolean =
    systolic in 1..300 && diastolic in 1..300 && systolic > diastolic

fun PressureData.displayDate(): String = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(timestamp))

fun PressureData.toCsvLine(): String =
    "$systolic,$diastolic,${Instant.ofEpochMilli(timestamp)}\r\n"

fun pressureCsv(data: List<PressureData>): String =
    "systolic_mmHg,diastolic_mmHg,measured_at_UTC\r\n" +
        data.sortedBy { it.timestamp }.joinToString("") { it.toCsvLine() }
