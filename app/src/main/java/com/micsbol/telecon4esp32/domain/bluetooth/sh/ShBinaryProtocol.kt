package com.micsbol.telecon4esp32.domain.bluetooth.sh

import com.micsbol.telecon4esp32.domain.bluetooth.AppBinaryFrame
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Smart Home (`SH`) binary protocol — app byte `0x48` (`H`).
 *
 * Compact system snapshot plus device flags; room detail also travels on SIMPLE `SH:DATA`.
 */
object ShBinaryProtocol {
    const val APP_BYTE: Byte = 0x48
    /** climate…garage_on (12) + device_flags u16 (2). */
    const val DATA_PAYLOAD_SIZE: Int = 14

    const val MASK_REFRESH: Int = 1 shl 0
    const val MASK_ROOM_DEVICE: Int = 1 shl 1
    const val MASK_SCENE: Int = 1 shl 2

    const val SCENE_ALL_LIGHTS_OFF: Int = 0
    const val SCENE_AWAY: Int = 1
}

object ShPacketEncoder {
    /**
     * Encodes SET commands.
     * - Room device: `room_id` (u8), `device_id` (u8), `state` (0/1)
     * - Scene: `scene` as name ([SmartHomeProtocol.SCENE_*]) or int
     * - Refresh: `refresh`
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
        pairs["scene"]?.let { scene ->
            mask = mask or ShBinaryProtocol.MASK_SCENE
            values += sceneByte(scene)
        }

        return AppBinaryFrame.buildSetPacket(ShBinaryProtocol.APP_BYTE, mask, values)
    }

    private fun sceneByte(value: Any): Byte = when (value) {
        is Number -> value.toInt().coerceIn(0, 255).toByte()
        is String -> when (value) {
            SmartHomeProtocol.SCENE_ALL_LIGHTS_OFF -> ShBinaryProtocol.SCENE_ALL_LIGHTS_OFF
            SmartHomeProtocol.SCENE_AWAY -> ShBinaryProtocol.SCENE_AWAY
            else -> value.toIntOrNull()?.coerceIn(0, 255) ?: 0
        }.toByte()
        else -> 0
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
        val deviceFlags = buf.short.toInt() and 0xFFFF

        return buildMap {
            put("climate_temp", climateTemp.toString())
            put("climate_status", climateStatus.toString())
            put("energy_kw", (energyX10 / 10f).toString())
            put("power_kw", (energyX10 / 10f).toString())
            put("security_status", securityStatus.toString())
            put("water_l", waterL.toString())
            put("status", status.toString())
            put("living_on", livingOn.toString())
            put("kitchen_on", kitchenOn.toString())
            put("bedroom_on", bedroomOn.toString())
            put("garage_on", garageOn.toString())
            put("device_flags", deviceFlags.toString())
            putDeviceFlagKeys(deviceFlags)
        }
    }

    private fun MutableMap<String, String>.putDeviceFlagKeys(flags: Int) {
        fun putFlag(room: String, device: String) {
            val bit = SmartHomeProtocol.deviceFlagBit(room, device) ?: return
            put("${room}_$device", if ((flags and (1 shl bit)) != 0) "1" else "0")
        }
        putFlag(SmartHomeProtocol.ROOM_LIVING, SmartHomeProtocol.DEVICE_LIGHT)
        putFlag(SmartHomeProtocol.ROOM_LIVING, SmartHomeProtocol.DEVICE_AMBIENCE)
        putFlag(SmartHomeProtocol.ROOM_LIVING, SmartHomeProtocol.DEVICE_OUTLET)
        putFlag(SmartHomeProtocol.ROOM_KITCHEN, SmartHomeProtocol.DEVICE_LIGHT)
        putFlag(SmartHomeProtocol.ROOM_KITCHEN, SmartHomeProtocol.DEVICE_APPLIANCE)
        putFlag(SmartHomeProtocol.ROOM_KITCHEN, SmartHomeProtocol.DEVICE_WATER)
        putFlag(SmartHomeProtocol.ROOM_BEDROOM, SmartHomeProtocol.DEVICE_LIGHT)
        putFlag(SmartHomeProtocol.ROOM_GARAGE, SmartHomeProtocol.DEVICE_LIGHT)
        putFlag(SmartHomeProtocol.ROOM_GARAGE, SmartHomeProtocol.DEVICE_LOCK)
    }
}
