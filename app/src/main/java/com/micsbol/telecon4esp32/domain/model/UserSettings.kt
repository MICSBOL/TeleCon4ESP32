package com.micsbol.telecon4esp32.domain.model

import com.micsbol.telecon4esp32.domain.model.JoystickMode

data class UserSettings(
    val leftStickMode: JoystickMode = JoystickMode.Spring(),
    val rightStickMode: JoystickMode = JoystickMode.Spring(),
    val leftStickRangeShape: JoystickRangeShape = JoystickRangeShape.CIRCLE,
    val rightStickRangeShape: JoystickRangeShape = JoystickRangeShape.CIRCLE,
    val switchInitialStates: Map<Int, Boolean> = (0..5).associateWith { false },
    val leftKnobInitialValue: Float = 0.5f,
    val rightKnobInitialValue: Float = 0.5f,
    val leftPanelUnit: String = "",
    val rightPanelUnit: String = "",
    val analogIndicatorUnit: String = "",
    val batteryLabel: String = "",
    val plotLabels: List<String> = List(PLOT_LABEL_COUNT) { "" },
    val plotCalibrations: List<PlotCalibration> = PlotCalibration.defaults(),
    val channelRouting: ChannelRouting = ChannelRouting.defaults(),
    val leftPanelOn: Boolean = true,
    val rightPanelOn: Boolean = true,
    val leftPanelColorGreen: Boolean = true,
    val rightPanelColorGreen: Boolean = true,
) {
    companion object {
        const val PLOT_LABEL_COUNT = 4
        /** Numbered analog bus on RC:PLOT / CC 33 (`v0`…`v7` = CH1…CH8). */
        const val ANALOG_CHANNEL_COUNT = 8
        const val PANEL_COLOR_GREEN_ARGB = 0xFF00FF00.toInt()
        const val PANEL_COLOR_RED_ARGB = 0xFFFF0000.toInt()

        fun panelColorArgb(isGreen: Boolean): Int =
            if (isGreen) PANEL_COLOR_GREEN_ARGB else PANEL_COLOR_RED_ARGB
    }
}