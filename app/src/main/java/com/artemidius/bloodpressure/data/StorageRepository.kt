package com.artemidius.bloodpressure.data

import androidx.compose.runtime.Stable
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

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
}

class StorageRepositoryImpl @Inject constructor() : StorageRepository {
    private val db = Firebase.firestore
    private val _dataFlow = MutableStateFlow<List<PressureData>>(emptyList())

    override fun saveData(data: PressureData, userId: String) {
        val map = hashMapOf(
            "systolic" to data.systolic,
            "diastolic" to data.diastolic,
            "timestamp" to data.timestamp
        )
        db
            .collection("blood_pressure/$userId/data")
            .add(map)
    }

    override suspend fun retrieveData(userId: String): StateFlow<List<PressureData>> {
        db.collection("blood_pressure/$userId/data")
            .get()
            .addOnSuccessListener { result ->
                    _dataFlow.value = result.documents
                        .map {
                            PressureData(
                                systolic = it.getLong("systolic")?.toInt() ?: 0,
                                diastolic = it.getLong("diastolic")?.toInt() ?: 0,
                                timestamp = it.getLong("timestamp") ?: 0
                            )
                        }.filter {
                            it.systolic != 0 && it.diastolic != 0 && it.timestamp != 0L
                        }.sortedBy { it.timestamp }
            }
            .addOnFailureListener {
                _dataFlow.value = emptyList()
            }
        return _dataFlow
    }
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