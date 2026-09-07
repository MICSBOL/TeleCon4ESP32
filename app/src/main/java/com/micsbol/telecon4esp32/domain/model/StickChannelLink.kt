package com.micsbol.telecon4esp32.domain.model

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Optional analog-channel binding for one stick.
 * When [enabled] is false the HUD hides the channel list; stored picks are kept.
 * Vertical and horizontal may each point at CH1…CH8, the same channel (both), or none.
 */
data class StickChannelLink(
    val enabled: Boolean = false,
    val vertical: TelemetryChannel? = null,
    val horizontal: TelemetryChannel? = null,
) {
    fun withEnabled(on: Boolean, axis: JoystickAxis): StickChannelLink {
        if (!on) return copy(enabled = false)
        return copy(
            enabled = true,
            vertical = if (axis != JoystickAxis.HORIZONTAL) {
                vertical ?: DEFAULT_VERTICAL
            } else {
                vertical
            },
            horizontal = if (axis != JoystickAxis.VERTICAL) {
                horizontal ?: DEFAULT_HORIZONTAL
            } else {
                horizontal
            },
        )
    }

    fun toggling(axis: JoystickAxis, channel: TelemetryChannel): StickChannelLink =
        when (axis) {
            JoystickAxis.VERTICAL -> copy(
                vertical = if (vertical == channel) null else analogOrNull(channel),
            )
            JoystickAxis.HORIZONTAL -> copy(
                horizontal = if (horizontal == channel) null else analogOrNull(channel),
            )
            JoystickAxis.COMBINED -> this
        }

    /**
     * Analog-bus samples for enabled axes.
     * One-axis sticks map −1…1 onto 0…1 so the value fills the current radar span.
     * Combined sticks map polar angle onto [span] (180° or 270°) and magnitude onto range.
     */
    fun samples(
        x: Float,
        y: Float,
        axis: JoystickAxis,
        radarSpanDegrees: Float = 180f,
    ): Map<TelemetryChannel, Float> {
        if (!enabled) return emptyMap()
        val span = radarSpanDegrees
        val out = linkedMapOf<TelemetryChannel, Float>()
        when (axis) {
            JoystickAxis.HORIZONTAL ->
                horizontal?.let { out[it] = angleProgress(x, 0f, JoystickAxis.HORIZONTAL, span) }
            JoystickAxis.VERTICAL ->
                vertical?.let { out[it] = normalizedAxis(y) }
            JoystickAxis.COMBINED -> {
                val polar = angleProgress(x, y, JoystickAxis.COMBINED, span)
                val range = rangeProgress(x, y)
                val h = horizontal
                val v = vertical
                when {
                    h != null && v != null && h == v -> out[h] = polar
                    else -> {
                        if (h != null) out[h] = polar
                        if (v != null) out[v] = range
                    }
                }
            }
        }
        return out
    }

    fun encode(): String = listOf(
        if (enabled) "1" else "0",
        vertical?.name.orEmpty(),
        horizontal?.name.orEmpty(),
    ).joinToString(",")

    companion object {
        val DEFAULT = StickChannelLink()
        val DEFAULT_LEFT = StickChannelLink()
        val DEFAULT_RIGHT = StickChannelLink()
        val DEFAULT_VERTICAL = TelemetryChannel.CH_1
        val DEFAULT_HORIZONTAL = TelemetryChannel.CH_2

        fun decode(raw: String?): StickChannelLink {
            if (raw.isNullOrBlank()) return DEFAULT
            val parts = raw.split(',')
            val enabled = parts.getOrNull(0)?.trim() == "1"
            return StickChannelLink(
                enabled = enabled,
                vertical = analogOrNull(TelemetryChannel.fromStored(parts.getOrNull(1)?.trim())),
                horizontal = analogOrNull(TelemetryChannel.fromStored(parts.getOrNull(2)?.trim())),
            )
        }

        fun analogOrNull(channel: TelemetryChannel?): TelemetryChannel? =
            channel?.takeIf { it in TelemetryChannel.ANALOG_CHANNELS }

        fun normalizedAxis(value: Float): Float =
            ((value.coerceIn(-1f, 1f) + 1f) / 2f)

        /**
         * Maps stick travel onto 0…1 so radar can stretch it across 180° or 270°.
         * Combined: polar angle from north (up); 0 is the left edge of [spanDegrees].
         */
        fun angleProgress(
            x: Float,
            y: Float,
            axis: JoystickAxis,
            spanDegrees: Float,
        ): Float {
            val span = spanDegrees.coerceIn(180f, 270f)
            return when (axis) {
                JoystickAxis.HORIZONTAL -> normalizedAxis(x)
                JoystickAxis.VERTICAL -> normalizedAxis(y)
                JoystickAxis.COMBINED -> {
                    val nx = x.coerceIn(-1f, 1f)
                    val ny = y.coerceIn(-1f, 1f)
                    if (hypot(nx, ny) < 1e-4f) 0.5f else {
                        val degrees = atan2(nx, ny) * (180f / PI.toFloat())
                        val half = span / 2f
                        ((degrees + half) / span).coerceIn(0f, 1f)
                    }
                }
            }
        }

        fun rangeProgress(x: Float, y: Float): Float =
            hypot(x.coerceIn(-1f, 1f), y.coerceIn(-1f, 1f)).coerceIn(0f, 1f)

        fun merge(vararg maps: Map<TelemetryChannel, Float>): Map<TelemetryChannel, Float> {
            val out = linkedMapOf<TelemetryChannel, Float>()
            maps.forEach(out::putAll)
            return out
        }

        fun axisFromNormalized(normalized01: Float): Float =
            (normalized01.coerceIn(0f, 1f) * 2f - 1f).coerceIn(-1f, 1f)
    }
}

/**
 * XY for the stick graph: a bound analog channel (0…1) maps onto −1…1.
 * Unbound axes keep the live joystick value.
 */
fun StickChannelLink.displayXy(
    stickX: Float,
    stickY: Float,
    lastNormalized01: (TelemetryChannel) -> Float?,
): Pair<Float, Float> = Pair(
    displayAxis(horizontal, stickX, lastNormalized01),
    displayAxis(vertical, stickY, lastNormalized01),
)

fun StickChannelLink.displayHistory(
    stickX: List<Float>,
    stickY: List<Float>,
    history01: (TelemetryChannel) -> List<Float>?,
): Pair<List<Float>, List<Float>> = Pair(
    displayAxisHistory(horizontal, stickX, history01),
    displayAxisHistory(vertical, stickY, history01),
)

private fun displayAxis(
    channel: TelemetryChannel?,
    stick: Float,
    lastNormalized01: (TelemetryChannel) -> Float?,
): Float {
    if (channel == null) return stick.coerceIn(-1f, 1f)
    val normalized = lastNormalized01(channel) ?: return stick.coerceIn(-1f, 1f)
    return StickChannelLink.axisFromNormalized(normalized)
}

private fun displayAxisHistory(
    channel: TelemetryChannel?,
    stickHistory: List<Float>,
    history01: (TelemetryChannel) -> List<Float>?,
): List<Float> {
    if (channel == null) return stickHistory
    val samples = history01(channel)
    if (samples.isNullOrEmpty()) return stickHistory
    return samples.map { StickChannelLink.axisFromNormalized(it) }
}
