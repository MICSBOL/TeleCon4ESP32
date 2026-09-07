package com.micsbol.telecon4esp32.domain.bluetooth

import com.micsbol.telecon4esp32.domain.model.TelemetryChannel

/**
 * Phone-side analog bus for stick/knob channel links. Driven channels keep a
 * rolling 0…1 trace; unlinked channels are omitted so the HUD can fall back to
 * telemetry or the default sinusoid.
 */
class ControlAnalogHistory(
    private val maxPoints: Int = SimpleProtocolTelemetryMapper.MAX_PLOT_POINTS,
) {
    private val buffers = TelemetryChannel.ANALOG_CHANNELS.associateWith {
        ArrayDeque<Float>(maxPoints + 1)
    }
    private var driven: Set<TelemetryChannel> = emptySet()

    fun ingest(values: Map<TelemetryChannel, Float>) {
        driven = values.keys.filter { it in TelemetryChannel.ANALOG_CHANNELS }.toSet()
        driven.forEach { channel ->
            val sample = values.getValue(channel).coerceIn(0f, 1f)
            val buffer = buffers.getValue(channel)
            if (buffer.isEmpty()) {
                repeat(maxPoints) { buffer.addLast(sample) }
            } else {
                buffer.addLast(sample)
                while (buffer.size > maxPoints) buffer.removeFirst()
            }
        }
        TelemetryChannel.ANALOG_CHANNELS.forEach { channel ->
            if (channel !in driven) buffers.getValue(channel).clear()
        }
    }

    fun snapshot(): Map<TelemetryChannel, List<Float>> =
        driven.associateWith { buffers.getValue(it).toList() }
}
