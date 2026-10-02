package com.micsbol.telecon4esp32.domain.use_case

import com.micsbol.telecon4esp32.domain.repository.ICodeAssetRepository
import java.io.File
import javax.inject.Inject

class SaveCodeAssetUseCase @Inject constructor(
    private val repository: ICodeAssetRepository
) {
    suspend operator fun invoke(
        assetFileName: String,
        outputFileName: String
    ) = repository.saveAssetToDownloads(assetFileName, outputFileName)

    suspend fun saveFile(
        source: File,
        outputFileName: String,
    ) = repository.saveFileToDownloads(source, outputFileName)
}
