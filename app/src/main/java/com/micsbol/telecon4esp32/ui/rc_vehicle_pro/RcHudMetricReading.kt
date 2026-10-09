package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import com.micsbol.telecon4esp32.domain.bluetooth.TelemetryChannelRouter
import com.micsbol.telecon4esp32.domain.bluetooth.TelemetryState
import com.micsbol.telecon4esp32.domain.model.TelemetryChannel
import com.micsbol.telecon4esp32.domain.model.TelemetryChannelKind
import kotlin.math.roundToInt

/** Scales a telemetry channel into the top-bar gauges. */
object RcHudMetricReading {

    fun speedKmh(channel: TelemetryChannel, telemetry: TelemetryState): Float? {
        val raw = numeric(channel, telemetry) ?: return null
        return when (channel.kind) {
            TelemetryChannelKind.I32 -> if (raw > 0) raw / 10f else null
            TelemetryChannelKind.U8 -> raw / 255f * RcVehicleProLayout.HUD_MAX_SPEED_KMH
            TelemetryChannelKind.BIT -> null
        }
    }

    fun tempCelsius(channel: TelemetryChannel, telemetry: TelemetryState): Int {
        val raw = numeric(channel, telemetry) ?: return 0
        return when (channel.kind) {
            TelemetryChannelKind.U8 -> (25 + (raw.coerceIn(0, 255) * 0.2f)).roundToInt()
            TelemetryChannelKind.I32 -> raw.coerceIn(-40, 250)
            TelemetryChannelKind.BIT -> 0
        }
    }

    fun batteryPercent(channel: TelemetryChannel, telemetry: TelemetryState): Int {
        val raw = numeric(channel, telemetry) ?: return 0
        val value = raw.coerceAtLeast(0)
        return if (value <= 100) value else ((value / 255f) * 100f).roundToInt().coerceIn(0, 100)
    }

    /** U8 is tenths of an amp (0–25.5 A). A panel value is already in tenths. */
    fun currentAmps(channel: TelemetryChannel, telemetry: TelemetryState): Float {
        val raw = numeric(channel, telemetry) ?: return 0f
        return when (channel.kind) {
            TelemetryChannelKind.U8 -> raw.coerceIn(0, 255) / 10f
            TelemetryChannelKind.I32 -> raw.coerceAtLeast(0) / 10f
            TelemetryChannelKind.BIT -> 0f
        }
    }

    private fun numeric(channel: TelemetryChannel, telemetry: TelemetryState): Int? = when (channel.kind) {
        TelemetryChannelKind.U8 -> TelemetryChannelRouter.lastU8(channel, telemetry)
        TelemetryChannelKind.I32 -> when (channel) {
            TelemetryChannel.PANEL_LEFT -> telemetry.panelState.leftValue
            TelemetryChannel.PANEL_RIGHT -> telemetry.panelState.rightValue
            else -> null
        }
        TelemetryChannelKind.BIT -> null
    }
}
