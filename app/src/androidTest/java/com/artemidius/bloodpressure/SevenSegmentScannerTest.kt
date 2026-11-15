package com.artemidius.bloodpressure

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import com.artemidius.bloodpressure.ml.ScanImageRepositoryImpl
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artemidius.bloodpressure.ml.ImageData
import com.artemidius.bloodpressure.ml.TextRecognizerImpl
import com.artemidius.bloodpressure.ml.TextRecognizer
import com.artemidius.bloodpressure.ml.PressureReadingExtractor
import com.artemidius.bloodpressure.ml.limitScanSize
import dev.sevendigits.SevenDigitsRecognizer
import dev.sevendigits.RecognitionResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SevenSegmentScannerTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun recognizesPressureAndIgnoresPulseAndClockInMonitorPhotos() = runBlocking {
        val scanner = TextRecognizerImpl(instrumentation.targetContext)
        for (name in listOf("126-81-81-a.jpg", "126-81-81-b.jpg")) {
            val bitmap = fixture(name)
            try {
                assertEquals(name, ImageData(126, 81), scanner.recognize(bitmap, 0))
                assertFalse(bitmap.isRecycled)
            } finally { bitmap.recycle() }
        }
    }

    @Test fun honorsCameraRotationBeforeMappingPressure() = runBlocking {
        val scanner = TextRecognizerImpl(instrumentation.targetContext)
        val upright = fixture("126-81-81-a.jpg")
        val sideways = Bitmap.createBitmap(upright, 0, 0, upright.width, upright.height,
            Matrix().apply { postRotate(-90f) }, false)
        try {
            assertEquals(ImageData(126, 81), scanner.recognize(sideways, 90))
        } finally { sideways.recycle(); upright.recycle() }
    }

    @Test fun recognizesSmallerMonitorPhotos() = runBlocking {
        val scanner = TextRecognizerImpl(instrumentation.targetContext)
        for (name in listOf("126-81-81-a.jpg", "126-81-81-b.jpg")) {
            val original = fixture(name)
            val frame = Bitmap.createScaledBitmap(original, 960, 1280, true)
            try {
                assertEquals(name, ImageData(126, 81), scanner.recognize(frame, 0))
            } finally { frame.recycle(); original.recycle() }
        }
    }

    @Test fun decodesCapturedPhotoWithExifRotationAndCleansTemporaryFile() = runBlocking {
        val context = instrumentation.targetContext
        val images = ScanImageRepositoryImpl(context, TextRecognizerImpl(context))
        val uri = images.createPhoto()
        val upright = fixture("126-81-81-a.jpg")
        val sideways = Bitmap.createBitmap(upright, 0, 0, upright.width, upright.height,
            Matrix().apply { postRotate(-90f) }, false)
        try {
            context.contentResolver.openOutputStream(uri)!!.use { sideways.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            context.contentResolver.openFileDescriptor(uri, "rw")!!.use {
                ExifInterface(it.fileDescriptor).apply {
                    setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                    saveAttributes()
                }
            }
            assertEquals(ImageData(126, 81), images.recognize(uri))
        } finally { images.deletePhoto(uri); sideways.recycle(); upright.recycle() }
        assertFalse(java.io.File(context.cacheDir, "scan-photos/${uri.lastPathSegment}").exists())
    }

    @Test fun assignsPressureFromActualDetectorBoxesWithoutPrintedLabels() = runBlocking {
        val engine = SevenDigitsRecognizer.create(instrumentation.targetContext)
        for (name in listOf("126-81-81-a.jpg", "126-81-81-b.jpg")) {
            val bitmap = fixture(name)
            try {
                val visual = engine.recognizeVisual(bitmap)
                val numericRows = visual.lines.map { row -> row.filter { it.text.matches(Regex("[0-9]+")) } }
                    .filter { it.isNotEmpty() }
                val unlabelled = RecognitionResult(visual.imageWidth, visual.imageHeight, numericRows)
                assertEquals(name, ImageData(126, 81), PressureReadingExtractor.extract(unlabelled).reading)
            } finally { bitmap.recycle() }
        }
    }

    @Test fun decodingLimitsLargePhotosAndPreservesSmallerDimensions() = runBlocking {
        val context = instrumentation.targetContext
        var decodedSize = 0 to 0
        val images = ScanImageRepositoryImpl(context, object : TextRecognizer {
            override suspend fun recognize(bitmap: Bitmap, rotationDegrees: Int): ImageData? {
                decodedSize = bitmap.width to bitmap.height
                return null
            }
        })
        for ((size, expected) in listOf(
            (800 to 600) to (800 to 600),
            (960 to 1280) to (960 to 1280),
            (1281 to 960) to (1280 to 959),
            (2040 to 1530) to (1280 to 960),
            (1530 to 2040) to (960 to 1280),
            (2560 to 2560) to (1280 to 1280),
            (4096 to 3072) to (1280 to 960)
        )) {
            val uri = images.createPhoto()
            val bitmap = Bitmap.createBitmap(size.first, size.second, Bitmap.Config.ARGB_8888)
            try {
                context.contentResolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                images.recognize(uri)
                assertEquals(expected, decodedSize)
            } finally { bitmap.recycle(); images.deletePhoto(uri) }
        }
    }

    @Test fun smallerBitmapIsReturnedWithoutResamplingOrRecycling() {
        val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        try {
            assertSame(bitmap, bitmap.limitScanSize())
            assertFalse(bitmap.isRecycled)
        } finally { bitmap.recycle() }
    }

    private fun fixture(name: String): Bitmap =
        instrumentation.context.assets.open("monitor-fixtures/$name").use {
            requireNotNull(BitmapFactory.decodeStream(it))
        }
}
