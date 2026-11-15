package com.artemidius.bloodpressure.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.Firebase
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class StorageRepositoryImpl @Inject constructor(private val preferences: SharedPreferences) : StorageRepository {
    private val db = Firebase.firestore
    private fun collection(userId: String) = db.collection("blood_pressure").document(
        userId.also { require(it.isNotBlank()) }
    ).collection("data")

    override suspend fun saveData(data: PressureData, userId: String) {
        require(data.id.isNotBlank() && validPressure(data.systolic, data.diastolic))
        collection(userId).document(data.id).set(mapOf(
            "systolic" to data.systolic, "diastolic" to data.diastolic,
            "timestamp" to data.timestamp
        )).awaitResult()
    }

    override fun observeData(userId: String) = callbackFlow {
        val registration = collection(userId).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            if (snapshot != null) {
                val readings = snapshot.documents.mapNotNull { doc ->
                    val sys = doc.getLong("systolic") ?: return@mapNotNull null
                    val dia = doc.getLong("diastolic") ?: return@mapNotNull null
                    val time = doc.getLong("timestamp") ?: return@mapNotNull null
                    if (sys !in 1..300 || dia !in 1..300 || time <= 0) return@mapNotNull null
                    PressureData(sys.toInt(), dia.toInt(), time, doc.id)
                }.sortedByDescending { it.timestamp }
                trySend(ReadingSnapshot(readings, snapshot.metadata.hasPendingWrites(), snapshot.metadata.isFromCache))
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun deleteData(id: String, userId: String) {
        collection(userId).document(id).delete().awaitResult()
    }

    override suspend fun deleteAllData(userId: String) {
        // Server reads avoid reporting a partial cached deletion as complete.
        while (true) {
            val page = collection(userId).limit(400).get(Source.SERVER).awaitResult()
            if (page.isEmpty) break
            db.batch().apply { page.documents.forEach { delete(it.reference) } }.commit().awaitResult()
        }
    }

    override suspend fun isSyncEnabled() = preferences.getBoolean("sync_enabled", false)
    override fun setSyncEnabled(status: Boolean) { preferences.edit { putBoolean("sync_enabled", status) } }
}
