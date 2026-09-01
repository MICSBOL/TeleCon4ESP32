package com.micsbol.telecon4esp32.domain.session

import com.micsbol.telecon4esp32.domain.model.PlotCalibration
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.domain.model.toPlotByte
import java.util.Locale

/**
 * CSV for a Control Panel session. Phone-only: plots are stored as both the raw 0–255
 * byte and the calibrated engineering value. Sticks are the values already sent as RC:CTRL.
 */
object SessionCsv {
    private const val PLOT_COUNT = UserSettings.ANALOG_CHANNEL_COUNT

    fun header(): String = buildString {
        append("timestamp_ms,elapsed_s")
        repeat(PLOT_COUNT) { index ->
            append(",plot${index}_raw,plot$index")
        }
        append(",left,right,analog,batt,led,lx,ly,rx,ry,lk,rk,sw")
        append('\n')
    }

    fun commentLines(
        labels: List<String>,
        calibrations: List<PlotCalibration>,
    ): String = buildString {
        append("# TeleCon4ESP32 Control Panel session")
        append('\n')
        repeat(PLOT_COUNT) { index ->
            val calibration = calibrations.getOrElse(index) { PlotCalibration.DEFAULT }
            val label = labels.getOrElse(index) { "" }.ifBlank { "CH${index + 1}" }
            append("# plot$index=")
            append(escapeComment(label))
            append(" offset=")
            append(formatCsvFloat(calibration.offset))
            append(" span=")
            append(formatCsvFloat(calibration.span))
            append(" unit=")
            append(escapeComment(calibration.unit))
            append(" value=offset+(byte/255)*span")
            append('\n')
        }
    }

    fun row(
        timestampMs: Long,
        elapsedMs: Long,
        plotNormalized: List<Float?>,
        calibrations: List<PlotCalibration>,
        leftPanel: Int,
        rightPanel: Int,
        analog: Int,
        battery: Int,
        led: Int,
        leftStickX: Float,
        leftStickY: Float,
        rightStickX: Float,
        rightStickY: Float,
        leftKnob: Float,
        rightKnob: Float,
        switches: List<Boolean>,
    ): String = buildString {
        append(timestampMs)
        append(',')
        append(formatCsvFloat(elapsedMs / 1000f))
        repeat(PLOT_COUNT) { index ->
            val normalized = plotNormalized.getOrNull(index)
            val calibration = calibrations.getOrElse(index) { PlotCalibration.DEFAULT }
            append(',')
            if (normalized == null) {
                append(',')
            } else {
                append(normalized.toPlotByte())
                append(',')
                append(formatCsvFloat(calibration.toEngineering(normalized)))
            }
        }
        append(',')
        append(leftPanel)
        append(',')
        append(rightPanel)
        append(',')
        append(analog)
        append(',')
        append(battery)
        append(',')
        append(String.format(Locale.US, "%02X", led and 0xFF))
        append(',')
        append(formatCsvFloat(leftStickX))
        append(',')
        append(formatCsvFloat(leftStickY))
        append(',')
        append(formatCsvFloat(rightStickX))
        append(',')
        append(formatCsvFloat(rightStickY))
        append(',')
        append(formatCsvFloat(leftKnob))
        append(',')
        append(formatCsvFloat(rightKnob))
        append(',')
        append(packSwitchBits(switches))
        append('\n')
    }

    private fun packSwitchBits(switches: List<Boolean>): String {
        var bits = 0
        switches.forEachIndexed { index, on ->
            if (on && index < 8) bits = bits or (1 shl index)
        }
        return String.format(Locale.US, "%02X", bits)
    }

    private fun formatCsvFloat(value: Float): String =
        String.format(Locale.US, "%.5f", value)

    private fun escapeComment(value: String): String =
        value.replace('\n', ' ').replace(',', ';')
}
