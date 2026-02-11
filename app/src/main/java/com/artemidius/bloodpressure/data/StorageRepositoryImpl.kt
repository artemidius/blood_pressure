package com.artemidius.bloodpressure.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class StorageRepositoryImpl @Inject constructor(private val sharedPreferences: SharedPreferences) : StorageRepository {
    private val db = Firebase.firestore
    private val _dataFlow = MutableStateFlow<List<PressureData>>(emptyList())
    private val syncKey = "sync_enabled"

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

    override suspend fun isSyncEnabled(): Boolean {
        return sharedPreferences.getBoolean(syncKey, false)
    }

    override fun setSyncEnabled(status: Boolean) {
        sharedPreferences.edit {
            putBoolean(syncKey, status)
        }
    }
}