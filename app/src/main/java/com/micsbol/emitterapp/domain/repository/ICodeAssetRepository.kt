package com.micsbol.emitterapp.domain.repository

import com.micsbol.emitterapp.domain.model.SavedCodeAsset

interface ICodeAssetRepository {
    suspend fun saveAssetToDownloads(
        assetFileName: String,
        outputFileName: String
    ): SavedCodeAsset
}
