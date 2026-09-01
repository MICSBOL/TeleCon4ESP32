package com.micsbol.telecon4esp32.domain.camera

import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board

/** SoftAP credentials the Control Panel camera pane should join for HTTP video. */
data class CameraVideoSoftApTarget(
    val ssid: String,
    val password: String,
)

/**
 * CAM video SoftAP: starter SSID on DevKit overlay / CAM starter firmware;
 * [Esp32CameraDefaults.SOFTAP_SSID] for Advanced Kit A CAM.
 */
fun resolveCameraVideoSoftApTarget(
    board: Esp32Board,
    mode: BluetoothConnectionMode,
): CameraVideoSoftApTarget {
    val ssid = when (board.normalizedControlBoard()) {
        Esp32Board.CAM -> when (mode) {
            BluetoothConnectionMode.WIFI_CAM_STARTER -> Esp32CameraDefaults.STARTER_SOFTAP_SSID
            else -> Esp32CameraDefaults.SOFTAP_SSID
        }
        else -> Esp32CameraDefaults.STARTER_SOFTAP_SSID
    }
    return CameraVideoSoftApTarget(
        ssid = ssid,
        password = Esp32CameraDefaults.SOFTAP_PASSWORD,
    )
}

/**
 * DevKit Wi‑Fi control uses a different SoftAP than CAM video. Joining the CAM AP
 * would drop the control network — show a message instead.
 */
fun cameraVideoSoftApConflictsWithControlLink(
    board: Esp32Board,
    mode: BluetoothConnectionMode,
): Boolean = board.normalizedControlBoard() != Esp32Board.CAM && mode.isWifiLink

/**
 * Profile used while the Control Panel camera pane is trying to stream.
 * Session-arms SoftAP HTTP without persisting the overlay setting, and never
 * opens SoftAP TCP when Bluetooth is the control link.
 */
fun resolveSessionCameraLinkProfile(
    applicationId: ApplicationId,
    board: Esp32Board,
    mode: BluetoothConnectionMode,
    useSoftApCamera: Boolean,
): CameraLinkProfile {
    val stored = resolveCameraLinkProfile(applicationId, board, mode, useSoftApCamera)
    if (stored.shouldStartCameraStream) return stored
    return if (board.normalizedControlBoard() == Esp32Board.CAM) {
        CameraLinkProfile.WIFI_SOFTAP
    } else {
        CameraLinkProfile.WIFI_CAMERA_DEVKIT_BT
    }
}
