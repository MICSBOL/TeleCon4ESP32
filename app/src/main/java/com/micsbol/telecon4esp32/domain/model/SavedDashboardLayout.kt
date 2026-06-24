package com.micsbol.telecon4esp32.domain.model

data class SavedDashboardLayout(
    val id: String,
    val name: String,
    val widgets: List<DashboardWidgetPlacement>,
)

data class DashboardWidgetPlacement(
    val id: String,
    val type: DashboardWidgetType,
    val column: Int,
    val row: Int,
    val columnSpan: Int = 1,
    val rowSpan: Int = 1,
    val joystickMode: JoystickMode = JoystickMode.Spring(),
    val isOn: Boolean = false,
    val level: Float = 0.5f,
    val isPressed: Boolean = false,
    val readoutValue: Float = 3.30f,
)

enum class DashboardWidgetType {
    JOYSTICK,
    SLIDER,
    WAVEFORM,
    VALUE_READOUT,
    RELAY,
    OUTPUT_KNOB,
    PUSH_BUTTON,
    LED_INDICATOR,
}
