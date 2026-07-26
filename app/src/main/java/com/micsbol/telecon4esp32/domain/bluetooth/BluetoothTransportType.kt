package com.micsbol.telecon4esp32.domain.bluetooth

/**
 * Physical link used to reach the ESP32.
 *
 * - [CLASSIC] — Bluetooth Classic RFCOMM/SPP socket (BluetoothSerial firmware).
 * - [BLE] — Bluetooth Low Energy GATT link using the Nordic UART Service; carries
 *   the same wire protocols as SPP. Preferred pairing is the binary protocol.
 * - [WIFI] — SoftAP TCP control socket (RC Vehicle Pro ESP32-CAM Wi‑Fi firmware).
 */
enum class BluetoothTransportType {
    CLASSIC,
    BLE,
    WIFI,
    ;

    companion object {
        fun fromStored(value: String?): BluetoothTransportType =
            entries.firstOrNull { it.name == value } ?: CLASSIC
    }
}
