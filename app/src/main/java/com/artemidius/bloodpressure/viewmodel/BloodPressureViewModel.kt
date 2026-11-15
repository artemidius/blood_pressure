package com.artemidius.bloodpressure.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artemidius.bloodpressure.auth.AuthRepository
import com.artemidius.bloodpressure.data.*
import com.artemidius.bloodpressure.health.connect.HealthConnectRepository
import com.artemidius.bloodpressure.ml.ImageData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BloodPressureViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storage: StorageRepository,
    private val files: FileRepository,
    private val health: HealthConnectRepository
) : ViewModel() {
    private val mutable = MutableStateFlow<BloodPressureScreenState>(BloodPressureScreenState.Loading)
    val state = mutable.asStateFlow()
    private var observer: Job? = null
    private var operation: Job? = null
    private var export: Job? = null
    private var account: String? = null
    private var draftId = UUID.randomUUID().toString()
    private var startupPrefillPending = true
    val fileName get() = "blood-pressure-${java.time.LocalDate.now()}.csv"
    init { refresh() }

    private fun update(block: (BloodPressureScreenState.Data) -> BloodPressureScreenState.Data) {
        (mutable.value as? BloodPressureScreenState.Data)?.let { mutable.value = block(it) }
    }
    private fun updateDraft(block: (BloodPressureScreenState.Data) -> BloodPressureScreenState.Data) {
        update {
            if (it.saving) it else {
                startupPrefillPending = false
                block(it)
            }
        }
    }
    fun setSys(value: String) { updateDraft { it.copy(systolic = value, message = null) } }
    fun setDia(value: String) { updateDraft { it.copy(diastolic = value, message = null) } }
    fun setTimestamp(value: Long) { updateDraft { it.copy(timestamp = value) } }
    fun setInput(value: ImageData) { updateDraft { it.copy(systolic = value.systolic.toString(), diastolic = value.diastolic.toString(), message = null) } }
    fun edit(value: PressureData) {
        if ((mutable.value as? BloodPressureScreenState.Data)?.saving == true) return
        startupPrefillPending = false
        draftId = value.id
        update { it.copy(systolic = value.systolic.toString(), diastolic = value.diastolic.toString(), timestamp = value.timestamp, editingId = value.id, message = null) }
    }
    fun newReading() {
        startupPrefillPending = false
        draftId = UUID.randomUUID().toString()
        update { it.copy(systolic = "120", diastolic = "80", timestamp = System.currentTimeMillis(), editingId = null, message = null) }
    }
    fun report(message: String) { update { it.copy(message = message) } }

    fun refresh() {
        val user = authRepository.getUserId()
        observer?.cancel()
        if (user != account) {
            operation?.cancel(); export?.cancel()
            account = user; draftId = UUID.randomUUID().toString()
            startupPrefillPending = true
            mutable.value = if (user == null) BloodPressureScreenState.Unauthorized else BloodPressureScreenState.Data()
        }
        if (user == null) { mutable.value = BloodPressureScreenState.Unauthorized; return }
        if (mutable.value !is BloodPressureScreenState.Data) mutable.value = BloodPressureScreenState.Data()
        update { it.copy(loading = true, error = null) }
        observer = viewModelScope.launch {
            storage.observeData(user).catch {
                if (account == user) update { it.copy(loading = false, error = "Could not load readings. Check your connection and retry.") }
            }.collect { snapshot ->
                if (account == user) update { current ->
                    // An empty cache does not prove that the account has no history.
                    // Cached readings can seed the wheels until the server responds, but
                    // user input always takes precedence over late history updates.
                    val prefill = startupPrefillPending && !current.saving &&
                        (snapshot.readings.isNotEmpty() || !snapshot.cached)
                    val latest = if (prefill) snapshot.readings.maxByOrNull { it.timestamp } else null
                    if (prefill && !snapshot.cached) startupPrefillPending = false
                    current.copy(
                        systolic = if (prefill) latest?.systolic?.toString() ?: "120" else current.systolic,
                        diastolic = if (prefill) latest?.diastolic?.toString() ?: "80" else current.diastolic,
                        list = snapshot.readings, pending = snapshot.pending, cached = snapshot.cached,
                        // Keep placeholders hidden until there are cached readings or
                        // the server confirms the account really has no history.
                        loading = startupPrefillPending && snapshot.cached && snapshot.readings.isEmpty(),
                        error = null
                    )
                }
            }
        }
    }

    fun submitData() {
        val current = mutable.value as? BloodPressureScreenState.Data ?: return
        val user = account ?: return
        if (current.saving || operation?.isActive == true) return
        val sys = current.systolic.toIntOrNull() ?: 0
        val dia = current.diastolic.toIntOrNull() ?: 0
        if (!validPressure(sys, dia)) { report("Enter values from 1 to 300 mmHg, with systolic higher than diastolic."); return }
        if (current.timestamp > System.currentTimeMillis()) { report("Measurement time cannot be in the future."); return }
        startupPrefillPending = false
        val reading = PressureData(sys, dia, current.timestamp, draftId)
        update { it.copy(saving = true, message = "Saving…") }
        operation = viewModelScope.launch {
            try {
                // Firestore queues writes offline. A timeout is not proof that a write failed.
                val confirmed = withTimeoutOrNull(15_000) { storage.saveData(reading, user); true } == true
                if (!confirmed) {
                    report("Waiting for cloud confirmation. The reading may still sync; Retry save uses the same record.")
                    return@launch
                }
                update { it.copy(editingId = reading.id) }
                var message = "Reading saved."
                if (storage.isSyncEnabled()) {
                    try { withTimeout(15_000) { health.writeBloodPressure(reading) } }
                    catch (e: Exception) {
                        if (e is CancellationException && e !is TimeoutCancellationException) throw e
                        message = "Reading saved. Health Connect was not updated. Check permissions and save again to retry."
                    }
                }
                report(message)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                report("Could not confirm the save. Retry save uses the same record.")
            } finally { if (account == user) update { it.copy(saving = false) } }
        }
    }

    fun delete(value: PressureData) {
        val user = account ?: return
        if (operation?.isActive == true) return
        update { it.copy(saving = true) }
        operation = viewModelScope.launch {
            try {
                withTimeout(15_000) { storage.deleteData(value.id, user) }
                if (storage.isSyncEnabled()) health.deleteBloodPressure(value.id)
                if ((mutable.value as? BloodPressureScreenState.Data)?.editingId == value.id) newReading()
                report("Reading deleted.")
            } catch (e: Exception) {
                if (e is CancellationException && e !is TimeoutCancellationException) throw e
                report("Deletion could not be fully confirmed. Check history and Health Connect before retrying.")
            } finally { if (account == user) update { it.copy(saving = false) } }
        }
    }

    fun saveFile(uri: Uri?, resolver: ContentResolver) {
        if (uri == null) return
        val current = mutable.value as? BloodPressureScreenState.Data ?: return
        if (current.loading || current.error != null) { report("Load your readings before exporting."); return }
        val snapshot = current.list.toList()
        val user = account
        export?.cancel()
        export = viewModelScope.launch {
            try { files.saveFile(snapshot, uri, resolver); if (account == user) report("CSV exported.") }
            catch (e: Exception) { if (e is CancellationException) throw e; if (account == user) report("Export failed. Choose another location and retry.") }
        }
    }

    suspend fun deleteAllReadings() {
        val user = account ?: return
        check(operation?.isActive != true) { "Wait for the current save to finish." }
        withTimeout(60_000) { storage.deleteAllData(user) }
        newReading()
        report("Cloud readings deleted. Manage any existing Health Connect copies in Health Connect settings.")
    }
}
