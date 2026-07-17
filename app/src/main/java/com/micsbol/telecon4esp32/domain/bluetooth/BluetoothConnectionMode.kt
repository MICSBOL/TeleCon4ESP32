package com.micsbol.telecon4esp32.domain.bluetooth

/**
 * User-facing Bluetooth link choice for an application.
 *
 * Classic offers both text and binary wire formats; BLE always uses binary
 * (same frames as Classic ADVANCED over the Nordic UART Service).
 */
enum class BluetoothConnectionMode {
    /** Classic SPP + SIMPLE text lines. */
    CLASSIC_SIMPLE,

    /** Classic SPP + ADVANCED binary packets. */
    CLASSIC_BINARY,

    /** BLE (NUS) + ADVANCED binary packets. */
    BLE_BINARY,
    ;

    val transport: BluetoothTransportType
        get() = when (this) {
            CLASSIC_SIMPLE, CLASSIC_BINARY -> BluetoothTransportType.CLASSIC
            BLE_BINARY -> BluetoothTransportType.BLE
        }

    val protocolMode: BluetoothProtocolMode
        get() = when (this) {
            CLASSIC_SIMPLE -> BluetoothProtocolMode.SIMPLE
            CLASSIC_BINARY, BLE_BINARY -> BluetoothProtocolMode.ADVANCED
        }

    companion object {
        fun from(
            transport: BluetoothTransportType,
            protocolMode: BluetoothProtocolMode,
        ): BluetoothConnectionMode = when (transport) {
            BluetoothTransportType.BLE -> BLE_BINARY
            BluetoothTransportType.CLASSIC -> when (protocolMode) {
                BluetoothProtocolMode.ADVANCED -> CLASSIC_BINARY
                BluetoothProtocolMode.SIMPLE -> CLASSIC_SIMPLE
            }
        }
    }
}
