package com.artemidius.bloodpressure.data

import android.content.ContentResolver
import android.net.Uri
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

class FileRepositoryImpl @Inject constructor() : FileRepository {
    override suspend fun saveFile(
        data: List<PressureData>,
        uri: Uri?,
        contentResolver: ContentResolver
    ) {
        try {
            contentResolver.openFileDescriptor(uri!!, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { stream ->
                    data.forEach { line ->
                        stream.write(line.toCsvLine().toByteArray())
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}