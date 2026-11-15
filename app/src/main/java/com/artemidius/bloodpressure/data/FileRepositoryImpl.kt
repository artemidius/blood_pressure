package com.artemidius.bloodpressure.data

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class FileRepositoryImpl @Inject constructor() : FileRepository {
    override suspend fun saveFile(data: List<PressureData>, uri: Uri?, contentResolver: ContentResolver) {
        if (uri == null) return
        withContext(Dispatchers.IO) {
            val stream = contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot open export")
            stream.bufferedWriter(Charsets.UTF_8).use { it.write(pressureCsv(data)) }
        }
    }
}
