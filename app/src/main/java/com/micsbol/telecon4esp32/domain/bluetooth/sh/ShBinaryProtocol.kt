package com.micsbol.telecon4esp32.domain.bluetooth.sh

import com.micsbol.telecon4esp32.domain.bluetooth.AppBinaryFrame
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Smart Home (`SH`) binary protocol — app byte `0x48` (`H`).
 *
 * Compact system snapshot; room detail stays SIMPLE-friendly for firmware demos.
 */
object ShBinaryProtocol {
    const val APP_BYTE: Byte = 0x48
    const val DATA_PAYLOAD_SIZE: Int = 12

    const val MASK_REFRESH: Int = 1 shl 0
    const val MASK_ROOM_DEVICE: Int = 1 shl 1
}

object ShPacketEncoder {
    /**
     * Encodes SET commands. For `room`/`device` toggles, [pairs] must include
     * `room_id` (u8), `device_id` (u8), and `state` (0/1).
     */
    fun buildSetPacket(pairs: Map<String, Any>): ByteArray {
        var mask = 0
        val values = mutableListOf<Byte>()

        if (pairs.containsKey("refresh")) {
            mask = mask or ShBinaryProtocol.MASK_REFRESH
            values += AppBinaryFrame.boolByte(pairs.getValue("refresh"))
        }
        if (pairs.containsKey("room_id") && pairs.containsKey("device_id")) {
            mask = mask or ShBinaryProtocol.MASK_ROOM_DEVICE
            values += AppBinaryFrame.u8Byte(pairs.getValue("room_id"))
            values += AppBinaryFrame.u8Byte(pairs.getValue("device_id"))
            values += AppBinaryFrame.boolByte(pairs["state"] ?: 1)
        }

        return AppBinaryFrame.buildSetPacket(ShBinaryProtocol.APP_BYTE, mask, values)
    }
}

object ShBinaryTelemetryMapper {
    fun decodeDataPacket(bytes: ByteArray): Map<String, String>? {
        val (type, payload) = AppBinaryFrame.decodeInbound(bytes, ShBinaryProtocol.APP_BYTE) ?: return null
        if (type != AppBinaryFrame.TYPE_DATA) return null
        if (payload.size < ShBinaryProtocol.DATA_PAYLOAD_SIZE) return null

        val buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        val climateTemp = buf.get().toInt()
        val climateStatus = buf.get().toInt() and 0xFF
        val energyX10 = buf.short.toInt() and 0xFFFF
        val securityStatus = buf.get().toInt() and 0xFF
        val waterL = buf.short.toInt() and 0xFFFF
        val status = buf.get().toInt() and 0xFF
        val livingOn = buf.get().toInt() and 0xFF
        val kitchenOn = buf.get().toInt() and 0xFF
        val bedroomOn = buf.get().toInt() and 0xFF
        val garageOn = buf.get().toInt() and 0xFF

        return mapOf(
            "climate_temp" to climateTemp.toString(),
            "climate_status" to climateStatus.toString(),
            "energy_kw" to (energyX10 / 10f).toString(),
            "power_kw" to (energyX10 / 10f).toString(),
            "security_status" to securityStatus.toString(),
            "water_l" to waterL.toString(),
            "status" to status.toString(),
            "living_on" to livingOn.toString(),
            "kitchen_on" to kitchenOn.toString(),
            "bedroom_on" to bedroomOn.toString(),
            "garage_on" to garageOn.toString(),
        )
    }
}
