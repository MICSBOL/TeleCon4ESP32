package com.example.emitterapp.domain.repository

import com.example.emitterapp.domain.model.SavedCodeAsset

interface ICodeAssetRepository {
    suspend fun saveAssetToDownloads(
        assetFileName: String,
        outputFileName: String
    ): SavedCodeAsset
}
