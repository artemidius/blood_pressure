package com.artemidius.bloodpressure.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artemidius.bloodpressure.ml.ImageData
import com.artemidius.bloodpressure.ml.ScanImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanState(
    val processing: Boolean = false,
    val waitingForImage: Boolean = false,
    val error: String? = null,
    val result: ImageData? = null
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val images: ScanImageRepository,
    private val saved: SavedStateHandle
) : ViewModel() {
    private val mutable = MutableStateFlow(ScanState(waitingForImage = saved.get<String>("source") != null))
    val state = mutable.asStateFlow()
    private var job: Job? = null
    private var generation = 0

    init {
        // A retained ViewModel keeps processing through rotation. After process death,
        // ask for another image instead of restoring a spinner without a running job.
        if (saved.get<Boolean>("processing") == true) {
            clearPhoto()
            saved["processing"] = false
            saved["source"] = null
            mutable.value = ScanState(error = "Recognition was interrupted. Please submit another image.")
        }
    }

    fun takePhoto(): Uri? {
        cancel()
        return try {
            images.createPhoto().also { uri ->
                saved["photo"] = uri
                waiting("camera")
            }
        } catch (_: Exception) {
            mutable.value = ScanState(error = "Could not prepare a photo. Try again or choose an image from the gallery.")
            null
        }
    }

    fun chooseGallery() { cancel(); waiting("gallery") }
    private fun waiting(source: String) {
        saved["source"] = source
        mutable.value = ScanState(waitingForImage = true)
    }

    fun photoReturned(success: Boolean) {
        if (saved.get<String>("source") != "camera") return
        val uri = saved.get<Uri>("photo")
        if (success && uri != null) recognize(uri) else cancel()
    }
    fun galleryReturned(uri: Uri?) {
        if (saved.get<String>("source") != "gallery") return
        if (uri != null) recognize(uri) else cancel()
    }
    fun launchFailed() {
        cancel()
        mutable.value = ScanState(error = "Could not open the camera or gallery. Please try another image source.")
    }

    private fun recognize(uri: Uri) {
        job?.cancel()
        val request = ++generation
        val photo = saved.get<Uri>("photo")
        saved["source"] = null
        saved["processing"] = true
        mutable.value = ScanState(processing = true)
        job = viewModelScope.launch {
            try {
                val result = images.recognize(uri)
                ensureActive()
                if (request == generation) mutable.value = if (result != null) ScanState(result = result)
                    else ScanState(error = "Reading not recognized. Make sure both SYS and DIA values are clear, then submit another image.")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (request == generation) mutable.value = ScanState(error = "Could not read this image. Please take another photo or choose a different image.")
            } finally {
                if (request == generation) saved["processing"] = false
            }
        }.also { inference ->
            // Also cleans up if cancellation occurs before the coroutine starts.
            inference.invokeOnCompletion {
                photo?.let(images::deletePhoto)
                if (saved.get<Uri>("photo") == photo) saved["photo"] = null
            }
        }
    }

    fun consumeResult(): ImageData? {
        val result = mutable.value.result
        mutable.value = ScanState()
        return result
    }

    fun cancel() {
        generation++
        job?.cancel(); job = null
        clearPhoto()
        saved["source"] = null
        saved["processing"] = false
        mutable.value = ScanState()
    }

    private fun clearPhoto() {
        saved.get<Uri>("photo")?.let(images::deletePhoto)
        saved["photo"] = null
    }

    override fun onCleared() { clearPhoto() }
}
