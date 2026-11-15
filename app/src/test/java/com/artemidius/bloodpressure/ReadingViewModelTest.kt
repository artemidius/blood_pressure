package com.artemidius.bloodpressure

import android.content.ContentResolver
import android.net.Uri
import com.artemidius.bloodpressure.auth.AuthRepository
import com.artemidius.bloodpressure.data.*
import com.artemidius.bloodpressure.health.connect.HealthConnectRepository
import com.artemidius.bloodpressure.viewmodel.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val auth = mockk<AuthRepository>()
    private val health = mockk<HealthConnectRepository>(relaxed = true)
    private val files = mockk<FileRepository>(relaxed = true)
    private val storage = FakeStorage()
    private lateinit var vm: BloodPressureViewModel
    @Before fun setup() { Dispatchers.setMain(dispatcher); every { auth.getUserId() } returns "alice"; vm = BloodPressureViewModel(auth, storage, files, health) }
    @After fun tearDown() { every { auth.getUserId() } returns null; vm.refresh(); Dispatchers.resetMain() }
    private fun state() = vm.state.value as BloodPressureScreenState.Data
    private fun input() { vm.setSys("120"); vm.setDia("80") }

    @Test fun `startup selects newest saved measurement without editing the old record`() = runTest(dispatcher) {
        val timestamp = state().timestamp
        storage.alice.value = ReadingSnapshot(listOf(
            PressureData(115, 75, 100, "older"),
            PressureData(138, 88, 300, "latest"),
            PressureData(125, 82, 200, "middle")
        ), false, false)
        runCurrent()
        assertEquals("138", state().systolic); assertEquals("88", state().diastolic)
        assertEquals(timestamp, state().timestamp); assertNull(state().editingId)
        vm.submitData(); runCurrent()
        assertNotEquals("latest", storage.saved.single().id)
    }
    @Test fun `startup without history keeps 120 over 80`() = runTest(dispatcher) {
        storage.alice.value = ReadingSnapshot(emptyList(), false, false)
        runCurrent()
        assertEquals("120", state().systolic); assertEquals("80", state().diastolic)
        assertFalse(state().loading)
    }
    @Test fun `empty cache waits for server and cached history can seed startup`() = runTest(dispatcher) {
        assertTrue(state().loading)
        storage.alice.value = ReadingSnapshot(emptyList(), false, true); runCurrent()
        assertTrue(state().loading)
        storage.alice.value = ReadingSnapshot(listOf(PressureData(125, 82, 100, "cached")), false, true); runCurrent()
        assertEquals("125", state().systolic); assertEquals("82", state().diastolic)
        assertFalse(state().loading)
        storage.alice.value = ReadingSnapshot(listOf(PressureData(132, 86, 200, "server")), false, false); runCurrent()
        assertEquals("132", state().systolic); assertEquals("86", state().diastolic)
        storage.alice.value = ReadingSnapshot(listOf(PressureData(140, 90, 300, "later")), false, false); runCurrent()
        vm.refresh(); runCurrent()
        assertEquals("132", state().systolic); assertEquals("86", state().diastolic)
    }
    @Test fun `late startup history never overwrites an edited or scanned draft`() = runTest(dispatcher) {
        storage.alice.value = ReadingSnapshot(emptyList(), false, true); runCurrent()
        vm.setSys("145")
        storage.alice.value = ReadingSnapshot(listOf(PressureData(130, 85, 100, "saved")), false, false); runCurrent()
        assertEquals("145", state().systolic); assertEquals("80", state().diastolic)
        every { auth.getUserId() } returns "bob"; vm.refresh()
        vm.setInput(com.artemidius.bloodpressure.ml.ImageData(128, 78))
        storage.bob.value = ReadingSnapshot(listOf(PressureData(135, 89, 200, "bob")), false, false); runCurrent()
        assertEquals("128", state().systolic); assertEquals("78", state().diastolic)
    }
    @Test fun `switching accounts seeds only the new accounts latest reading`() = runTest(dispatcher) {
        storage.alice.value = ReadingSnapshot(listOf(PressureData(140, 90, 100, "alice")), false, false); runCurrent()
        storage.bob.value = ReadingSnapshot(listOf(PressureData(118, 76, 200, "bob")), false, false)
        every { auth.getUserId() } returns "bob"; vm.refresh(); runCurrent()
        assertEquals("118", state().systolic); assertEquals("76", state().diastolic)
    }

    @Test fun `refresh replaces subscriber and account switch clears previous readings`() = runTest(dispatcher) {
        runCurrent(); storage.alice.value = ReadingSnapshot(listOf(PressureData(140, 90, 1, "a")), false, false); runCurrent()
        repeat(3) { vm.refresh(); runCurrent() }
        assertEquals(1, storage.alice.subscriptionCount.value)
        every { auth.getUserId() } returns "bob"; vm.refresh()
        assertTrue(state().list.isEmpty()); runCurrent()
        assertEquals(0, storage.alice.subscriptionCount.value)
        assertTrue(state().list.isEmpty())
    }
    @Test fun `pending saves suppress duplicate taps and retry keeps record ID`() = runTest(dispatcher) {
        input(); storage.waitForSave = true
        vm.submitData(); runCurrent(); vm.submitData(); runCurrent()
        assertEquals(1, storage.saved.size); assertTrue(state().saving)
        advanceTimeBy(15_001); runCurrent()
        assertFalse(state().saving); assertFalse(state().message == "Reading saved.")
        storage.waitForSave = false; vm.submitData(); runCurrent()
        assertEquals(storage.saved[0].id, storage.saved[1].id)
        assertEquals("Reading saved.", state().message)
    }
    @Test fun `failed save does not report success and retains retry identity`() = runTest(dispatcher) {
        input(); storage.failSave = true; vm.submitData(); runCurrent()
        assertTrue(state().message!!.contains("Could not"))
        storage.failSave = false; vm.submitData(); runCurrent()
        assertEquals(storage.saved[0].id, storage.saved[1].id)
    }
    @Test fun `export cancellation does nothing and export takes one snapshot`() = runTest(dispatcher) {
        storage.alice.value = ReadingSnapshot(emptyList(), false, false)
        runCurrent()
        val resolver = mockk<ContentResolver>(); val uri = mockk<Uri>()
        vm.saveFile(null, resolver); runCurrent(); coVerify(exactly = 0) { files.saveFile(any(), any(), any()) }
        vm.saveFile(uri, resolver); runCurrent()
        storage.alice.value = ReadingSnapshot(listOf(PressureData(120, 80, 1, "a")), false, false); runCurrent()
        coVerify(exactly = 1) { files.saveFile(emptyList(), uri, resolver) }
    }
    @Test fun `Health Connect failure preserves successful cloud save`() = runTest(dispatcher) {
        input(); storage.sync = true
        coEvery { health.writeBloodPressure(any()) } throws SecurityException()
        vm.submitData(); runCurrent()
        assertTrue(state().message!!.startsWith("Reading saved. Health Connect was not updated"))
    }
    @Test fun `turning sync off never asks for permission`() = runTest(dispatcher) {
        storage.sync = true
        every { health.isHealthConnectAvailable() } returns true
        coEvery { health.hasPermission() } returns false
        val model = HealthConnectViewModel(storage, health); runCurrent()
        model.enableDataSync(false); runCurrent()
        assertFalse(storage.sync); assertFalse(model.state.value.launchPermissionDialog)
    }
    @Test fun `returning from settings checks current permission`() = runTest(dispatcher) {
        every { health.isHealthConnectAvailable() } returns true
        coEvery { health.hasPermission() } returns false
        val model = HealthConnectViewModel(storage, health); runCurrent()
        model.enableDataSync(true); runCurrent(); assertTrue(model.state.value.launchPermissionDialog)
        model.updatePermissionStatus(false); runCurrent()
        coEvery { health.hasPermission() } returns true
        model.refresh(); runCurrent(); assertTrue(storage.sync); assertTrue(model.state.value.syncSwitch!!.isChecked)
    }
    @Test fun `new draft and edit preserve distinct record identities`() = runTest(dispatcher) {
        input(); vm.submitData(); runCurrent()
        val original = storage.saved.single()
        vm.edit(original); vm.setSys("130"); vm.submitData(); runCurrent()
        assertEquals(original.id, storage.saved.last().id)
        vm.newReading(); input(); vm.submitData(); runCurrent()
        assertNotEquals(original.id, storage.saved.last().id)
    }
}

private class FakeStorage : StorageRepository {
    val alice = MutableStateFlow(ReadingSnapshot(emptyList(), false, true))
    val bob = MutableStateFlow(ReadingSnapshot(emptyList(), false, true))
    val saved = mutableListOf<PressureData>()
    var sync = false; var failSave = false; var waitForSave = false
    override fun observeData(userId: String): Flow<ReadingSnapshot> = if (userId == "alice") alice else bob
    override suspend fun saveData(data: PressureData, userId: String) { saved += data; if (failSave) error("failed"); if (waitForSave) awaitCancellation() }
    override suspend fun deleteData(id: String, userId: String) {}
    override suspend fun deleteAllData(userId: String) {}
    override suspend fun isSyncEnabled() = sync
    override fun setSyncEnabled(status: Boolean) { sync = status }
}
