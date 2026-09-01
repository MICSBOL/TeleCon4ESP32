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
