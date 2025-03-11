package com.artemidius.bloodpressure.data

import android.content.ContentResolver
import android.net.Uri
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

/**
 * Interface for interacting with file storage operations.
 * This interface provides methods for saving data to files.
 */
interface FileRepository {
    /**
     * Saves a list of [PressureData] to a file specified by the given URI.
     *
     * This function writes the provided pressure data to a file accessed via a content URI.
     * It formats the data as a CSV (Comma-Separated Values) string, where each line
     * represents a single data point with timestamp and pressure value separated by a comma.
     * The file will be overwritten if it already exists at the given URI.
     *
     * @param data The list of [PressureData] to be saved.
     * @param uri The [Uri] representing the file location where the data will be saved.
     * @param contentResolver The [ContentResolver] used to access the file at the given URI.
     */
    suspend fun saveFile(data: List<PressureData>, uri: Uri?, contentResolver: ContentResolver)
}

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
