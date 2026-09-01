package com.micsbol.telecon4esp32.ui.rc_settings

import com.micsbol.telecon4esp32.domain.model.PlotCalibration
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.domain.model.parseCalibrationFloat
import com.micsbol.telecon4esp32.domain.model.toCalibrationDraftText

data class DisplayLabelDraft(
    val leftPanelUnit: String = "",
    val rightPanelUnit: String = "",
    val analogIndicatorUnit: String = "",
    val batteryLabel: String = "",
    val plotLabels: List<String> = List(UserSettings.PLOT_LABEL_COUNT) { "" },
    val plotOffsets: List<String> = List(UserSettings.PLOT_LABEL_COUNT) {
        PlotCalibration.DEFAULT_OFFSET.toCalibrationDraftText()
    },
    val plotSpans: List<String> = List(UserSettings.PLOT_LABEL_COUNT) {
        PlotCalibration.DEFAULT_SPAN.toCalibrationDraftText()
    },
    val plotUnits: List<String> = List(UserSettings.PLOT_LABEL_COUNT) { "" },
) {
    fun toPlotCalibrations(): List<PlotCalibration> =
        List(UserSettings.PLOT_LABEL_COUNT) { index ->
            val span = parseCalibrationFloat(
                plotSpans.getOrElse(index) { "" },
                PlotCalibration.DEFAULT_SPAN,
            ).let { parsed ->
                if (parsed == 0f) PlotCalibration.DEFAULT_SPAN else parsed
            }
            PlotCalibration(
                offset = parseCalibrationFloat(
                    plotOffsets.getOrElse(index) { "" },
                    PlotCalibration.DEFAULT_OFFSET,
                ),
                span = span,
                unit = plotUnits.getOrElse(index) { "" }.trim(),
            )
        }
}

fun UserSettings.toDisplayLabelDraft(): DisplayLabelDraft {
    val calibrations = PlotCalibration.padded(plotCalibrations)
    return DisplayLabelDraft(
        leftPanelUnit = leftPanelUnit,
        rightPanelUnit = rightPanelUnit,
        analogIndicatorUnit = analogIndicatorUnit,
        batteryLabel = batteryLabel,
        plotLabels = plotLabels,
        plotOffsets = calibrations.map { it.offset.toCalibrationDraftText() },
        plotSpans = calibrations.map { it.span.toCalibrationDraftText() },
        plotUnits = calibrations.map { it.unit },
    )
}
