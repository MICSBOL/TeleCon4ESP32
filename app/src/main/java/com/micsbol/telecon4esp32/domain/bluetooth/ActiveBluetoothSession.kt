package com.micsbol.telecon4esp32.domain.bluetooth

import com.micsbol.telecon4esp32.domain.model.ApplicationId

/** Context the app requests when opening a Bluetooth session from a specific application screen. */
data class BluetoothSessionContext(
    val applicationId: ApplicationId,
    val protocolMode: BluetoothProtocolMode,
    val transport: BluetoothTransportType = BluetoothTransportType.CLASSIC,
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
)
