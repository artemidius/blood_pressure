package com.artemidius.bloodpressure.bitmap

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import kotlin.math.min

/**
 * These are some extensions to help recognizer with image OCR. Makes text more "machine-readable"
 */

fun Bitmap.toGrayscale(): Bitmap {
    val output = createBitmap(width, height, config!!)
    Canvas(output).apply {
        drawBitmap(this@toGrayscale, 0f, 0f, Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                setSaturation(0f)
            })
        })
    }
    return output
}

fun Bitmap.adjustContrast(scale: Float = 1.5f): Bitmap {
    val output = createBitmap(width, height, config!!)
    val translate = (-0.5f * scale + 0.5f) * 255
    Canvas(output).apply {
        drawBitmap(this@adjustContrast, 0f, 0f, Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                scale, 0f,   0f,   0f, translate,
                0f,   scale, 0f,   0f, translate,
                0f,   0f,   scale, 0f, translate,
                0f,   0f,   0f,   1f, 0f
            )))
        })
    }
    return output
}

fun Bitmap.optimizeResolution(): Bitmap {
    val scale = min(1080f/width, 1920f/height)
    return this.scale((width * scale).toInt(), (height * scale).toInt())
}
