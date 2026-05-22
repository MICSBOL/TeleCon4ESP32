package com.micsbol.emitterapp.domain.use_case

import com.micsbol.emitterapp.domain.repository.ICodeAssetRepository
import javax.inject.Inject

class SaveCodeAssetUseCase @Inject constructor(
    private val repository: ICodeAssetRepository
) {
    suspend operator fun invoke(
        assetFileName: String,
        outputFileName: String
    ) = repository.saveAssetToDownloads(assetFileName, outputFileName)
}
