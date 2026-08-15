package com.micsbol.telecon4esp32.domain.bluetooth

/**
 * Link-layer connect failures (before or instead of [HandshakeFailure]).
 * Classic RFCOMM vs BLE GATT are different transports — a BLE-only ESP32 never reaches
 * RC:CONNECT when the app is set to Classic Simple/Binary.
 */
sealed interface BluetoothConnectFailure {
    data object MissingSessionContext : BluetoothConnectFailure

    /**
     * Same MAC is still bonded under a previous TeleCon/ESP32 name while the board
     * now advertises a different firmware name (typical after BLE ↔ Classic reflash).
     */
    data object StaleFirmwareBond : BluetoothConnectFailure

    /** App requested Classic SPP; RFCOMM socket failed (often BLE-only firmware). */
    data class ClassicLinkFailed(val technicalDetail: String) : BluetoothConnectFailure

    /** App requested BLE; GATT/NUS open failed (often Classic-only firmware). */
    data class BleLinkFailed(val technicalDetail: String) : BluetoothConnectFailure

    /**
     * SoftAP TCP control unreachable.
     * [connectionMode] / optional [softApSsid] select CAM vs DevKit error copy.
     */
    data class WifiSoftApLinkFailed(
        val technicalDetail: String,
        val connectionMode: BluetoothConnectionMode = BluetoothConnectionMode.WIFI_BINARY,
        val softApSsid: String? = null,
    ) : BluetoothConnectFailure

    data class Generic(val technicalDetail: String) : BluetoothConnectFailure

    companion object {
        fun fromLinkError(
            rawMessage: String,
            transport: BluetoothTransportType?,
            connectionMode: BluetoothConnectionMode? = null,
            softApSsid: String? = null,
        ): BluetoothConnectFailure = when (transport) {
            BluetoothTransportType.CLASSIC -> ClassicLinkFailed(rawMessage)
            BluetoothTransportType.BLE -> BleLinkFailed(rawMessage)
            BluetoothTransportType.WIFI -> WifiSoftApLinkFailed(
                technicalDetail = rawMessage,
                connectionMode = connectionMode
                    ?: BluetoothConnectionMode.WIFI_BINARY,
                softApSsid = softApSsid,
            )
            null -> Generic(rawMessage)
        }
    }
}
