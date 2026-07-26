package com.micsbol.telecon4esp32.domain.bluetooth

/**
 * Link-layer connect failures (before or instead of [HandshakeFailure]).
 * Classic RFCOMM vs BLE GATT are different transports — a BLE-only ESP32 never reaches
 * RC:CONNECT when the app is set to Classic Simple/Binary.
 */
sealed interface BluetoothConnectFailure {
    data object MissingSessionContext : BluetoothConnectFailure

    /** App requested Classic SPP; RFCOMM socket failed (often BLE-only firmware). */
    data class ClassicLinkFailed(val technicalDetail: String) : BluetoothConnectFailure

    /** App requested BLE; GATT/NUS open failed (often Classic-only firmware). */
    data class BleLinkFailed(val technicalDetail: String) : BluetoothConnectFailure

    /** SoftAP TCP control unreachable (phone not on TeleCon-RC-CAM, or firmware down). */
    data class WifiSoftApLinkFailed(val technicalDetail: String) : BluetoothConnectFailure

    data class Generic(val technicalDetail: String) : BluetoothConnectFailure

    companion object {
        fun fromLinkError(
            rawMessage: String,
            transport: BluetoothTransportType?,
        ): BluetoothConnectFailure = when (transport) {
            BluetoothTransportType.CLASSIC -> ClassicLinkFailed(rawMessage)
            BluetoothTransportType.BLE -> BleLinkFailed(rawMessage)
            BluetoothTransportType.WIFI -> WifiSoftApLinkFailed(rawMessage)
            null -> Generic(rawMessage)
        }
    }
}
