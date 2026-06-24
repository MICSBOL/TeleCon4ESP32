package com.micsbol.telecon4esp32.ui.customdashboard

import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.domain.model.DashboardWidgetPlacement
import com.micsbol.telecon4esp32.domain.model.DashboardWidgetType
import com.micsbol.telecon4esp32.domain.model.SavedDashboardLayout

enum class CustomDashboardScreenPhase {
    HOME,
    EDITOR,
}

data class DashboardWaveformStats(
    val rmsVolts: Float = 0.71f,
    val frequencyHz: Int = 120,
    val peakVolts: Float = 1.02f,
    val averageVolts: Float = 0.02f,
)

data class CustomDashboardEditorState(
    val layoutId: String,
    val layoutName: String = "",
    val isEditMode: Boolean = true,
    val widgets: List<DashboardWidgetPlacement> = emptyList(),
    val joystickPositions: Map<String, Pair<Float, Float>> = emptyMap(),
    val joystickConfigWidgetId: String? = null,
    val plotSeries: List<PlotData> = emptyList(),
    val plotRevision: Long = 0L,
    val waveformStats: DashboardWaveformStats = DashboardWaveformStats(),
    val selectedWidgetId: String? = null,
    val isEsp32Connected: Boolean = true,
    val saveConfirmationVisible: Boolean = false,
    val saveNameDialogVisible: Boolean = false,
    val saveNameDialogIsRename: Boolean = false,
    val pendingSaveName: String = "",
)

data class CustomDashboardUiState(
    val phase: CustomDashboardScreenPhase = CustomDashboardScreenPhase.HOME,
    val savedDashboards: List<SavedDashboardLayout> = emptyList(),
    val editor: CustomDashboardEditorState? = null,
    val pendingDeleteLayoutId: String? = null,
)

fun DashboardWidgetType.defaultColumnSpan(): Int = when (this) {
    DashboardWidgetType.JOYSTICK -> 3
    DashboardWidgetType.SLIDER -> 3
    DashboardWidgetType.WAVEFORM -> 4
    DashboardWidgetType.VALUE_READOUT -> 3
    DashboardWidgetType.RELAY -> 2
    DashboardWidgetType.OUTPUT_KNOB -> 3
    DashboardWidgetType.PUSH_BUTTON -> 2
    DashboardWidgetType.LED_INDICATOR -> 2
}

fun DashboardWidgetType.defaultRowSpan(): Int = when (this) {
    DashboardWidgetType.JOYSTICK -> 3
    DashboardWidgetType.SLIDER -> 2
    DashboardWidgetType.WAVEFORM -> 3
    DashboardWidgetType.VALUE_READOUT -> 2
    DashboardWidgetType.RELAY -> 1
    DashboardWidgetType.OUTPUT_KNOB -> 3
    DashboardWidgetType.PUSH_BUTTON -> 1
    DashboardWidgetType.LED_INDICATOR -> 1
}

fun DashboardWidgetType.titleRes(): Int = when (this) {
    DashboardWidgetType.JOYSTICK -> R.string.custom_dashboard_joystick_title
    DashboardWidgetType.SLIDER -> R.string.custom_dashboard_slider_title
    DashboardWidgetType.WAVEFORM -> R.string.custom_dashboard_waveform_title
    DashboardWidgetType.VALUE_READOUT -> R.string.custom_dashboard_value_readout_title
    DashboardWidgetType.RELAY -> R.string.custom_dashboard_palette_relay_title
    DashboardWidgetType.OUTPUT_KNOB -> R.string.custom_dashboard_output_title
    DashboardWidgetType.PUSH_BUTTON -> R.string.custom_dashboard_push_button_title
    DashboardWidgetType.LED_INDICATOR -> R.string.custom_dashboard_led_title
}

fun DashboardWidgetType.descriptionRes(): Int = when (this) {
    DashboardWidgetType.JOYSTICK -> R.string.custom_dashboard_palette_joystick_desc
    DashboardWidgetType.SLIDER -> R.string.custom_dashboard_palette_slider_desc
    DashboardWidgetType.WAVEFORM -> R.string.custom_dashboard_palette_waveform_desc
    DashboardWidgetType.VALUE_READOUT -> R.string.custom_dashboard_palette_value_readout_desc
    DashboardWidgetType.RELAY -> R.string.custom_dashboard_palette_relay_desc
    DashboardWidgetType.OUTPUT_KNOB -> R.string.custom_dashboard_palette_output_desc
    DashboardWidgetType.PUSH_BUTTON -> R.string.custom_dashboard_palette_push_button_desc
    DashboardWidgetType.LED_INDICATOR -> R.string.custom_dashboard_palette_led_desc
}

fun DashboardWidgetType.categoryRes(): Int = when (this) {
    DashboardWidgetType.JOYSTICK -> R.string.custom_dashboard_palette_category_input
    DashboardWidgetType.SLIDER -> R.string.custom_dashboard_palette_category_input
    DashboardWidgetType.WAVEFORM -> R.string.custom_dashboard_palette_category_monitor
    DashboardWidgetType.VALUE_READOUT -> R.string.custom_dashboard_palette_category_monitor
    DashboardWidgetType.RELAY -> R.string.custom_dashboard_palette_category_output
    DashboardWidgetType.OUTPUT_KNOB -> R.string.custom_dashboard_palette_category_output
    DashboardWidgetType.PUSH_BUTTON -> R.string.custom_dashboard_palette_category_output
    DashboardWidgetType.LED_INDICATOR -> R.string.custom_dashboard_palette_category_output
}

fun joystickStickPosition(
    widgetId: String,
    positions: Map<String, Pair<Float, Float>>,
): Pair<Float, Float> = positions[widgetId] ?: Pair(0f, 0f)

fun defaultWidgetStateForType(type: DashboardWidgetType): DashboardWidgetStateDefaults =
    DashboardWidgetStateDefaults(
        isOn = type == DashboardWidgetType.LED_INDICATOR,
        level = when (type) {
            DashboardWidgetType.SLIDER -> 0.45f
            DashboardWidgetType.OUTPUT_KNOB -> 0.65f
            else -> 0.5f
        },
        readoutValue = 3.30f,
    )

data class DashboardWidgetStateDefaults(
    val isOn: Boolean = false,
    val level: Float = 0.5f,
    val isPressed: Boolean = false,
    val readoutValue: Float = 3.30f,
)

fun displayDashboardName(name: String, untitledLabel: String): String =
    name.trim().ifBlank { untitledLabel }

fun isDashboardPersisted(
    layoutId: String,
    savedDashboards: List<SavedDashboardLayout>,
): Boolean = savedDashboards.any { it.id == layoutId }

fun demoPlotSeries(points: List<Float>): List<PlotData> = listOf(
    PlotData(
        name = "CH1",
        dataPoints = points,
        colorArgb = 0xFF22D3EE.toInt(),
    ),
)

fun findAvailableGridSlot(
    widgets: List<DashboardWidgetPlacement>,
    columnSpan: Int,
    rowSpan: Int,
    gridColumns: Int,
    gridRows: Int,
): Pair<Int, Int>? {
    for (row in 0..(gridRows - rowSpan)) {
        for (column in 0..(gridColumns - columnSpan)) {
            if (!hasWidgetOverlap(
                    widgetId = "",
                    column = column,
                    row = row,
                    columnSpan = columnSpan,
                    rowSpan = rowSpan,
                    widgets = widgets,
                )
            ) {
                return column to row
            }
        }
    }
    return null
}

fun hasWidgetOverlap(
    widgetId: String,
    column: Int,
    row: Int,
    columnSpan: Int,
    rowSpan: Int,
    widgets: List<DashboardWidgetPlacement>,
): Boolean {
    val left = column
    val top = row
    val right = column + columnSpan
    val bottom = row + rowSpan

    return widgets.any { other ->
        if (other.id == widgetId) return@any false
        val otherLeft = other.column
        val otherTop = other.row
        val otherRight = other.column + other.columnSpan
        val otherBottom = other.row + other.rowSpan

        left < otherRight && right > otherLeft && top < otherBottom && bottom > otherTop
    }
}
