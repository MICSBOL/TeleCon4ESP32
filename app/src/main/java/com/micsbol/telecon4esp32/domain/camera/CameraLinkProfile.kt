package com.micsbol.telecon4esp32.domain.camera

import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.usesCamera

/**
 * How an application pairs SoftAP HTTP video with the control link.
 *
 * Portable SoftAP model (Control Panel, RC Vehicle, Greenhouse, …):
 * - Role B — [Esp32Board.CAM] SoftAP TCP+video ([WIFI_SOFTAP]) for
 *   [BluetoothConnectionMode.WIFI_CAM_STARTER], [BluetoothConnectionMode.WIFI_SIMPLE],
 *   [BluetoothConnectionMode.WIFI_BINARY], and legacy [BluetoothConnectionMode.WIFI_SOFTAP]
 * - Role A — DevKit Bluetooth (Classic Simple / Classic Binary / BLE Binary) plus
 *   optional video-only SoftAP camera overlay → [WIFI_CAMERA_DEVKIT_BT]
 *
 * DevKit without the overlay stays [CONTROL_ONLY] (no SoftAP HTTP).
 *
 * Single-board SoftAP+Bluetooth on one ESP32-CAM is not a supported product mode.
 */
enum class CameraLinkProfile {
    /** No SoftAP HTTP video (DevKit without overlay, or non-camera application). */
    CONTROL_ONLY,

    /**
     * Role B: SoftAP HTTP video + SoftAP TCP control on one CAM board
     * (`TeleCon_ControlPanel_CAM_WiFi_Simple` / Advanced SoftAP Binary).
     * Wire protocol is selected by the connection mode (`simple` / `binary`), not this profile.
     */
    WIFI_SOFTAP,

    /**
     * Role A: SoftAP HTTP video on a separate ESP32-CAM (`TeleCon_ControlPanel_CAM_SoftAP_Video`)
     * + Bluetooth control on DevKit (Classic Simple, Classic Binary, or BLE Binary).
     * Never opens SoftAP TCP `:3333`.
     */
    WIFI_CAMERA_DEVKIT_BT,
}

fun resolveCameraLinkProfile(
    applicationId: ApplicationId,
    board: Esp32Board,
    mode: BluetoothConnectionMode,
    useSoftApCamera: Boolean = false,
): CameraLinkProfile {
    if (!applicationId.usesCamera()) {
        return CameraLinkProfile.CONTROL_ONLY
    }
    if (board == Esp32Board.CAM) {
        return CameraLinkProfile.WIFI_SOFTAP
    }
    val overlay = useSoftApCamera || board.isKitBDual
    if (overlay && mode.isBluetoothLink) {
        return CameraLinkProfile.WIFI_CAMERA_DEVKIT_BT
    }
    return CameraLinkProfile.CONTROL_ONLY
}

fun resolveCameraLinkProfile(
    applicationId: ApplicationId,
    board: Esp32Board,
    transport: BluetoothTransportType,
    useSoftApCamera: Boolean = false,
): CameraLinkProfile {
    // Portable default SoftAP TCP mode; Advanced apps pick WIFI_BINARY via stored mode.
    val mode = when (transport) {
        BluetoothTransportType.WIFI -> BluetoothConnectionMode.WIFI_SIMPLE
        BluetoothTransportType.BLE -> BluetoothConnectionMode.BLE_BINARY
        BluetoothTransportType.CLASSIC -> BluetoothConnectionMode.CLASSIC_SIMPLE
    }
    return resolveCameraLinkProfile(applicationId, board, mode, useSoftApCamera)
}

val CameraLinkProfile.shouldStartCameraStream: Boolean
    get() = this == CameraLinkProfile.WIFI_SOFTAP ||
        this == CameraLinkProfile.WIFI_CAMERA_DEVKIT_BT

/** SoftAP TCP control auto-opens once HTTP video proves SoftAP is reachable. */
val CameraLinkProfile.autoConnectSoftApControlWhenCameraOnline: Boolean
    get() = this == CameraLinkProfile.WIFI_SOFTAP

val CameraLinkProfile.isSoftApControl: Boolean
    get() = this == CameraLinkProfile.WIFI_SOFTAP

val CameraLinkProfile.isBluetoothControlWithCamera: Boolean
    get() = this == CameraLinkProfile.WIFI_CAMERA_DEVKIT_BT

/**
 * SoftAP video performance presets (Smooth / Balanced / High) apply whenever
 * SoftAP HTTP video is armed — Kit A SoftAP or DevKit Bluetooth + SoftAP camera overlay.
 */
fun shouldShowRuntimeStreamQualityPanel(
    applicationId: ApplicationId,
    board: Esp32Board,
    mode: BluetoothConnectionMode,
    useSoftApCamera: Boolean = false,
): Boolean {
    if (applicationId != ApplicationId.RC_VEHICLE_PRO &&
        applicationId != ApplicationId.CONTROL_PANEL
    ) {
        return false
    }
    val profile = resolveCameraLinkProfile(applicationId, board, mode, useSoftApCamera)
    return profile.shouldStartCameraStream
}

/** Settings screen: RC Vehicle Pro / Control Panel SoftAP performance section. */
fun showSoftApPerformanceSettings(
    applicationId: ApplicationId,
    board: Esp32Board,
    mode: BluetoothConnectionMode,
    useSoftApCamera: Boolean = false,
): Boolean = shouldShowRuntimeStreamQualityPanel(
    applicationId,
    board,
    mode,
    useSoftApCamera,
)
