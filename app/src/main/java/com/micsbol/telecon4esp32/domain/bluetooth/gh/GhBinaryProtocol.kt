package com.micsbol.telecon4esp32.domain.bluetooth.gh

/**
 * Greenhouse (`GH`) binary protocol constants shared by Android and ESP32 firmware.
 *
 * Wire families (same socket as SIMPLE lines and RC binary):
 * - Phone → ESP32 SET: `0xAA 0x47` + mask + values + checksum
 * - ESP32 → Phone DATA: `0xCC 0x47` + length + type + payload + checksum
 */
object GhBinaryProtocol {

    const val APP_BYTE: Int = 0x47 // ASCII 'G'

    const val OUTBOUND_HEADER_0: Byte = 0xAA.toByte()
    const val OUTBOUND_HEADER_1: Byte = APP_BYTE.toByte()

    const val INBOUND_HEADER_0: Byte = 0xCC.toByte()
    const val INBOUND_HEADER_1: Byte = APP_BYTE.toByte()

    const val TYPE_DATA: Byte = 0x01

    const val DATA_PAYLOAD_SIZE: Int = 17

    const val MASK_FAN: Int = 1 shl 0
    const val MASK_HEATER: Int = 1 shl 1
    const val MASK_PUMP: Int = 1 shl 2
    const val MASK_LIGHTS: Int = 1 shl 3
    const val MASK_AUTO: Int = 1 shl 4
    const val MASK_VENT: Int = 1 shl 5
    const val MASK_TARGET_TEMP: Int = 1 shl 6
    const val MASK_TARGET_HUM: Int = 1 shl 7

    const val FLAG_FAN: Int = 1 shl 0
    const val FLAG_HEATER: Int = 1 shl 1
    const val FLAG_PUMP: Int = 1 shl 2
    const val FLAG_LIGHTS: Int = 1 shl 3
    const val FLAG_AUTO: Int = 1 shl 4
    const val FLAG_STABLE: Int = 1 shl 5
    const val FLAG_HAS_CAMERA: Int = 1 shl 6
}
