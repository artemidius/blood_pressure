package com.artemidius.bloodpressure.ml

import android.content.Context
import android.util.Log
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

interface ScanImageRepository {
    fun createPhoto(): Uri
    fun deletePhoto(uri: Uri)
    suspend fun recognize(uri: Uri): ImageData?
}

class ScanImageRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val recognizer: TextRecognizer
) : ScanImageRepository {
    override fun createPhoto(): Uri {
        val directory = File(context.cacheDir, "scan-photos").apply { mkdirs() }
        val file = File.createTempFile("monitor-", ".jpg", directory)
        return FileProvider.getUriForFile(context, "${context.packageName}.scanphotos", file)
    }

    override fun deletePhoto(uri: Uri) {
        // Only delete our own temporary camera captures, never gallery originals.
        if (uri.authority == "${context.packageName}.scanphotos") {
            runCatching { context.contentResolver.delete(uri, null, null) }
        }
    }

    override suspend fun recognize(uri: Uri): ImageData? = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodePhoto(context.contentResolver, uri)
            Log.d("PressureScanner", "Decoded image: ${bitmap.width}x${bitmap.height}")
            try { recognizer.recognize(bitmap, 0) }
            finally { bitmap.recycle() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            // Do not log URIs, image contents, or exception messages containing them.
            Log.w("PressureScanner", "Image recognition failed: ${failure.javaClass.simpleName}")
            throw failure
        }
    }
}
