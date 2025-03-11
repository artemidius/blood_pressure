package com.artemidius.bloodpressure

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import kotlin.math.min
import androidx.core.graphics.scale
import androidx.core.graphics.set

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

fun Bitmap.binarize(threshold: Int = 128): Bitmap {
    val output = createBitmap(width, height, config!!)
    for (x in 0 until width) {
        for (y in 0 until height) {
            val pixel = getPixel(x, y)
            val luminance = (Color.red(pixel)*0.299 + Color.green(pixel)*0.587 + Color.blue(pixel)*0.114).toInt()
            output[x, y] = if (luminance < threshold) Color.BLACK else Color.WHITE
        }
    }
    return output
}


