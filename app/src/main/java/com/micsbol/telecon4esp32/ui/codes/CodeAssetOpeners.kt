package com.micsbol.telecon4esp32.ui.codes

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.util.hostedPdfUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

enum class PdfOpenResult {
    Success,
    NoActivity,
    CopyFailed,
}

enum class ZipSharePrepareResult {
    Success,
    NoActivity,
    CopyFailed,
}

fun shareZipAssetExternally(
    context: Context,
    assetFileName: String,
    outputFileName: String,
): ZipSharePrepareResult {
    return try {
        val tempFile = File(context.cacheDir, outputFileName)
        context.assets.open(assetFileName).use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        shareZipFile(context, tempFile)
    } catch (_: IOException) {
        ZipSharePrepareResult.CopyFailed
    }
}

/**
 * Copies a bundled sketch or downloads the hosted ZIP into the cache.
 */
suspend fun materializeZip(context: Context, asset: CodeAssetInfo): File = withContext(Dispatchers.IO) {
    val outputFileName = asset.outputFileName
    val remoteUrl = asset.remoteZipUrl
    if (!remoteUrl.isNullOrBlank()) {
        downloadZipToCache(context, remoteUrl, outputFileName)
    } else {
        val assetFileName = asset.assetFileName
            ?: throw IOException("Sketch ZIP is not published")
        val tempFile = File(context.cacheDir, outputFileName)
        context.assets.open(assetFileName).use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        tempFile
    }
}

fun shareZipFile(context: Context, zipFile: File): ZipSharePrepareResult {
    return try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFile,
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(
            sendIntent,
            context.getString(R.string.codes_zip_share_chooser_title),
        ).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(chooser)
            ZipSharePrepareResult.Success
        } catch (_: ActivityNotFoundException) {
            ZipSharePrepareResult.NoActivity
        }
    } catch (_: IOException) {
        ZipSharePrepareResult.CopyFailed
    }
}

private fun downloadZipToCache(context: Context, url: String, outputFileName: String): File {
    val tempFile = File(context.cacheDir, outputFileName)
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        instanceFollowRedirects = true
        connectTimeout = 15_000
        readTimeout = 60_000
        setRequestProperty("User-Agent", "TeleCon4ESP32")
    }
    try {
        val code = connection.responseCode
        if (code !in 200..299) {
            throw IOException("Sketch download failed ($code)")
        }
        connection.inputStream.use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    } finally {
        connection.disconnect()
    }
    return tempFile
}

fun openPdfUrlExternally(context: Context, url: String): PdfOpenResult {
    return try {
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(hostedPdfUrl(url)))
        context.startActivity(viewIntent)
        PdfOpenResult.Success
    } catch (_: ActivityNotFoundException) {
        PdfOpenResult.NoActivity
    }
}

fun openPdfAssetExternally(context: Context, assetFileName: String): PdfOpenResult {
    return try {
        val tempFile = File(context.cacheDir, assetFileName)
        context.assets.open(assetFileName).use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile,
        )
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        }
        val chooser = Intent.createChooser(
            viewIntent,
            context.getString(R.string.codes_pdf_open_with),
        ).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(chooser)
            PdfOpenResult.Success
        } catch (_: ActivityNotFoundException) {
            PdfOpenResult.NoActivity
        }
    } catch (_: IOException) {
        PdfOpenResult.CopyFailed
    }
}

fun Context.launchPlayStorePdfReaderSearch() {
    val query = "pdf reader"
    val marketUri = Uri.parse(
        "market://search?q=${Uri.encode(query)}&c=apps",
    )
    val webUri = Uri.parse(
        "https://play.google.com/store/search?q=${Uri.encode(query)}&c=apps",
    )
    try {
        startActivity(Intent(Intent.ACTION_VIEW, marketUri))
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}
