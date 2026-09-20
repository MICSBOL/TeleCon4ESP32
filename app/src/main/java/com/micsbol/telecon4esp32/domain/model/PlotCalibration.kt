package com.micsbol.telecon4esp32.domain.model

import java.util.Locale
import kotlin.math.abs

/**
 * Phone-side mapping from an ESP32 plot byte (0–255, stored as 0f–1f) to engineering units.
 *
 * `value = offset + normalized * span`
 */
data class PlotCalibration(
    val offset: Float = DEFAULT_OFFSET,
    val span: Float = DEFAULT_SPAN,
    val unit: String = "",
) {
    fun toEngineering(normalized: Float): Float =
        offset + normalized.coerceIn(0f, 1f) * span

    fun formatEngineering(normalized: Float): String {
        val number = formatEngineeringNumber(toEngineering(normalized))
        val trimmedUnit = unit.trim()
        return if (trimmedUnit.isEmpty()) number else "$number $trimmedUnit"
    }

    companion object {
        const val DEFAULT_OFFSET = 0f
        const val DEFAULT_SPAN = 255f

        val DEFAULT = PlotCalibration()

        fun defaults(count: Int = UserSettings.PLOT_LABEL_COUNT): List<PlotCalibration> =
            List(count) { DEFAULT }

        fun padded(list: List<PlotCalibration>, count: Int = UserSettings.PLOT_LABEL_COUNT): List<PlotCalibration> =
            List(count) { index -> list.getOrElse(index) { DEFAULT } }
    }
}

fun PlotCalibration.resolvedYRange(): Pair<Float, Float> {
    val min = if (offset.isFinite()) offset else PlotCalibration.DEFAULT_OFFSET
    val rawSpan = if (span.isFinite()) span else PlotCalibration.DEFAULT_SPAN
    val max = min + rawSpan
    return if (max > min) min to max else min to (min + 1f)
}

fun plotScaleTicks(min: Float, max: Float, count: Int = 3): List<Float> {
    val (lo, hi) = if (min.isFinite() && max.isFinite() && max > min) {
        min to max
    } else {
        0f to 1f
    }
    if (count <= 1) return listOf(lo)
    val last = (count - 1).toFloat()
    return List(count) { index -> lo + (hi - lo) * index / last }
}

/**
 * Normalized plot height of engineering 0: 0 at the bottom (Y min), 1 at the top (Y max).
 * Null when 0 is outside the visible range.
 */
fun PlotCalibration.zeroLineNormalized(): Float? {
    val (min, max) = resolvedYRange()
    if (0f < min || 0f > max) return null
    val span = max - min
    if (span <= 0f) return null
    return ((0f - min) / span).coerceIn(0f, 1f)
}

fun Float.toPlotByte(): Int = (this.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)

fun formatEngineeringNumber(value: Float): String {
    val abs = abs(value)
    val formatted = when {
        abs >= 100f -> String.format(Locale.US, "%.0f", value)
        abs >= 10f -> String.format(Locale.US, "%.1f", value)
        abs >= 1f -> String.format(Locale.US, "%.2f", value)
        else -> String.format(Locale.US, "%.3f", value)
    }
    return if (formatted.contains('.')) {
        formatted.trimEnd('0').trimEnd('.')
    } else {
        formatted
    }
}

fun parseCalibrationFloat(text: String, default: Float): Float {
    val parsed = text.trim().replace(',', '.').toFloatOrNull() ?: return default
    return if (parsed.isFinite()) parsed else default
}

fun Float.toCalibrationDraftText(): String {
    if (!isFinite()) return "0"
    return if (this == toLong().toFloat()) {
        toLong().toString()
    } else {
        String.format(Locale.US, "%.4f", this).trimEnd('0').trimEnd('.')
    }
}
