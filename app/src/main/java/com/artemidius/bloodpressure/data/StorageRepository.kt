package com.artemidius.bloodpressure.data

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Interface for managing the storage of PressureData.
 */
interface StorageRepository {
    /**
     * Saves pressure data to the storage.
     */
    fun saveData(data: PressureData, userId: String)
    /**
     * Retrieves a list of pressure data for a specific user.
     */
    suspend fun retrieveData(userId: String): StateFlow<List<PressureData>>

    /**
     * Checks if synchronization is currently enabled.
     */
    suspend fun isSyncEnabled(): Boolean

    /**
     * Sets the synchronization status.
     */
    fun setSyncEnabled(status: Boolean)
}

data class PressureData(
    val systolic: Int,
    val diastolic: Int,
    val timestamp: Long
)

@Serializable
@Stable
data class PressureUiItem(
    val systolic: Int,
    val diastolic: Int,
    val date: String
)

fun PressureData.toUiItem(): PressureUiItem {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
    return PressureUiItem(systolic, diastolic, formatter.format(date))
}

fun PressureData.toCsvLine(): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("dd.MM,HH:mm", Locale.getDefault())
    return "$systolic,$diastolic,${ formatter.format(date)}\r\n"
}