package com.micsbol.telecon4esp32.util

import com.micsbol.telecon4esp32.domain.repository.ISessionCsvRepository
import java.io.File

class FakeSessionCsvRepository(
    private val directory: File = File(System.getProperty("java.io.tmpdir"), "telecon_csv_test"),
) : ISessionCsvRepository {
    val savedDownloads = mutableListOf<File>()

    override fun createCacheFile(fileName: String): File {
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, fileName)
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()
        return file
    }

    override suspend fun saveToDownloads(source: File, outputFileName: String): String {
        val dest = File(directory, "downloads_$outputFileName")
        source.copyTo(dest, overwrite = true)
        savedDownloads += dest
        return dest.absolutePath
    }
}
