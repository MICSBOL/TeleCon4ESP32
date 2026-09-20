package com.micsbol.telecon4esp32.domain.model

import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Analog span for one stick axis. [min] is down / left; [max] is up / right.
 * Rest is either [min] (range start) or [max] (range end).
 */
data class StickAxisRange(
    val min: Float = 0f,
    val max: Float = StickOutput.FULL_SCALE.toFloat(),
) {
    fun sanitized(): StickAxisRange {
        val lo = if (min.isFinite()) min else 0f
        val hi = if (max.isFinite()) max else StickOutput.FULL_SCALE.toFloat()
        return if (hi > lo) StickAxisRange(lo, hi) else StickAxisRange(lo, lo + 1f)
    }

    companion object {
        val DEFAULT = StickAxisRange()
    }
}

/**
 * Optional analog-channel binding for one stick.
 * When [enabled] is false the HUD hides the channel list; stored picks are kept.
 * Vertical and horizontal may each point at CH1…CH8, the same channel (both), or none.
 */
data class StickChannelLink(
    val enabled: Boolean = false,
    val vertical: TelemetryChannel? = null,
    val horizontal: TelemetryChannel? = null,
    val verticalRange: StickAxisRange = StickAxisRange.DEFAULT,
    val horizontalRange: StickAxisRange = StickAxisRange.DEFAULT,
) {
    val hasChannel: Boolean get() = vertical != null || horizontal != null

    fun assignedChannels(exceptAxis: JoystickAxis? = null): Set<TelemetryChannel> {
        if (!enabled) return emptySet()
        return buildSet {
            if (exceptAxis != JoystickAxis.VERTICAL) vertical?.let(::add)
            if (exceptAxis != JoystickAxis.HORIZONTAL) horizontal?.let(::add)
        }
    }

    fun withEnabled(
        on: Boolean,
        axis: JoystickAxis,
        occupied: Set<TelemetryChannel> = emptySet(),
    ): StickChannelLink {
        if (!on) return copy(enabled = false)
        val vertical = vertical?.takeUnless { it in occupied }
        val horizontal = horizontal?.takeUnless { it in occupied }
        return copy(enabled = true, vertical = vertical, horizontal = horizontal)
    }

    fun selecting(
        axis: JoystickAxis,
        channel: TelemetryChannel?,
        occupied: Set<TelemetryChannel> = emptySet(),
    ): StickChannelLink {
        val analog = analogOrNull(channel)
        if (analog != null && analog in occupied) return this
        val next = when (axis) {
            JoystickAxis.VERTICAL -> copy(
                vertical = analog,
                horizontal = if (horizontal == analog) null else horizontal,
            )
            JoystickAxis.HORIZONTAL -> copy(
                horizontal = analog,
                vertical = if (vertical == analog) null else vertical,
            )
            JoystickAxis.COMBINED -> this
        }
        return next.copy(enabled = next.vertical != null || next.horizontal != null)
    }

    fun toggling(
        axis: JoystickAxis,
        channel: TelemetryChannel,
        occupied: Set<TelemetryChannel> = emptySet(),
    ): StickChannelLink {
        val current = when (axis) {
            JoystickAxis.VERTICAL -> vertical
            JoystickAxis.HORIZONTAL -> horizontal
            JoystickAxis.COMBINED -> return this
        }
        return selecting(axis, if (current == channel) null else channel, occupied)
    }

    fun withRange(axis: JoystickAxis, range: StickAxisRange): StickChannelLink {
        val sanitized = range.sanitized()
        return when (axis) {
            JoystickAxis.VERTICAL -> copy(verticalRange = sanitized)
            JoystickAxis.HORIZONTAL -> copy(horizontalRange = sanitized)
            JoystickAxis.COMBINED -> copy(
                verticalRange = sanitized,
                horizontalRange = sanitized,
            )
        }
    }

    /**
     * Analog-bus samples when a channel is selected.
     * Down / left is range start (0); up / right is range end (1).
     */
    fun samples(
        x: Float,
        y: Float,
        axis: JoystickAxis,
        restX: Float = 0f,
        restY: Float = 0f,
    ): Map<TelemetryChannel, Float> {
        if (!enabled) return emptyMap()
        val out = linkedMapOf<TelemetryChannel, Float>()
        val xUnit = StickOutput.normalizedToUnit(x)
        val yUnit = StickOutput.normalizedToUnit(y)
        when (axis) {
            JoystickAxis.HORIZONTAL ->
                horizontal?.let { out[it] = xUnit }
            JoystickAxis.VERTICAL ->
                vertical?.let { out[it] = yUnit }
            JoystickAxis.COMBINED -> {
                val h = horizontal
                val v = vertical
                when {
                    h != null && v != null && h == v -> {
                        out[h] = if (abs(y) >= abs(x)) yUnit else xUnit
                    }
                    else -> {
                        if (h != null) out[h] = xUnit
                        if (v != null) out[v] = yUnit
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
        formatRangeValue(verticalRange.min),
        formatRangeValue(verticalRange.max),
        formatRangeValue(horizontalRange.min),
        formatRangeValue(horizontalRange.max),
    ).joinToString(",")

    companion object {
        val DEFAULT = StickChannelLink()
        val DEFAULT_LEFT = StickChannelLink()
        val DEFAULT_RIGHT = StickChannelLink()

        fun decode(raw: String?): StickChannelLink {
            if (raw.isNullOrBlank()) return DEFAULT
            val parts = raw.split(',')
            val enabled = parts.getOrNull(0)?.trim() == "1"
            return StickChannelLink(
                enabled = enabled,
                vertical = analogOrNull(TelemetryChannel.fromStored(parts.getOrNull(1)?.trim())),
                horizontal = analogOrNull(TelemetryChannel.fromStored(parts.getOrNull(2)?.trim())),
                verticalRange = decodeAxisRange(parts.getOrNull(3), parts.getOrNull(4)),
                horizontalRange = decodeAxisRange(parts.getOrNull(5), parts.getOrNull(6)),
            )
        }

        fun decodeAxisRange(minRaw: String?, maxRaw: String?): StickAxisRange {
            val min = minRaw?.trim()?.toFloatOrNull()?.takeIf { it.isFinite() } ?: 0f
            val max = maxRaw?.trim()?.toFloatOrNull()?.takeIf { it.isFinite() }
                ?: StickOutput.FULL_SCALE.toFloat()
            return StickAxisRange(min, max).sanitized()
        }

        fun formatRangeValue(value: Float): String = formatEngineeringNumber(value)

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

data class ExclusiveAnalogAssignments(
    val leftStick: StickChannelLink,
    val rightStick: StickChannelLink,
    val knobs: List<KnobChannelLink>,
)

fun exclusiveAnalogAssignments(
    leftStick: StickChannelLink,
    rightStick: StickChannelLink,
    knobs: List<KnobChannelLink>,
): ExclusiveAnalogAssignments {
    val taken = mutableSetOf<TelemetryChannel>()
    fun claimStick(link: StickChannelLink): StickChannelLink {
        if (!link.enabled) return link
        var vertical = link.vertical
        var horizontal = link.horizontal
        if (vertical != null) {
            if (vertical in taken) vertical = null else taken += vertical
        }
        if (horizontal != null) {
            if (horizontal in taken) horizontal = null else taken += horizontal
        }
        return link.copy(
            vertical = vertical,
            horizontal = horizontal,
        )
    }
    fun claimKnob(link: KnobChannelLink): KnobChannelLink {
        if (!link.enabled) return link
        val channel = link.channel
        if (channel == null) return link
        if (channel in taken) return link.copy(channel = null)
        taken += channel
        return link
    }
    return ExclusiveAnalogAssignments(
        leftStick = claimStick(leftStick),
        rightStick = claimStick(rightStick),
        knobs = knobs.map(::claimKnob),
    )
}

/**
 * Optional analog-channel binding for one knob. No channel is bound by default.
 */
data class KnobChannelLink(
    val enabled: Boolean = false,
    val channel: TelemetryChannel? = null,
    val range: StickAxisRange = StickAxisRange.DEFAULT,
) {
    fun assignedChannels(): Set<TelemetryChannel> =
        if (enabled) setOfNotNull(channel) else emptySet()

    fun withEnabled(
        on: Boolean,
        occupied: Set<TelemetryChannel> = emptySet(),
    ): KnobChannelLink {
        if (!on) return copy(enabled = false)
        return copy(enabled = true, channel = channel?.takeUnless { it in occupied })
    }

    fun selecting(
        channel: TelemetryChannel?,
        occupied: Set<TelemetryChannel> = emptySet(),
    ): KnobChannelLink {
        val next = StickChannelLink.analogOrNull(channel)
        if (next != null && next in occupied) return this
        return copy(channel = next, enabled = next != null)
    }

    fun toggling(
        channel: TelemetryChannel,
        occupied: Set<TelemetryChannel> = emptySet(),
    ): KnobChannelLink = selecting(if (this.channel == channel) null else channel, occupied)

    fun withRange(range: StickAxisRange): KnobChannelLink = copy(range = range.sanitized())

    fun sample(value01: Float): Map<TelemetryChannel, Float> {
        if (!enabled || channel == null) return emptyMap()
        return mapOf(channel to value01.coerceIn(0f, 1f))
    }

    fun encode(): String = listOf(
        if (enabled) "1" else "0",
        channel?.name.orEmpty(),
        StickChannelLink.formatRangeValue(range.min),
        StickChannelLink.formatRangeValue(range.max),
    ).joinToString(",")

    companion object {
        val DEFAULT = KnobChannelLink()

        fun decode(raw: String?): KnobChannelLink {
            if (raw.isNullOrBlank()) return DEFAULT
            val parts = raw.split(',')
            val channel = StickChannelLink.analogOrNull(
                TelemetryChannel.fromStored(parts.getOrNull(1)?.trim()),
            )
            return KnobChannelLink(
                enabled = parts.getOrNull(0)?.trim() == "1" && channel != null,
                channel = channel,
                range = StickChannelLink.decodeAxisRange(parts.getOrNull(2), parts.getOrNull(3)),
            )
        }
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
): Pair<Float, Float> {
    if (!enabled) {
        return Pair(stickX.coerceIn(-1f, 1f), stickY.coerceIn(-1f, 1f))
    }
    return Pair(
        displayAxis(horizontal, stickX, lastNormalized01),
        displayAxis(vertical, stickY, lastNormalized01),
    )
}

/**
 * Replaces plot traces whose routed analog channel is driven by a stick overlay.
 * Overlay samples are 0…1 (native 0…4094); plot calibration / HUD Y scale remap them.
 */
fun overlayStickOnPlotSeries(
    series: List<PlotData>,
    routing: ChannelRouting,
    overlay: Map<TelemetryChannel, List<Float>>,
): List<PlotData> {
    if (overlay.isEmpty()) return series
    return series.mapIndexed { index, plot ->
        val channel = routing.sourceFor(TelemetrySink.plotAt(index))
        val points = overlay[channel]
        if (points.isNullOrEmpty()) plot else plot.copy(dataPoints = points)
    }
}

fun StickChannelLink.displayHistory(
    stickX: List<Float>,
    stickY: List<Float>,
    history01: (TelemetryChannel) -> List<Float>?,
): Pair<List<Float>, List<Float>> {
    if (!enabled) return Pair(stickX, stickY)
    return Pair(
        displayAxisHistory(horizontal, stickX, history01),
        displayAxisHistory(vertical, stickY, history01),
    )
}

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
