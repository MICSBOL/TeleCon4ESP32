package com.micsbol.telecon4esp32.data.session

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.micsbol.telecon4esp32.domain.repository.ISessionCsvRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SessionCsv"
private const val CSV_MIME_TYPE = "text/csv"
private const val PLAIN_MIME_TYPE = "text/plain"

@Singleton
class AndroidSessionCsvRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : ISessionCsvRepository {

    override fun createCacheFile(fileName: String): File {
        val file = File(context.cacheDir, fileName)
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()
        return file
    }

    override suspend fun saveToDownloads(source: File, outputFileName: String): String {
        return withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToPublicDownloads(source, outputFileName)
            } else {
                saveToLegacyDownloads(source, outputFileName)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToPublicDownloads(source: File, outputFileName: String): String {
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/"
        val collections = listOf(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
        )
        val mimeTypes = listOf(CSV_MIME_TYPE, PLAIN_MIME_TYPE)
        var lastError: Throwable? = null
        for (collection in collections) {
            for (mimeType in mimeTypes) {
                runCatching {
                    insertAndWrite(collection, source, outputFileName, relativePath, mimeType)
                }.onSuccess { location ->
                    return location
                }.onFailure { error ->
                    lastError = error
                    Log.w(TAG, "MediaStore save failed ($mimeType via $collection)", error)
                }
            }
        }
        throw lastError ?: IllegalStateException("Could not create $outputFileName in Downloads")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun insertAndWrite(
        collection: Uri,
        source: File,
        outputFileName: String,
        relativePath: String,
        mimeType: String,
    ): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, outputFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val outputUri = resolver.insert(collection, values)
            ?: error("Could not create $outputFileName in Downloads")
        try {
            FileInputStream(source).use { inputStream ->
                resolver.openOutputStream(outputUri)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                } ?: error("Could not open $outputFileName for writing")
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(outputUri, values, null, null)
            return "$relativePath$outputFileName"
        } catch (throwable: Throwable) {
            resolver.delete(outputUri, null, null)
            throw throwable
        }
    }

    private fun saveToLegacyDownloads(source: File, outputFileName: String): String {
        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val directory = if (publicDir != null && (publicDir.exists() || publicDir.mkdirs()) && publicDir.canWrite()) {
            publicDir
        } else {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(context.filesDir, Environment.DIRECTORY_DOWNLOADS)
        }
        if (!directory.exists()) {
            check(directory.mkdirs()) { "Could not create ${directory.absolutePath}" }
        }
        val dest = File(directory, outputFileName)
        FileInputStream(source).use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
        MediaScannerConnection.scanFile(
            context,
            arrayOf(dest.absolutePath),
            arrayOf(CSV_MIME_TYPE),
            null,
        )
        return dest.absolutePath
    }
}
