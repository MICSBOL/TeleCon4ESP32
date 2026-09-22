package com.micsbol.telecon4esp32.ui.codes

import androidx.annotation.DrawableRes
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
    /** Optional flat illustration for the docs screen (not NeoIconBadge). */
    @DrawableRes val illustrationRes: Int? = null,
    @StringRes val subtitleRes: Int? = null,
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

/** Fast guide, user guide, and control protocol for the module docs button. */
fun documentationAssetsFor(language: String): List<CodeAssetInfo> {
    val isSpanish = language == "es"
    return listOf(
        CodeAssetInfo(
            titleRes = R.string.codes_document_fast_guide,
            icon = Icons.Default.Dock,
            assetFileName = if (isSpanish) "fast_guide_esp.pdf" else "fast_guide_eng.pdf",
            type = CodeAssetType.Pdf,
            illustrationRes = R.drawable.ic_guide_clipboard,
            subtitleRes = R.string.codes_document_fast_guide_hint,
        ),
        CodeAssetInfo(
            titleRes = R.string.codes_document_general,
            icon = Icons.Default.DocumentScanner,
            assetFileName = if (isSpanish) {
                "telecon_user_guide_esp.pdf"
            } else {
                "telecon_user_guide_eng.pdf"
            },
            type = CodeAssetType.Pdf,
            illustrationRes = R.drawable.ic_guide_documents,
            subtitleRes = R.string.codes_document_general_hint,
        ),
        CodeAssetInfo(
            titleRes = R.string.codes_document_protocol,
            icon = Icons.Default.Code,
            assetFileName = if (isSpanish) {
                "telecon_control_protocol_esp.pdf"
            } else {
                "telecon_control_protocol_eng.pdf"
            },
            type = CodeAssetType.Pdf,
            illustrationRes = R.drawable.ic_guide_protocol,
            subtitleRes = R.string.codes_document_protocol_hint,
        ),
    )
}

/** Documents shown on the module docs screen (guides only; no sketch ZIPs). */
fun codeAssetsFor(applicationId: ApplicationId, language: String): List<CodeAssetInfo> =
    documentationAssetsFor(language)

/** Sketch packages for the current board + connection (settings only). */
fun codePackageAssetsMatching(
    applicationId: ApplicationId,
    board: Esp32Board,
    mode: BluetoothConnectionMode,
    useSoftApCamera: Boolean = false,
): List<CodeAssetInfo> {
    val profile = resolveCameraLinkProfile(applicationId, board, mode, useSoftApCamera)
    return codePackagesFor(applicationId).filter { it.matches(board, mode, profile) }
}

fun codeAssetsMatching(
    applicationId: ApplicationId,
    language: String,
    board: Esp32Board,
    mode: BluetoothConnectionMode,
    useSoftApCamera: Boolean = false,
): List<CodeAssetInfo> = codePackageAssetsMatching(
    applicationId = applicationId,
    board = board,
    mode = mode,
    useSoftApCamera = useSoftApCamera,
)

fun controlPanelCodePackageIds(
    board: Esp32Board,
    mode: BluetoothConnectionMode,
    useSoftApCamera: Boolean = false,
): List<String> = codePackageAssetsMatching(
    ApplicationId.CONTROL_PANEL,
    board = board,
    mode = mode,
    useSoftApCamera = useSoftApCamera,
).mapNotNull { it.id }

private fun codePackagesFor(applicationId: ApplicationId): List<CodeAssetInfo> =
    when (applicationId) {
        ApplicationId.CONTROL_PANEL -> controlPanelCodePackages()
        ApplicationId.RC_VEHICLE_PRO -> emptyList()
    }

private fun controlPanelCodePackages(): List<CodeAssetInfo> {
    val bluetoothModes = setOf(
        BluetoothConnectionMode.CLASSIC_SIMPLE,
        BluetoothConnectionMode.CLASSIC_BINARY,
        BluetoothConnectionMode.BLE_BINARY,
    )
    val dualBoards = setOf(Esp32Board.DEV_KIT, Esp32Board.CAM_AND_DEV_KIT)

    return listOf(
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
    )
}

fun currentCodeAssetLanguage(): String = Locale.getDefault().language
