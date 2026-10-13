package com.micsbol.telecon4esp32.ui.codes

import android.app.Activity
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
import java.security.MessageDigest

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
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
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
        viewPdfFile(context, tempFile)
    } catch (_: IOException) {
        PdfOpenResult.CopyFailed
    }
}

private const val DOC_VERSION_PREFS = "doc_pdf_versions"

/**
 * Opens a documentation PDF.
 * GitHub Pages is checked with If-None-Match, and the file is downloaded only when it changed.
 * The opened file name includes that version, so the phone viewer cannot keep showing the
 * copy it imported before the last install.
 */
suspend fun openPublishedPdf(context: Context, asset: CodeAssetInfo): PdfOpenResult {
    val file = withContext(Dispatchers.IO) {
        preparePublishedPdf(context, asset)
    } ?: return PdfOpenResult.CopyFailed
    return withContext(Dispatchers.Main) {
        viewPdfFile(context, file)
    }
}

private fun preparePublishedPdf(context: Context, asset: CodeAssetInfo): File? {
    val name = asset.assetFileName ?: return null
    val dir = File(context.filesDir, "docs").apply { mkdirs() }
    File(context.cacheDir, name).delete()
    val remote = asset.remotePdfUrl
    if (!remote.isNullOrBlank()) {
        try {
            val latest = downloadLatestPdf(context, dir, name, remote)
            deleteOlderPdfVersions(dir, name, latest)
            return latest
        } catch (_: IOException) {
            newestSavedPdf(dir, name)?.let { return it }
        }
    }
    val fallback = copyCurrentAsset(context, dir, name) ?: return null
    deleteOlderPdfVersions(dir, name, fallback)
    return fallback
}

private fun downloadLatestPdf(
    context: Context,
    dir: File,
    assetName: String,
    remoteUrl: String,
    conditional: Boolean = true,
): File {
    val prefs = context.applicationContext.getSharedPreferences(DOC_VERSION_PREFS, Context.MODE_PRIVATE)
    val storedEtag = prefs.getString(remoteUrl, null)
    val known = prefs.getString("$remoteUrl#file", null)
        ?.let { File(dir, it) }
        ?.takeIf { it.isFile && it.length() > 0L }
    val sendCondition = conditional && !storedEtag.isNullOrBlank() && known != null
    val connection = openPdfConnection(remoteUrl, "GET")
    if (sendCondition) {
        connection.setRequestProperty("If-None-Match", storedEtag)
    }
    try {
        val code = connection.responseCode
        if (code == HttpURLConnection.HTTP_NOT_MODIFIED) {
            if (known != null) return known
            if (!conditional) throw IOException("Saved guide is missing")
            connection.disconnect()
            return downloadLatestPdf(context, dir, assetName, remoteUrl, conditional = false)
        }
        if (code !in 200..299) {
            throw IOException("Guide download failed ($code)")
        }
        val etag = responseHeader(connection, "ETag")
            ?: responseHeader(connection, "Last-Modified")
        val expected = connection.contentLengthLong
        val tmp = File(dir, "$assetName.part")
        connection.inputStream.use { inputStream ->
            FileOutputStream(tmp).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        if (expected > 0L && tmp.length() != expected) {
            throw IOException("Guide download was incomplete")
        }
        if (!isPdf(tmp)) {
            throw IOException("Guide download was not a PDF")
        }
        val token = hashFile(tmp)
        val dest = versionedPdf(dir, assetName, token)
        replaceFile(tmp, dest)
        prefs.edit()
            .putString(remoteUrl, etag ?: token)
            .putString("$remoteUrl#file", dest.name)
            .apply()
        return dest
    } catch (error: IOException) {
        File(dir, "$assetName.part").delete()
        throw error
    } finally {
        connection.disconnect()
    }
}

private fun versionedPdf(dir: File, assetName: String, token: String?): File {
    if (token.isNullOrBlank()) return File(dir, assetName)
    val safe = token.filter { it.isLetterOrDigit() }
    val dot = assetName.lastIndexOf('.')
    val base = if (dot > 0) assetName.substring(0, dot) else assetName
    val ext = if (dot > 0) assetName.substring(dot) else ""
    return File(dir, "$base-$safe$ext")
}

private fun newestSavedPdf(dir: File, assetName: String): File? {
    val dot = assetName.lastIndexOf('.')
    val base = if (dot > 0) assetName.substring(0, dot) else assetName
    return dir.listFiles()
        ?.filter { file ->
            file.isFile &&
                file.length() > 0L &&
                file.name.startsWith("$base-") &&
                file.name.endsWith(".pdf")
        }
        ?.maxByOrNull { it.lastModified() }
}

private fun deleteOlderPdfVersions(dir: File, assetName: String, keep: File) {
    val dot = assetName.lastIndexOf('.')
    val base = if (dot > 0) assetName.substring(0, dot) else assetName
    dir.listFiles()?.forEach { file ->
        if (file.absolutePath == keep.absolutePath) return@forEach
        val sameGuide = file.name == assetName ||
            file.name.startsWith("$base-") ||
            file.name == "$assetName.part"
        if (sameGuide) file.delete()
    }
}

private fun copyCurrentAsset(context: Context, dir: File, assetName: String): File? {
    val tmp = File(dir, "$assetName.part")
    val digest = MessageDigest.getInstance("SHA-256")
    try {
        context.assets.open(assetName).use { inputStream ->
            FileOutputStream(tmp).use { outputStream ->
                val buffer = ByteArray(8192)
                while (true) {
                    val read = inputStream.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                    outputStream.write(buffer, 0, read)
                }
            }
        }
    } catch (_: IOException) {
        tmp.delete()
        return null
    }
    val token = digest.digest().take(6).joinToString("") { "%02x".format(it) }
    val dest = versionedPdf(dir, assetName, token)
    if (dest.isFile && dest.length() > 0L) {
        tmp.delete()
        return dest
    }
    return try {
        replaceFile(tmp, dest)
        dest.takeIf { it.isFile && it.length() > 0L }
    } catch (_: IOException) {
        tmp.delete()
        null
    }
}

private fun replaceFile(tmp: File, dest: File) {
    if (dest.exists() && dest.absolutePath != tmp.absolutePath && !dest.delete()) {
        throw IOException("Could not replace the saved guide")
    }
    if (tmp.absolutePath == dest.absolutePath) return
    if (!tmp.renameTo(dest)) {
        tmp.copyTo(dest, overwrite = true)
        if (!tmp.delete()) tmp.deleteOnExit()
    }
}

private fun isPdf(file: File): Boolean {
    if (file.length() < 5L) return false
    return file.inputStream().use { input ->
        val magic = ByteArray(4)
        input.read(magic) == 4 &&
            magic[0] == '%'.code.toByte() &&
            magic[1] == 'P'.code.toByte() &&
            magic[2] == 'D'.code.toByte() &&
            magic[3] == 'F'.code.toByte()
    }
}

private fun hashFile(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().take(6).joinToString("") { "%02x".format(it) }
}

private fun responseHeader(connection: HttpURLConnection, name: String): String? {
    connection.getHeaderField(name)?.takeIf { it.isNotBlank() }?.let { return it }
    val values = connection.headerFields.entries
        .firstOrNull { it.key?.equals(name, ignoreCase = true) == true }
        ?.value
    return values?.firstOrNull { it.isNotBlank() }
}

private fun openPdfConnection(url: String, method: String): HttpURLConnection {
    return (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        instanceFollowRedirects = true
        useCaches = false
        connectTimeout = 12_000
        readTimeout = 60_000
        setRequestProperty("User-Agent", "TeleCon4ESP32")
        setRequestProperty("Accept-Encoding", "identity")
        setRequestProperty("Cache-Control", "no-cache")
        setRequestProperty("Pragma", "no-cache")
    }
}

private fun viewPdfFile(context: Context, file: File): PdfOpenResult {
    return try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(
            viewIntent,
            context.getString(R.string.codes_pdf_open_with),
        ).apply {
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(chooser)
            PdfOpenResult.Success
        } catch (_: ActivityNotFoundException) {
            PdfOpenResult.NoActivity
        }
    } catch (_: IllegalArgumentException) {
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
