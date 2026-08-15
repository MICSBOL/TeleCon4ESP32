package com.micsbol.telecon4esp32.domain.camera

import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.usesCamera

/**
 * How an application pairs SoftAP HTTP video with the control link.
 *
 * Portable SoftAP model (RC Vehicle, Greenhouse, …):
 * - [Esp32Board.CAM] SoftAP TCP+video for [BluetoothConnectionMode.WIFI_CAM_STARTER],
 *   [BluetoothConnectionMode.WIFI_SIMPLE], [BluetoothConnectionMode.WIFI_BINARY]
 *   (Advanced Kit A SoftAP binary), and legacy [BluetoothConnectionMode.WIFI_SOFTAP]
 * - [Esp32Board.CAM_AND_DEV_KIT] + [BluetoothConnectionMode.BLE_BINARY] → Kit B
 *   ([WIFI_CAMERA_DEVKIT_BLE])
 *
 * DevKit / non-camera apps stay [CONTROL_ONLY] (no SoftAP HTTP).
 *
 * Single-board SoftAP+BLE on one ESP32-CAM is not a supported product mode.
 */
enum class CameraLinkProfile {
    /** No SoftAP HTTP video (DevKit or non-camera application). */
    CONTROL_ONLY,

    /**
     * SoftAP HTTP video + SoftAP TCP control on one CAM board (Normal starter / Kit A).
     * Wire protocol is selected by the connection mode (`simple` / `binary`), not this profile.
     */
    WIFI_SOFTAP,

    /**
     * Kit B — SoftAP HTTP video on ESP32-CAM (video-only) + BLE Binary on DevKit.
     * Never opens SoftAP TCP `:3333`.
     */
    WIFI_CAMERA_DEVKIT_BLE,
}

fun resolveCameraLinkProfile(
    applicationId: ApplicationId,
    board: Esp32Board,
    mode: BluetoothConnectionMode,
): CameraLinkProfile {
    if (!applicationId.usesCamera() || !board.usesSoftApCamera) {
        return CameraLinkProfile.CONTROL_ONLY
    }
    if (board.isKitBDual) {
        return CameraLinkProfile.WIFI_CAMERA_DEVKIT_BLE
    }
    return when (mode) {
        BluetoothConnectionMode.WIFI_CAM_STARTER,
        BluetoothConnectionMode.WIFI_SOFTAP,
        BluetoothConnectionMode.WIFI_SIMPLE,
        BluetoothConnectionMode.WIFI_BINARY,
        -> CameraLinkProfile.WIFI_SOFTAP
        // Stale BLE on single CAM → SoftAP TCP+video (Kit A), not dual-radio.
        BluetoothConnectionMode.BLE_BINARY,
        BluetoothConnectionMode.CLASSIC_SIMPLE,
        BluetoothConnectionMode.CLASSIC_BINARY,
        -> CameraLinkProfile.WIFI_SOFTAP
    }
}

fun resolveCameraLinkProfile(
    applicationId: ApplicationId,
    board: Esp32Board,
    transport: BluetoothTransportType,
): CameraLinkProfile {
    // Portable default SoftAP TCP mode; Advanced apps pick WIFI_BINARY via stored mode.
    val mode = when (transport) {
        BluetoothTransportType.WIFI -> BluetoothConnectionMode.WIFI_SIMPLE
        BluetoothTransportType.BLE -> BluetoothConnectionMode.BLE_BINARY
        BluetoothTransportType.CLASSIC -> BluetoothConnectionMode.CLASSIC_SIMPLE
    }
    return resolveCameraLinkProfile(applicationId, board, mode)
}

val CameraLinkProfile.shouldStartCameraStream: Boolean
    get() = this == CameraLinkProfile.WIFI_SOFTAP ||
        this == CameraLinkProfile.WIFI_CAMERA_DEVKIT_BLE

/** SoftAP TCP control auto-opens once HTTP video proves SoftAP is reachable. */
val CameraLinkProfile.autoConnectSoftApControlWhenCameraOnline: Boolean
    get() = this == CameraLinkProfile.WIFI_SOFTAP

val CameraLinkProfile.isSoftApControl: Boolean
    get() = this == CameraLinkProfile.WIFI_SOFTAP

val CameraLinkProfile.isBleControlWithCamera: Boolean
    get() = this == CameraLinkProfile.WIFI_CAMERA_DEVKIT_BLE

/**
 * SoftAP video performance presets (Smooth / Balanced / High) apply whenever RC
 * SoftAP HTTP video is armed — Kit A SoftAP or Kit B SoftAP camera.
 */
fun showSoftApPerformanceSettings(
    applicationId: ApplicationId,
    board: Esp32Board,
    mode: BluetoothConnectionMode,
): Boolean {
    if (applicationId != ApplicationId.RC_VEHICLE_PRO) return false
    val profile = resolveCameraLinkProfile(applicationId, board, mode)
    return profile.shouldStartCameraStream
}
