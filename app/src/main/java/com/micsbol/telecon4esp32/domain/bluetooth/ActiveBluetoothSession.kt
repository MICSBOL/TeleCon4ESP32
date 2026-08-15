package com.micsbol.telecon4esp32.domain.bluetooth

import com.micsbol.telecon4esp32.domain.model.ApplicationId

/** Context the app requests when opening a Bluetooth session from a specific application screen. */
data class BluetoothSessionContext(
    val applicationId: ApplicationId,
    val protocolMode: BluetoothProtocolMode,
    val transport: BluetoothTransportType = BluetoothTransportType.CLASSIC,
    /**
     * Full Settings mode. Distinguishes CAM SoftAP (`WIFI_SOFTAP` → `proto=wifi`) from
     * DevKit SoftAP (`WIFI_SIMPLE` / `WIFI_BINARY` → `simple` / `binary`).
     */
    val connectionMode: BluetoothConnectionMode =
        BluetoothConnectionMode.from(transport, protocolMode),
)

/**
 * Active Bluetooth session bound to one application profile and protocol mode.
 * Home and in-app screens read this instead of a global connected flag.
 */
data class ActiveBluetoothSession(
    val applicationId: ApplicationId,
    val protocolMode: BluetoothProtocolMode,
    val deviceName: String?,
    val deviceAddress: String,
    val handshakeConfirmed: Boolean = false,
    val transport: BluetoothTransportType = BluetoothTransportType.CLASSIC,
    val connectionMode: BluetoothConnectionMode =
        BluetoothConnectionMode.from(transport, protocolMode),
)
