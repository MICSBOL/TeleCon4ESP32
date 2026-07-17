package com.micsbol.telecon4esp32.domain.bluetooth.sp

import com.micsbol.telecon4esp32.domain.bluetooth.AppBinaryFrame
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Solar Power (`SP`) binary protocol — app byte `0x53` (`S`).
 *
 * Live DATA payload is fixed-size; chart history (`hist_*`) remains SIMPLE-only.
 */
object SpBinaryProtocol {
    const val APP_BYTE: Byte = 0x53
    const val DATA_PAYLOAD_SIZE: Int = 50

    const val MASK_REFRESH: Int = 1 shl 0
    const val MASK_PERIOD: Int = 1 shl 1
    const val MASK_INVERTER: Int = 1 shl 2
    const val MASK_RESET_DAY: Int = 1 shl 3
}

object SpPacketEncoder {
    fun buildSetPacket(pairs: Map<String, Any>): ByteArray {
        var mask = 0
        val values = mutableListOf<Byte>()

        fun append(key: String, bit: Int, transform: (Any) -> Byte) {
            val raw = pairs[key] ?: return
            mask = mask or bit
            values += transform(raw)
        }

        append("refresh", SpBinaryProtocol.MASK_REFRESH) { AppBinaryFrame.boolByte(it) }
        append("period", SpBinaryProtocol.MASK_PERIOD) { AppBinaryFrame.u8Byte(it) }
        append("inverter", SpBinaryProtocol.MASK_INVERTER) { AppBinaryFrame.boolByte(it) }
        append("reset_day", SpBinaryProtocol.MASK_RESET_DAY) { AppBinaryFrame.boolByte(it) }

        return AppBinaryFrame.buildSetPacket(SpBinaryProtocol.APP_BYTE, mask, values)
    }
}

object SpBinaryTelemetryMapper {
    fun decodeDataPacket(bytes: ByteArray): Map<String, String>? {
        val (type, payload) = AppBinaryFrame.decodeInbound(bytes, SpBinaryProtocol.APP_BYTE) ?: return null
        if (type != AppBinaryFrame.TYPE_DATA) return null
        if (payload.size < SpBinaryProtocol.DATA_PAYLOAD_SIZE) return null

        val buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        return buildMap {
            put("solar_w", u16(buf).toString())
            put("load_w", u16(buf).toString())
            put("batt_w", buf.short.toInt().toString())
            put("grid_w", buf.short.toInt().toString())
            put("batt_pct", u8(buf).toString())
            put("volt", (buf.short.toInt() / 10f).toString())
            put("amp", (buf.short.toInt() / 10f).toString())
            put("today_kwh", (u16(buf) / 10f).toString())
            put("month_kwh", (u16(buf) / 10f).toString())
            put("total_kwh", (u16(buf) / 10f).toString())
            put("cons_week_kwh", (u16(buf) / 10f).toString())
            put("cons_day_kwh", (u16(buf) / 10f).toString())
            put("prod_kwh", (u16(buf) / 10f).toString())
            put("export_kwh", (u16(buf) / 10f).toString())
            put("batt_used_kwh", (u16(buf) / 10f).toString())
            put("panels", u8(buf).toString())
            put("status", u8(buf).toString())
            put("inverter", u8(buf).toString())
            put("grid_mode", u8(buf).toString())
            put("panel_eff", u8(buf).toString())
            put("fault", u8(buf).toString())
            put("batt_cap_kwh", (u16(buf) / 10f).toString())
            put("charge_eta_min", u16(buf).toString())
            put("total_charge_kwh", (u16(buf) / 10f).toString())
            put("max_solar_w", u16(buf).toString())
            put("low_batt_pct", u8(buf).toString())
            put("to_home_kwh", (u16(buf) / 10f).toString())
            put("to_batt_kwh", (u16(buf) / 10f).toString())
            put("to_grid_kwh", (u16(buf) / 10f).toString())
        }
    }

    private fun u8(buf: ByteBuffer): Int = buf.get().toInt() and 0xFF
    private fun u16(buf: ByteBuffer): Int = buf.short.toInt() and 0xFFFF
}
