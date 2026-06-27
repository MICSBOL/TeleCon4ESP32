package com.micsbol.telecon4esp32.ui.rc_settings

import com.micsbol.telecon4esp32.domain.model.UserSettings

data class DisplayLabelDraft(
    val leftPanelUnit: String = "",
    val rightPanelUnit: String = "",
    val analogIndicatorUnit: String = "",
    val batteryLabel: String = "",
    val plotLabels: List<String> = List(UserSettings.PLOT_LABEL_COUNT) { "" },
)

fun UserSettings.toDisplayLabelDraft() = DisplayLabelDraft(
    leftPanelUnit = leftPanelUnit,
    rightPanelUnit = rightPanelUnit,
    analogIndicatorUnit = analogIndicatorUnit,
    batteryLabel = batteryLabel,
    plotLabels = plotLabels,
)
