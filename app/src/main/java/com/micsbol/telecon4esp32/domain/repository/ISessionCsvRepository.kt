package com.micsbol.telecon4esp32.domain.repository

import java.io.File

interface ISessionCsvRepository {
    fun createCacheFile(fileName: String): File
    suspend fun saveToDownloads(source: File, outputFileName: String): String
}
