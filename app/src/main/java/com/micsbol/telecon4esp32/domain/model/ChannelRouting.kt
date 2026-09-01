package com.micsbol.telecon4esp32.domain.model

/**
 * Numbered telemetry sources on the wire. Firmware still sends [RC:PLOT] / [RC:DATA]
 * (and binary CC 11/22/33); Android maps these IDs onto widgets.
 */
enum class TelemetryChannelKind {
    U8,
    I32,
    BIT,
}

enum class TelemetryChannel(val kind: TelemetryChannelKind) {
    CH_1(TelemetryChannelKind.U8),
    CH_2(TelemetryChannelKind.U8),
    CH_3(TelemetryChannelKind.U8),
    CH_4(TelemetryChannelKind.U8),
    CH_5(TelemetryChannelKind.U8),
    CH_6(TelemetryChannelKind.U8),
    CH_7(TelemetryChannelKind.U8),
    CH_8(TelemetryChannelKind.U8),
    ANALOG(TelemetryChannelKind.U8),
    BATT(TelemetryChannelKind.U8),
    PANEL_LEFT(TelemetryChannelKind.I32),
    PANEL_RIGHT(TelemetryChannelKind.I32),
    LED_0(TelemetryChannelKind.BIT),
    LED_1(TelemetryChannelKind.BIT),
    LED_2(TelemetryChannelKind.BIT),
    LED_3(TelemetryChannelKind.BIT),
    LED_4(TelemetryChannelKind.BIT),
    LED_5(TelemetryChannelKind.BIT),
    LED_6(TelemetryChannelKind.BIT),
    LED_7(TelemetryChannelKind.BIT),
    ;

    companion object {
        val U8_SOURCES: List<TelemetryChannel> = entries.filter { it.kind == TelemetryChannelKind.U8 }
        val ANALOG_CHANNELS: List<TelemetryChannel> = listOf(
            CH_1, CH_2, CH_3, CH_4, CH_5, CH_6, CH_7, CH_8,
        )
        val I32_SOURCES: List<TelemetryChannel> = entries.filter { it.kind == TelemetryChannelKind.I32 }
        val BIT_SOURCES: List<TelemetryChannel> = entries.filter { it.kind == TelemetryChannelKind.BIT }

        fun fromStored(value: String?): TelemetryChannel? = when (value) {
            "PLOT_0" -> CH_1
            "PLOT_1" -> CH_2
            "PLOT_2" -> CH_3
            "PLOT_3" -> CH_4
            else -> entries.firstOrNull { it.name == value }
        }

        fun analogAt(index: Int): TelemetryChannel =
            ANALOG_CHANNELS.getOrElse(index) { CH_1 }

        fun u8At(index: Int): TelemetryChannel =
            U8_SOURCES.getOrElse(index) { CH_1 }
    }

    fun analogIndex(): Int? {
        val index = ANALOG_CHANNELS.indexOf(this)
        return index.takeIf { it >= 0 }
    }

    fun u8Index(): Int = U8_SOURCES.indexOf(this).coerceAtLeast(0)
}

/** Control Panel widgets that subscribe to a [TelemetryChannel]. */
enum class TelemetrySink(val kind: TelemetryChannelKind) {
    PLOT_0(TelemetryChannelKind.U8),
    PLOT_1(TelemetryChannelKind.U8),
    PLOT_2(TelemetryChannelKind.U8),
    PLOT_3(TelemetryChannelKind.U8),
    RADAR_ANGLE(TelemetryChannelKind.U8),
    RADAR_RANGE(TelemetryChannelKind.U8),
    ANALOG_GAUGE(TelemetryChannelKind.U8),
    BATTERY_GAUGE(TelemetryChannelKind.U8),
    PANEL_LEFT(TelemetryChannelKind.I32),
    PANEL_RIGHT(TelemetryChannelKind.I32),
    LED_0(TelemetryChannelKind.BIT),
    LED_1(TelemetryChannelKind.BIT),
    LED_2(TelemetryChannelKind.BIT),
    LED_3(TelemetryChannelKind.BIT),
    LED_4(TelemetryChannelKind.BIT),
    LED_5(TelemetryChannelKind.BIT),
    LED_6(TelemetryChannelKind.BIT),
    LED_7(TelemetryChannelKind.BIT),
    ;

    fun compatibleSources(): List<TelemetryChannel> = when (kind) {
        TelemetryChannelKind.U8 -> TelemetryChannel.U8_SOURCES
        TelemetryChannelKind.I32 -> TelemetryChannel.I32_SOURCES
        TelemetryChannelKind.BIT -> TelemetryChannel.BIT_SOURCES
    }

    companion object {
        fun fromStored(value: String?): TelemetrySink? =
            entries.firstOrNull { it.name == value }

        fun plotAt(index: Int): TelemetrySink = when (index) {
            0 -> PLOT_0
            1 -> PLOT_1
            2 -> PLOT_2
            else -> PLOT_3
        }
    }
}

/**
 * Phone-side map from widget to wire channel. Defaults match the stock Control Panel
 * (plots/radar = CH1…CH4 / `v0`…`v3`, analog/batt/panels/LEDs = RC:DATA fields).
 */
data class ChannelRouting(
    val sources: Map<TelemetrySink, TelemetryChannel> = DEFAULTS,
) {
    fun sourceFor(sink: TelemetrySink): TelemetryChannel {
        val bound = sources[sink]
        return if (bound != null && bound.kind == sink.kind) bound else DEFAULTS.getValue(sink)
    }

    fun with(sink: TelemetrySink, channel: TelemetryChannel): ChannelRouting {
        if (channel.kind != sink.kind) return padded()
        return ChannelRouting(padded().sources + (sink to channel))
    }

    fun padded(): ChannelRouting =
        ChannelRouting(TelemetrySink.entries.associateWith { sourceFor(it) })

    fun encode(): String = TelemetrySink.entries.joinToString(";") { sink ->
        "${sink.name}=${sourceFor(sink).name}"
    }

    fun isDefault(): Boolean = TelemetrySink.entries.all { sourceFor(it) == DEFAULTS.getValue(it) }

    companion object {
        val DEFAULTS: Map<TelemetrySink, TelemetryChannel> = mapOf(
            TelemetrySink.PLOT_0 to TelemetryChannel.CH_1,
            TelemetrySink.PLOT_1 to TelemetryChannel.CH_2,
            TelemetrySink.PLOT_2 to TelemetryChannel.CH_3,
            TelemetrySink.PLOT_3 to TelemetryChannel.CH_4,
            TelemetrySink.RADAR_ANGLE to TelemetryChannel.CH_1,
            TelemetrySink.RADAR_RANGE to TelemetryChannel.CH_2,
            TelemetrySink.ANALOG_GAUGE to TelemetryChannel.ANALOG,
            TelemetrySink.BATTERY_GAUGE to TelemetryChannel.BATT,
            TelemetrySink.PANEL_LEFT to TelemetryChannel.PANEL_LEFT,
            TelemetrySink.PANEL_RIGHT to TelemetryChannel.PANEL_RIGHT,
            TelemetrySink.LED_0 to TelemetryChannel.LED_0,
            TelemetrySink.LED_1 to TelemetryChannel.LED_1,
            TelemetrySink.LED_2 to TelemetryChannel.LED_2,
            TelemetrySink.LED_3 to TelemetryChannel.LED_3,
            TelemetrySink.LED_4 to TelemetryChannel.LED_4,
            TelemetrySink.LED_5 to TelemetryChannel.LED_5,
            TelemetrySink.LED_6 to TelemetryChannel.LED_6,
            TelemetrySink.LED_7 to TelemetryChannel.LED_7,
        )

        fun defaults(): ChannelRouting = ChannelRouting(DEFAULTS)

        fun decode(raw: String?): ChannelRouting {
            if (raw.isNullOrBlank()) return defaults()
            val parsed = mutableMapOf<TelemetrySink, TelemetryChannel>()
            raw.split(';').forEach { part ->
                val separator = part.indexOf('=')
                if (separator <= 0) return@forEach
                val sink = TelemetrySink.fromStored(part.substring(0, separator).trim()) ?: return@forEach
                val channel = TelemetryChannel.fromStored(part.substring(separator + 1).trim())
                    ?: return@forEach
                if (channel.kind == sink.kind) {
                    parsed[sink] = channel
                }
            }
            return ChannelRouting(DEFAULTS + parsed).padded()
        }
    }
}
