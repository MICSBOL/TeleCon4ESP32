package com.micsbol.telecon4esp32.ui.codes

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dock
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.ui.graphics.vector.ImageVector
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import java.util.Locale

private const val ESP32_BT_CONTROLLER_ZIP = "ESP32_BT_Controller-main.zip"

enum class CodeAssetType {
    Pdf,
    Zip,
}

data class CodeAssetInfo(
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val type: CodeAssetType,
    val assetFileName: String? = null,
    @StringRes val remoteUrlRes: Int? = null,
    val outputFileName: String = assetFileName ?: "document.pdf",
)

fun codeAssetsFor(applicationId: ApplicationId, language: String): List<CodeAssetInfo> =
    when (applicationId) {
        ApplicationId.CONTROL_PANEL -> controlPanelCodeAssets(language)
        else -> emptyList()
    }

private fun controlPanelCodeAssets(language: String): List<CodeAssetInfo> {
    val fastGuide = if (language == "es") {
        CodeAssetInfo(
            titleRes = R.string.codes_document_fast_guide,
            icon = Icons.Default.Dock,
            assetFileName = "fast_guide_esp.pdf",
            type = CodeAssetType.Pdf,
        )
    } else {
        CodeAssetInfo(
            titleRes = R.string.codes_document_fast_guide,
            icon = Icons.Default.Dock,
            assetFileName = "fast_guide_eng.pdf",
            type = CodeAssetType.Pdf,
        )
    }

    val generalDocumentation = if (language == "es") {
        CodeAssetInfo(
            titleRes = R.string.codes_document_general,
            icon = Icons.Default.DocumentScanner,
            type = CodeAssetType.Pdf,
            remoteUrlRes = R.string.documentation_pdf_es_url,
        )
    } else {
        CodeAssetInfo(
            titleRes = R.string.codes_document_general,
            icon = Icons.Default.DocumentScanner,
            type = CodeAssetType.Pdf,
            remoteUrlRes = R.string.documentation_pdf_en_url,
        )
    }

    return listOf(
        generalDocumentation,
        fastGuide,
        CodeAssetInfo(
            titleRes = R.string.codes_esp32_bt_controller_zip,
            icon = Icons.Default.Code,
            assetFileName = ESP32_BT_CONTROLLER_ZIP,
            type = CodeAssetType.Zip,
        ),
    )
}

fun currentCodeAssetLanguage(): String = Locale.getDefault().language
