package com.micsbol.telecon4esp32.ui.codes

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dock
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.ui.graphics.vector.ImageVector
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.camera.CameraLinkProfile
import com.micsbol.telecon4esp32.domain.camera.resolveCameraLinkProfile
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import java.util.Locale

private const val ESP32_BT_CONTROLLER_ZIP = "ESP32_BT_Controller-main.zip"

object ControlPanelCodePackageId {
    const val DEVKIT_CLASSIC = "DEVKIT_CLASSIC"
    const val DEVKIT_BLE = "DEVKIT_BLE"
    /** Role A video-only CAM. Never list [CAM_WIFI_SIMPLE] for this role. */
    const val CAM_SOFTAP_VIDEO = "CAM_SOFTAP_VIDEO"
    /** Role B one-CAM SoftAP Simple (video + TCP). */
    const val CAM_WIFI_SIMPLE = "CAM_WIFI_SIMPLE"
    const val CAM_WIFI_BINARY = "CAM_WIFI_BINARY"
}

enum class CodeAssetType {
    Pdf,
    Zip,
}

/**
 * A published or coming-soon code/doc package.
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
    /** Optional device label for dual-board (Role A) packages. */
    @StringRes val targetDeviceLabelRes: Int? = null,
    val id: String? = null,
    /** Role A video-only CAM sketch — hidden unless SoftAP overlay + Bluetooth. */
    val roleAVideoOnly: Boolean = false,
    /** Role B one-CAM TCP sketch — hidden for Role A overlay. */
    val roleBCamTcpOnly: Boolean = false,
) {
    fun matches(
        board: Esp32Board,
        mode: BluetoothConnectionMode,
        profile: CameraLinkProfile = CameraLinkProfile.CONTROL_ONLY,
    ): Boolean {
        val controlBoard = board.normalizedControlBoard()
        if (boards != null && board !in boards && controlBoard !in boards) return false
        if (modes != null && mode !in modes) return false
        if (roleAVideoOnly && profile != CameraLinkProfile.WIFI_CAMERA_DEVKIT_BT) return false
        if (roleBCamTcpOnly && profile != CameraLinkProfile.WIFI_SOFTAP) return false
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
    useSoftApCamera: Boolean = false,
): List<CodeAssetInfo> {
    val profile = resolveCameraLinkProfile(applicationId, board, mode, useSoftApCamera)
    return codeAssetsFor(applicationId, language).filter { it.matches(board, mode, profile) }
}

fun controlPanelCodePackageIds(
    board: Esp32Board,
    mode: BluetoothConnectionMode,
    useSoftApCamera: Boolean = false,
): List<String> = codeAssetsMatching(
    ApplicationId.CONTROL_PANEL,
    language = "en",
    board = board,
    mode = mode,
    useSoftApCamera = useSoftApCamera,
).mapNotNull { it.id }

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

    val bluetoothModes = setOf(
        BluetoothConnectionMode.CLASSIC_SIMPLE,
        BluetoothConnectionMode.CLASSIC_BINARY,
        BluetoothConnectionMode.BLE_BINARY,
    )
    val dualBoards = setOf(Esp32Board.DEV_KIT, Esp32Board.CAM_AND_DEV_KIT)

    return listOf(
        generalDocumentation,
        fastGuide,
        CodeAssetInfo(
            titleRes = R.string.codes_esp32_bt_controller_zip,
            icon = Icons.Default.Code,
            assetFileName = ESP32_BT_CONTROLLER_ZIP,
            type = CodeAssetType.Zip,
            boards = dualBoards,
            modes = setOf(
                BluetoothConnectionMode.CLASSIC_SIMPLE,
                BluetoothConnectionMode.CLASSIC_BINARY,
            ),
            targetDeviceLabelRes = R.string.app_settings_device_dev_kit,
            id = ControlPanelCodePackageId.DEVKIT_CLASSIC,
        ),
        CodeAssetInfo(
            titleRes = R.string.codes_esp32_ble_binary,
            icon = Icons.Default.Code,
            type = CodeAssetType.Zip,
            boards = dualBoards,
            modes = setOf(BluetoothConnectionMode.BLE_BINARY),
            targetDeviceLabelRes = R.string.app_settings_device_dev_kit,
            id = ControlPanelCodePackageId.DEVKIT_BLE,
        ),
        CodeAssetInfo(
            titleRes = R.string.codes_esp32_cam_softap_video,
            icon = Icons.Default.Code,
            type = CodeAssetType.Zip,
            boards = dualBoards,
            modes = bluetoothModes,
            targetDeviceLabelRes = R.string.app_settings_device_cam,
            id = ControlPanelCodePackageId.CAM_SOFTAP_VIDEO,
            roleAVideoOnly = true,
        ),
        CodeAssetInfo(
            titleRes = R.string.codes_esp32_cam_wifi_simple,
            icon = Icons.Default.Code,
            type = CodeAssetType.Zip,
            boards = setOf(Esp32Board.CAM),
            modes = setOf(BluetoothConnectionMode.WIFI_CAM_STARTER),
            targetDeviceLabelRes = R.string.app_settings_device_cam,
            id = ControlPanelCodePackageId.CAM_WIFI_SIMPLE,
            roleBCamTcpOnly = true,
        ),
        CodeAssetInfo(
            titleRes = R.string.codes_esp32_cam_wifi_binary,
            icon = Icons.Default.Code,
            type = CodeAssetType.Zip,
            boards = setOf(Esp32Board.CAM),
            modes = setOf(
                BluetoothConnectionMode.WIFI_BINARY,
                BluetoothConnectionMode.WIFI_SOFTAP,
            ),
            targetDeviceLabelRes = R.string.app_settings_device_cam,
            id = ControlPanelCodePackageId.CAM_WIFI_BINARY,
            roleBCamTcpOnly = true,
        ),
    )
}

fun currentCodeAssetLanguage(): String = Locale.getDefault().language
