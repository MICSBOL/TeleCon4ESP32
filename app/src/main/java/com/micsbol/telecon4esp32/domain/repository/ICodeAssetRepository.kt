package com.micsbol.telecon4esp32.domain.repository

import com.micsbol.telecon4esp32.domain.model.SavedCodeAsset

interface ICodeAssetRepository {
    suspend fun saveAssetToDownloads(
        assetFileName: String,
        outputFileName: String
    ): SavedCodeAsset
}
