package com.micsbol.telecon4esp32.ui.codes

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dock
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.ui.graphics.vector.ImageVector
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import java.util.Locale

private const val ESP32_BT_CONTROLLER_ZIP = "ESP32_BT_Controller-main.zip"

enum class CodeAssetType {
    Pdf,
    Zip,
}

/**
 * A published code/doc package.
 *
 * [boards] / [modes] restrict visibility to a settings combination.
 * `null` means the asset is shared (e.g. general docs) and can appear for any selection.
 */
data class CodeAssetInfo(
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val type: CodeAssetType,
    val assetFileName: String? = null,
    @StringRes val remoteUrlRes: Int? = null,
    val outputFileName: String = assetFileName ?: "document.pdf",
    val boards: Set<Esp32Board>? = null,
    val modes: Set<BluetoothConnectionMode>? = null,
    /** Optional device label for Kit B dual packages. */
    @StringRes val targetDeviceLabelRes: Int? = null,
) {
    fun matches(board: Esp32Board, mode: BluetoothConnectionMode): Boolean {
        if (boards != null && board !in boards) return false
        if (modes != null && mode !in modes) return false
        return true
    }

    val isPublished: Boolean
        get() = assetFileName != null || remoteUrlRes != null
}

fun codeAssetsFor(applicationId: ApplicationId, language: String): List<CodeAssetInfo> =
    when (applicationId) {
        ApplicationId.CONTROL_PANEL -> controlPanelCodeAssets(language)
        else -> emptyList()
    }

fun codeAssetsMatching(
    applicationId: ApplicationId,
    language: String,
    board: Esp32Board,
    mode: BluetoothConnectionMode,
): List<CodeAssetInfo> =
    codeAssetsFor(applicationId, language).filter { it.matches(board, mode) }

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
            boards = setOf(Esp32Board.DEV_KIT),
            modes = setOf(
                BluetoothConnectionMode.CLASSIC_SIMPLE,
                BluetoothConnectionMode.CLASSIC_BINARY,
            ),
            targetDeviceLabelRes = R.string.app_settings_device_dev_kit,
        ),
    )
}

fun currentCodeAssetLanguage(): String = Locale.getDefault().language
