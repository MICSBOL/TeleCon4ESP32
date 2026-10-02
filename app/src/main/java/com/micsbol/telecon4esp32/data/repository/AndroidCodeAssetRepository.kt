package com.micsbol.telecon4esp32.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.micsbol.telecon4esp32.domain.model.SavedCodeAsset
import com.micsbol.telecon4esp32.domain.repository.ICodeAssetRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val ZIP_MIME_TYPE = "application/zip"
private const val DOWNLOADS_RELATIVE_PATH = "Download/"

@Singleton
class AndroidCodeAssetRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : ICodeAssetRepository {

    override suspend fun saveAssetToDownloads(
        assetFileName: String,
        outputFileName: String
    ): SavedCodeAsset = withContext(Dispatchers.IO) {
        context.assets.open(assetFileName).use { inputStream ->
            writeDownload(inputStream, outputFileName)
        }
    }

    override suspend fun saveFileToDownloads(
        source: File,
        outputFileName: String,
    ): SavedCodeAsset = withContext(Dispatchers.IO) {
        source.inputStream().use { inputStream ->
            writeDownload(inputStream, outputFileName)
        }
    }

    private fun writeDownload(inputStream: InputStream, outputFileName: String): SavedCodeAsset {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToPublicDownloads(inputStream, outputFileName)
        } else {
            saveToAppDownloads(inputStream, outputFileName)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToPublicDownloads(
        inputStream: InputStream,
        outputFileName: String
    ): SavedCodeAsset {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val existingUri = findExistingDownload(collection, outputFileName)
        val isNewFile = existingUri == null
        val outputUri = existingUri ?: resolver.insert(
            collection,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, outputFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, ZIP_MIME_TYPE)
                put(MediaStore.MediaColumns.RELATIVE_PATH, DOWNLOADS_RELATIVE_PATH)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        ) ?: error("Could not create $outputFileName in Downloads")

        try {
            resolver.openOutputStream(outputUri, "wt").use { outputStream ->
                requireNotNull(outputStream) { "Could not open $outputFileName for writing" }
                inputStream.copyTo(outputStream)
            }

            if (isNewFile) {
                resolver.update(
                    outputUri,
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    },
                    null,
                    null
                )
            }
        } catch (throwable: Throwable) {
            if (isNewFile) {
                resolver.delete(outputUri, null, null)
            }
            throw throwable
        }

        return SavedCodeAsset(
            fileName = outputFileName,
            location = "$DOWNLOADS_RELATIVE_PATH$outputFileName"
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun findExistingDownload(
        collection: Uri,
        outputFileName: String
    ): Uri? {
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND " +
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf(outputFileName, DOWNLOADS_RELATIVE_PATH)

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        ).use { cursor ->
            if (cursor == null || !cursor.moveToFirst()) return null

            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            return ContentUris.withAppendedId(collection, cursor.getLong(idColumn))
        }
    }

    private fun saveToAppDownloads(
        inputStream: InputStream,
        outputFileName: String
    ): SavedCodeAsset {
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, Environment.DIRECTORY_DOWNLOADS)

        if (!directory.exists()) {
            check(directory.mkdirs()) { "Could not create ${directory.absolutePath}" }
        }

        val outputFile = File(directory, outputFileName)
        FileOutputStream(outputFile, false).use { outputStream ->
            inputStream.copyTo(outputStream)
        }

        return SavedCodeAsset(
            fileName = outputFileName,
            location = outputFile.absolutePath
        )
    }
}
