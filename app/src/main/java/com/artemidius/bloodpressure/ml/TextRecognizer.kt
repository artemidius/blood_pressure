package com.artemidius.bloodpressure.ml

import android.content.res.AssetManager
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.Analyzer
import androidx.camera.core.ImageProxy
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import com.artemidius.bloodpressure.bitmap.adjustContrast
import com.artemidius.bloodpressure.bitmap.optimizeResolution
import com.artemidius.bloodpressure.bitmap.toGrayscale
import com.google.android.gms.tasks.OnSuccessListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * Interface for recognizing text from camera.
 */
interface TextRecognizer {
    /**
     * Recognizes text from an image captured by the camera.
     * This function processes an [ImageProxy] containing an image frame and attempts
     * to extract text from it. The recognized text is then emitted to [result] flow.
     */
    fun recognizeText(imageProxy: ImageProxy)
    /**
     * Binds the given camera use cases to the camera and lifecycle.
     *
     * This function is responsible for connecting the camera preview and image analyzer.
     * It ensures that the camera is started and stopped appropriately based on the lifecycle state.
     * It also handles unbinding any previous use cases before binding the new ones, ensuring a clean transition.
     */
    fun bindToCamera(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        analyzer: Analyzer,
        previewUseCase: UseCase
    )
    /**
     * A [SharedFlow] emitting [ImageData] objects representing the result of an image processing operation.
     * This flow will emit new [ImageData] objects whenever a new result is available.
     */
    val result: SharedFlow<ImageData>
}

class TextRecognizerImpl @Inject constructor(assetManager: AssetManager) : TextRecognizer {
    val scope = CoroutineScope(Dispatchers.IO)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val _recognitionResult = MutableSharedFlow<ImageData>()
    override val result: SharedFlow<ImageData> = _recognitionResult

    override fun recognizeText(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap().optimizeResolution()
        val processedBitmap = bitmap
            .toGrayscale()
            .adjustContrast(1.4f)
        val mediaImage =
            InputImage.fromBitmap(processedBitmap, imageProxy.imageInfo.rotationDegrees)
        val listener = OnSuccessListener<Text?> { result ->
            scope.launch {
                _recognitionResult.emit(ImageData(0, 0))
                val numbers = mutableListOf<Int>()
                result?.textBlocks?.let { blocks ->
                    blocks.forEach {
                        val currentNums: List<Int> = it.text.split(" ")
                            .mapNotNull { it.toIntOrNull() }
                            .filter { it in 60..280 }
                        if (currentNums.isNotEmpty()) {
                            println(currentNums)
                        }
                        numbers.addAll(currentNums)
                    }
                }
                if (numbers.size == 2) {
                    numbers.sortDescending()
                    _recognitionResult.emit(ImageData(numbers[0], numbers[1]))
                }
            }
        }
        val process = recognizer.process(mediaImage)
        process.addOnSuccessListener(listener)
    }

    override fun bindToCamera(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        analyzer: Analyzer,
        previewUseCase: UseCase
    ) {
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer)

        provider.bindToLifecycle(
            lifecycleOwner = lifecycleOwner,
            cameraSelector = DEFAULT_BACK_CAMERA,
            previewUseCase,
            imageAnalysis
        )
    }
}

data class ImageData(
    val systolic: Int,
    val diastolic: Int
)
