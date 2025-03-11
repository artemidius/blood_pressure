package com.artemidius.bloodpressure.viewmodel

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis.Analyzer
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artemidius.bloodpressure.ml.ImageData
import com.artemidius.bloodpressure.ml.TextRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalGetImage
@HiltViewModel
class CameraPreviewViewModel @Inject constructor(
    private val textRecognizer: TextRecognizer
): ViewModel() {
    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest

    private val _state = MutableStateFlow(CameraScreenState())
    val state = _state

    init {
        viewModelScope.launch {
            textRecognizer.result.collectLatest {
                if (it.systolic > 0 && it.diastolic > 0) _state.value = CameraScreenState(it)
            }
        }
    }

    private val cameraPreviewUseCase = Preview.Builder().build().apply {
        setSurfaceProvider { newSurfaceRequest ->
            _surfaceRequest.update { newSurfaceRequest }
        }
    }
    private val analyzer = Analyzer { imageProxy ->
        textRecognizer.recognizeText(imageProxy)
        imageProxy.close()
    }

    fun bindToCamera(provider: ProcessCameraProvider, lifecycleOwner: LifecycleOwner) {
        textRecognizer.bindToCamera(
            provider = provider,
            lifecycleOwner = lifecycleOwner,
            analyzer = analyzer,
            previewUseCase = cameraPreviewUseCase
        )
    }
}

data class CameraScreenState(
    val data: ImageData? = null
)
