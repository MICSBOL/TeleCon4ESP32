package com.micsbol.telecon4esp32.domain.model

import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.bluetooth.ConnectionLinkFamily
import com.micsbol.telecon4esp32.domain.bluetooth.linkFamily

/** Applications that support an ADVANCED (binary) protocol in addition to SIMPLE lines. */
fun ApplicationId.supportsBinaryProtocol(): Boolean = true

/**
 * Binary / BLE options require premium for paid apps.
 * Free apps ([ApplicationId.CONTROL_PANEL]) always allow advanced modes.
 */
fun Entitlement.canUseAdvancedProtocol(applicationId: ApplicationId): Boolean {
    val feature = applicationId.premiumFeature() ?: return true
    return has(feature)
}

fun Entitlement.effectiveProtocolMode(
    applicationId: ApplicationId,
    stored: BluetoothProtocolMode,
): BluetoothProtocolMode {
    if (stored == BluetoothProtocolMode.SIMPLE) return BluetoothProtocolMode.SIMPLE
    return if (applicationId.supportsBinaryProtocol() && canUseAdvancedProtocol(applicationId)) {
        BluetoothProtocolMode.ADVANCED
    } else {
        BluetoothProtocolMode.SIMPLE
    }
}

fun Entitlement.effectiveConnectionMode(
    applicationId: ApplicationId,
    transport: BluetoothTransportType,
    storedProtocol: BluetoothProtocolMode,
    board: Esp32Board = Esp32Board.DEV_KIT,
): BluetoothConnectionMode {
    if (applicationId.usesCamera() && board.isKitBDual) {
        return if (canUseConnectionMode(applicationId, BluetoothConnectionMode.BLE_BINARY)) {
            BluetoothConnectionMode.BLE_BINARY
        } else {
            BluetoothConnectionMode.WIFI_CAM_STARTER
        }
    }
    if (transport == BluetoothTransportType.WIFI) {
        // CAM SoftAP: Normal → starter; Advanced → SoftAP Binary (Kit A).
        if (applicationId.usesCamera() && board == Esp32Board.CAM) {
            return if (canUseConnectionMode(applicationId, BluetoothConnectionMode.WIFI_BINARY)) {
                BluetoothConnectionMode.WIFI_BINARY
            } else {
                BluetoothConnectionMode.WIFI_CAM_STARTER
            }
        }
        val protocol = effectiveProtocolMode(applicationId, storedProtocol)
        return if (protocol == BluetoothProtocolMode.ADVANCED) {
            BluetoothConnectionMode.WIFI_BINARY
        } else {
            BluetoothConnectionMode.WIFI_SIMPLE
        }
    }
    val protocol = effectiveProtocolMode(applicationId, storedProtocol)
    val mode = BluetoothConnectionMode.from(transport, protocol)
    // BLE always implies binary when the user is allowed to use advanced.
    if (transport == BluetoothTransportType.BLE && canUseAdvancedProtocol(applicationId)) {
        return BluetoothConnectionMode.BLE_BINARY
    }
    // BLE without premium falls back to Classic Simple for the UI selection.
    if (transport == BluetoothTransportType.BLE && !canUseAdvancedProtocol(applicationId)) {
        return BluetoothConnectionMode.CLASSIC_SIMPLE
    }
    return mode
}

fun Entitlement.canUseConnectionMode(
    applicationId: ApplicationId,
    mode: BluetoothConnectionMode,
): Boolean = when (mode) {
    BluetoothConnectionMode.CLASSIC_SIMPLE,
    BluetoothConnectionMode.WIFI_CAM_STARTER,
    BluetoothConnectionMode.WIFI_SIMPLE,
    -> true
    BluetoothConnectionMode.WIFI_SOFTAP,
    BluetoothConnectionMode.CLASSIC_BINARY,
    BluetoothConnectionMode.BLE_BINARY,
    BluetoothConnectionMode.WIFI_BINARY,
    -> canUseAdvancedProtocol(applicationId)
}

/**
 * Connection modes offered in app settings for the selected board.
 *
 * ESP32-CAM alone (camera apps):
 * - [WIFI_CAM_STARTER] — Normal SoftAP Simple (starter SSID)
 * - [WIFI_BINARY] — Advanced Kit A SoftAP Binary (main CAM SSID + video + TCP)
 *
 * [Esp32Board.CAM_AND_DEV_KIT] (Advanced Kit B):
 * - [BLE_BINARY] — SoftAP video on CAM + BLE Binary on DevKit
 *
 * DevKit / non-camera apps: Classic Simple/Binary, BLE Binary, Wi‑Fi Simple/Binary.
 */
fun ApplicationId.availableConnectionModes(board: Esp32Board): List<BluetoothConnectionMode> {
    if (usesCamera() && board.isKitBDual) {
        return listOf(BluetoothConnectionMode.BLE_BINARY)
    }
    if (usesCamera() && board == Esp32Board.CAM) {
        return listOf(
            BluetoothConnectionMode.WIFI_CAM_STARTER,
            BluetoothConnectionMode.WIFI_BINARY,
        )
    }
    return listOf(
        BluetoothConnectionMode.CLASSIC_SIMPLE,
        BluetoothConnectionMode.CLASSIC_BINARY,
        BluetoothConnectionMode.BLE_BINARY,
        BluetoothConnectionMode.WIFI_SIMPLE,
        BluetoothConnectionMode.WIFI_BINARY,
    )
}

fun ApplicationId.connectionModesForFamily(
    board: Esp32Board,
    family: ConnectionLinkFamily,
): List<BluetoothConnectionMode> =
    availableConnectionModes(board).filter { it.linkFamily == family }

fun ApplicationId.isConnectionModeAvailable(
    board: Esp32Board,
    mode: BluetoothConnectionMode,
): Boolean = mode in availableConnectionModes(board)

/**
 * Maps a stored mode onto one valid for [board], preferring SoftAP on CAM and
 * Classic Simple on DevKit when the current choice is hidden.
 */
fun Entitlement.coerceConnectionModeForBoard(
    applicationId: ApplicationId,
    board: Esp32Board,
    mode: BluetoothConnectionMode,
): BluetoothConnectionMode {
    val visible = applicationId.availableConnectionModes(board)
    if (mode in visible && canUseConnectionMode(applicationId, mode)) return mode

    // Kit B dual board → BLE Binary when entitled.
    if (applicationId.usesCamera() && board.isKitBDual) {
        val ble = BluetoothConnectionMode.BLE_BINARY
        if (ble in visible && canUseConnectionMode(applicationId, ble)) {
            return ble
        }
        val starter = BluetoothConnectionMode.WIFI_CAM_STARTER
        if (starter in applicationId.availableConnectionModes(Esp32Board.CAM) &&
            canUseConnectionMode(applicationId, starter)
        ) {
            return starter
        }
    }

    // Legacy Kit A text SoftAP / Kit B BLE / stale Wi‑Fi on CAM → SoftAP Binary or Starter.
    if (
        applicationId.usesCamera() &&
        board == Esp32Board.CAM &&
        (
            mode.transport == BluetoothTransportType.WIFI ||
                mode == BluetoothConnectionMode.BLE_BINARY
            )
    ) {
        val preferred = when {
            mode == BluetoothConnectionMode.WIFI_CAM_STARTER -> mode
            mode == BluetoothConnectionMode.WIFI_BINARY -> mode
            mode == BluetoothConnectionMode.WIFI_SOFTAP ||
                mode == BluetoothConnectionMode.WIFI_SIMPLE ||
                mode == BluetoothConnectionMode.BLE_BINARY -> {
                if (canUseConnectionMode(applicationId, BluetoothConnectionMode.WIFI_BINARY)) {
                    BluetoothConnectionMode.WIFI_BINARY
                } else {
                    BluetoothConnectionMode.WIFI_CAM_STARTER
                }
            }
            else -> BluetoothConnectionMode.WIFI_CAM_STARTER
        }
        if (preferred in visible && canUseConnectionMode(applicationId, preferred)) {
            return preferred
        }
    }

    // SoftAP kit / starter mode while on DevKit → DevKit Wi‑Fi Simple / Binary.
    if (
        board == Esp32Board.DEV_KIT &&
        (
            mode == BluetoothConnectionMode.WIFI_SOFTAP ||
                mode == BluetoothConnectionMode.WIFI_CAM_STARTER
            )
    ) {
        val wifi = if (canUseConnectionMode(applicationId, BluetoothConnectionMode.WIFI_BINARY) &&
            mode == BluetoothConnectionMode.WIFI_SOFTAP
        ) {
            BluetoothConnectionMode.WIFI_BINARY
        } else {
            BluetoothConnectionMode.WIFI_SIMPLE
        }
        if (wifi in visible && canUseConnectionMode(applicationId, wifi)) {
            return wifi
        }
    }

    // On CAM, prefer SoftAP Binary (Kit A) when entitled; otherwise Normal starter.
    if (applicationId.usesCamera() && board == Esp32Board.CAM) {
        val binary = BluetoothConnectionMode.WIFI_BINARY
        if (binary in visible && canUseConnectionMode(applicationId, binary)) {
            return binary
        }
        val starter = BluetoothConnectionMode.WIFI_CAM_STARTER
        if (starter in visible && canUseConnectionMode(applicationId, starter)) {
            return starter
        }
    }

    return visible.firstOrNull { canUseConnectionMode(applicationId, it) }
        ?: BluetoothConnectionMode.CLASSIC_SIMPLE
}
