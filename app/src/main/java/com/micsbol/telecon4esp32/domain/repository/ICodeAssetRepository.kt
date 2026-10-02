package com.micsbol.telecon4esp32.domain.repository

import com.micsbol.telecon4esp32.domain.model.SavedCodeAsset
import java.io.File

interface ICodeAssetRepository {
    suspend fun saveAssetToDownloads(
        assetFileName: String,
        outputFileName: String
    ): SavedCodeAsset

    suspend fun saveFileToDownloads(
        source: File,
        outputFileName: String,
    ): SavedCodeAsset
}
