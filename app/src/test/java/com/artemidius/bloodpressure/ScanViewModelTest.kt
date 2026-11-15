package com.artemidius.bloodpressure

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.artemidius.bloodpressure.ml.ImageData
import com.artemidius.bloodpressure.ml.ScanImageRepository
import com.artemidius.bloodpressure.viewmodel.ScanState
import com.artemidius.bloodpressure.viewmodel.ScanViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val images = mockk<ScanImageRepository>(relaxed = true)
    private val uri = mockk<Uri>()
    private lateinit var vm: ScanViewModel
    @Before fun setup() { Dispatchers.setMain(dispatcher); vm = ScanViewModel(images, SavedStateHandle()) }
    @After fun tearDown() { vm.cancel(); Dispatchers.resetMain() }
    private fun select() { vm.chooseGallery(); vm.galleryReturned(uri) }

    @Test fun `one image shows progress then produces one consumable result`() = runTest(dispatcher) {
        val pending = CompletableDeferred<ImageData?>()
        coEvery { images.recognize(uri) } coAnswers { pending.await() }
        select(); runCurrent()
        assertTrue(vm.state.value.processing)
        pending.complete(ImageData(126, 81)); runCurrent()
        assertFalse(vm.state.value.processing)
        assertEquals(ImageData(126, 81), vm.consumeResult())
        assertNull(vm.consumeResult())
        coVerify(exactly = 1) { images.recognize(uri) }
        verify(exactly = 0) { images.deletePhoto(any()) }
    }
    @Test fun `unrecognized image offers retry and a new image can succeed`() = runTest(dispatcher) {
        coEvery { images.recognize(uri) } returns null
        select(); runCurrent()
        assertNotNull(vm.state.value.error); assertFalse(vm.state.value.processing)
        coEvery { images.recognize(uri) } returns ImageData(130, 85)
        vm.chooseGallery(); assertNull(vm.state.value.error)
        vm.galleryReturned(uri); runCurrent()
        assertEquals(ImageData(130, 85), vm.consumeResult())
    }
    @Test fun `unreadable image ends progress with an error`() = runTest(dispatcher) {
        coEvery { images.recognize(uri) } throws java.io.IOException("Unreadable image")
        select(); runCurrent()
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.processing); assertNull(vm.state.value.result)
    }
    @Test fun `cancel discards even a late noncancellable recognition result`() = runTest(dispatcher) {
        val pending = CompletableDeferred<ImageData?>()
        coEvery { images.recognize(uri) } coAnswers { withContext(NonCancellable) { pending.await() } }
        select(); runCurrent(); vm.cancel()
        pending.complete(ImageData(145, 95)); runCurrent()
        assertEquals(ScanState(), vm.state.value)
    }
    @Test fun `cancelled pickers do not recognize and camera files are removed`() = runTest(dispatcher) {
        every { images.createPhoto() } returns uri
        assertEquals(uri, vm.takePhoto())
        vm.photoReturned(false)
        verify { images.deletePhoto(uri) }
        vm.chooseGallery(); vm.galleryReturned(null); runCurrent()
        assertEquals(ScanState(), vm.state.value)
        coVerify(exactly = 0) { images.recognize(any()) }
    }
    @Test fun `camera result restores its pending destination and cleans it after recognition`() = runTest(dispatcher) {
        vm = ScanViewModel(images, SavedStateHandle(mapOf("source" to "camera", "photo" to uri)))
        coEvery { images.recognize(uri) } returns ImageData(126, 81)
        vm.photoReturned(true); runCurrent()
        assertEquals(ImageData(126, 81), vm.consumeResult())
        verify { images.deletePhoto(uri) }
    }
    @Test fun `process death during recognition restores retry instead of an endless spinner`() {
        vm = ScanViewModel(images, SavedStateHandle(mapOf("processing" to true, "photo" to uri)))
        assertNotNull(vm.state.value.error); assertFalse(vm.state.value.processing)
        verify { images.deletePhoto(uri) }
    }
    @Test fun `a picker callback after leaving the screen is ignored`() = runTest(dispatcher) {
        vm.chooseGallery(); vm.cancel(); vm.galleryReturned(uri); runCurrent()
        assertEquals(ScanState(), vm.state.value)
        coVerify(exactly = 0) { images.recognize(any()) }
    }
}
