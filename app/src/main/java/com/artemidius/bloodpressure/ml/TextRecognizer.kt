package com.artemidius.bloodpressure.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sevendigits.RecognitionOptions
import dev.sevendigits.SevenDigitsRecognizer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

interface TextRecognizer {
    /** The caller owns the bitmap and must keep it alive until this suspend call finishes. */
    suspend fun recognize(bitmap: Bitmap, rotationDegrees: Int): ImageData?
}

class TextRecognizerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : TextRecognizer {
    private val initialization = Mutex()
    private var scanner: SevenDigitsRecognizer? = null

    override suspend fun recognize(bitmap: Bitmap, rotationDegrees: Int): ImageData? {
        val recognizer = initialization.withLock {
            scanner ?: SevenDigitsRecognizer.create(context).also { scanner = it }
        }
        val options = RecognitionOptions(rotationDegrees = rotationDegrees)
        // Also enforce the limit for callers that supply a bitmap directly.
        // Never retry at a larger size: recognition is scale-sensitive and the
        // bounded image avoids the library's expensive extra zoom passes.
        val preview = bitmap.limitScanSize()
        Log.d("PressureScanner", "Recognition preview: ${preview.width}x${preview.height}")
        val visual = try {
            recognizer.recognizeVisual(preview, options)
        } finally { if (preview !== bitmap) preview.recycle() }
        val extraction = PressureReadingExtractor.extract(visual, rotationDegrees)
        Log.d("PressureScanner", "Recognition completed: rows=${visual.lines.size}, fields=${visual.regions.size}, " +
            "outcome=${extraction.outcome}, candidates=${extraction.candidateCount}, " +
            "score=${extraction.bestScore}, margin=${extraction.margin}")
        if (extraction.reading == null) {
            visual.lines.forEachIndexed { row, fields -> fields.forEachIndexed { field, region ->
                // Describe rejected OCR structure without logging the recognized words or values.
                val shape = region.text.map { c -> when {
                    c.isDigit() -> 'D'
                    c.isLetter() -> 'L'
                    c in ".,:;-/+" -> c
                    else -> '?'
                } }.joinToString("")
                Log.d("PressureScanner", "Unmatched field: row=$row field=$field shape=$shape " +
                    "box=${region.boundingBox} minCharacterScore=${region.minimumCharacterConfidence}")
            } }
        }
        return extraction.reading
    }
}
