package com.micsbol.telecon4esp32.domain.model

import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType

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
): BluetoothConnectionMode {
    if (transport == BluetoothTransportType.WIFI) {
        return BluetoothConnectionMode.WIFI_SOFTAP
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
    BluetoothConnectionMode.WIFI_SOFTAP,
    -> true
    BluetoothConnectionMode.CLASSIC_BINARY,
    BluetoothConnectionMode.BLE_BINARY,
    -> canUseAdvancedProtocol(applicationId)
}
