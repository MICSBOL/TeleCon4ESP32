package com.micsbol.telecon4esp32.ui.control_panel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.JoystickAxis
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.model.JoystickRangeShape
import com.micsbol.telecon4esp32.domain.model.TelemetryChannel
import com.micsbol.telecon4esp32.domain.model.TelemetrySink
import com.micsbol.telecon4esp32.ui.rc_settings.labelRes
import com.micsbol.telecon4esp32.ui.theme.AppGlass
import com.micsbol.telecon4esp32.ui.theme.Neo

@Composable
fun TelemetryWidgetOptionsMenu(
    expanded: Boolean,
    sink: TelemetrySink,
    selectedChannel: TelemetryChannel,
    widgetLabel: String,
    onChannelSelected: (TelemetryChannel) -> Unit,
    onLabelChange: (String) -> Unit,
    onDismiss: () -> Unit,
    panelOn: Boolean? = null,
    onPanelOnChange: ((Boolean) -> Unit)? = null,
    panelColorGreen: Boolean? = null,
    onPanelColorGreenChange: ((Boolean) -> Unit)? = null,
    extraContent: @Composable ColumnScope.(closeMenu: () -> Unit) -> Unit = {},
) {
    var labelText by remember(expanded) { mutableStateOf(widgetLabel) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val closeMenu = {
        onLabelChange(labelText)
        keyboardController?.hide()
        focusManager.clearFocus()
        onDismiss()
    }
    val fieldColors = hudMenuOutlinedFieldColors()

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = closeMenu,
        containerColor = AppGlass.DialogSurface,
    ) {
        CompositionLocalProvider(LocalContentColor provides Neo.TextPrimary) {
        Column(
            modifier = Modifier
                .heightIn(max = 280.dp)
                .width(240.dp)
                .verticalScroll(rememberScrollState()),
        ) {
        OutlinedTextField(
            value = labelText,
            onValueChange = { labelText = it },
            label = {
                Text(
                    text = stringResource(R.string.control_panel_widget_config_label),
                    color = Neo.TextPrimary,
                )
            },
            placeholder = {
                Text(
                    text = stringResource(sink.widgetLabelHintRes()),
                    color = Neo.TextMuted,
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Neo.TextPrimary),
            singleLine = true,
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { closeMenu() },
            ),
            trailingIcon = {
                IconButton(onClick = closeMenu) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.control_panel_widget_config_done),
                        tint = Neo.Accent,
                    )
                }
            },
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .width(220.dp),
        )
        HorizontalDivider(color = Neo.TextSecondary.copy(alpha = 0.3f))
        sink.compatibleSources().forEach { channel ->
            TelemetryOptionsMenuItem(
                label = stringResource(channel.labelRes()),
                selected = channel == selectedChannel,
                onClick = {
                    onChannelSelected(channel)
                    closeMenu()
                },
            )
        }
        if (panelOn != null && onPanelOnChange != null) {
            HorizontalDivider(color = Neo.TextSecondary.copy(alpha = 0.3f))
            TelemetryOptionsMenuItem(
                label = stringResource(R.string.rc_controller_settings_panel_on),
                selected = panelOn,
                onClick = {
                    onPanelOnChange(true)
                    closeMenu()
                },
            )
            TelemetryOptionsMenuItem(
                label = stringResource(R.string.rc_controller_settings_panel_off),
                selected = !panelOn,
                onClick = {
                    onPanelOnChange(false)
                    closeMenu()
                },
            )
        }
        if (panelColorGreen != null && onPanelColorGreenChange != null) {
            HorizontalDivider(color = Neo.TextSecondary.copy(alpha = 0.3f))
            TelemetryOptionsMenuItem(
                label = stringResource(R.string.rc_controller_settings_panel_color_green),
                selected = panelColorGreen,
                onClick = {
                    onPanelColorGreenChange(true)
                    closeMenu()
                },
            )
            TelemetryOptionsMenuItem(
                label = stringResource(R.string.rc_controller_settings_panel_color_red),
                selected = !panelColorGreen,
                onClick = {
                    onPanelColorGreenChange(false)
                    closeMenu()
                },
            )
        }
        extraContent(closeMenu)
        }
        }
    }
}

@Composable
private fun TelemetryOptionsMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                color = if (selected) Neo.Accent else Neo.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        onClick = onClick,
        trailingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Neo.Accent,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            null
        },
        colors = MenuDefaults.itemColors(
            textColor = Neo.TextPrimary,
            trailingIconColor = Neo.Accent,
        ),
    )
}

private fun TelemetrySink.widgetLabelHintRes(): Int = when (this) {
    TelemetrySink.PANEL_LEFT -> R.string.rc_controller_settings_left_panel_unit_hint
    TelemetrySink.PANEL_RIGHT -> R.string.rc_controller_settings_right_panel_unit_hint
    TelemetrySink.ANALOG_GAUGE -> R.string.rc_controller_settings_analog_indicator_unit_hint
    TelemetrySink.BATTERY_GAUGE -> R.string.rc_controller_settings_battery_label_hint
    TelemetrySink.PLOT_0 -> R.string.rc_controller_settings_plot_label_hint_1
    TelemetrySink.PLOT_1 -> R.string.rc_controller_settings_plot_label_hint_2
    TelemetrySink.PLOT_2 -> R.string.rc_controller_settings_plot_label_hint_3
    TelemetrySink.PLOT_3 -> R.string.rc_controller_settings_plot_label_hint_4
    else -> R.string.control_panel_widget_config_label
}

@Composable
internal fun hudMenuOutlinedFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Neo.TextPrimary,
    unfocusedTextColor = Neo.TextPrimary,
    focusedBorderColor = Neo.Accent,
    unfocusedBorderColor = Neo.TextSecondary.copy(alpha = 0.55f),
    focusedLabelColor = Neo.TextPrimary,
    unfocusedLabelColor = Neo.TextPrimary,
    disabledLabelColor = Neo.TextSecondary,
    cursorColor = Neo.Accent,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedPlaceholderColor = Neo.TextMuted,
    unfocusedPlaceholderColor = Neo.TextMuted,
)

@Composable
fun StickOptionsMenu(
    expanded: Boolean,
    selectedMode: JoystickMode,
    onModeSelected: (JoystickMode) -> Unit,
    onDismiss: () -> Unit,
    selectedRangeShape: JoystickRangeShape = JoystickRangeShape.CIRCLE,
    onRangeShapeSelected: (JoystickRangeShape) -> Unit = {},
) {
    var draft by remember(expanded) { mutableStateOf(selectedMode) }
    var rangeDraft by remember(expanded) { mutableStateOf(selectedRangeShape) }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = AppGlass.DialogSurface,
    ) {
        CompositionLocalProvider(LocalContentColor provides Neo.TextPrimary) {
        Column(
            modifier = Modifier
                .heightIn(max = 280.dp)
                .width(240.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            TelemetryOptionsMenuItem(
                label = stringResource(R.string.rc_joystick_mode_spring),
                selected = draft.isSpring,
                onClick = {
                    val next = draft.withSpring(true)
                    draft = next
                    onModeSelected(next)
                },
            )
            TelemetryOptionsMenuItem(
                label = stringResource(R.string.rc_joystick_mode_hold),
                selected = !draft.isSpring,
                onClick = {
                    val next = draft.withSpring(false)
                    draft = next
                    onModeSelected(next)
                },
            )
            HorizontalDivider(color = Neo.TextSecondary.copy(alpha = 0.3f))
            TelemetryOptionsMenuItem(
                label = stringResource(R.string.rc_joystick_axis_vertical),
                selected = draft.axis != JoystickAxis.HORIZONTAL,
                onClick = {
                    val next = draft.togglingAxis(JoystickAxis.VERTICAL)
                    draft = next
                    onModeSelected(next)
                },
            )
            TelemetryOptionsMenuItem(
                label = stringResource(R.string.rc_joystick_axis_horizontal),
                selected = draft.axis != JoystickAxis.VERTICAL,
                onClick = {
                    val next = draft.togglingAxis(JoystickAxis.HORIZONTAL)
                    draft = next
                    onModeSelected(next)
                },
            )
            HorizontalDivider(color = Neo.TextSecondary.copy(alpha = 0.3f))
            TelemetryOptionsMenuItem(
                label = stringResource(R.string.rc_joystick_range_circle),
                selected = rangeDraft == JoystickRangeShape.CIRCLE,
                onClick = {
                    rangeDraft = JoystickRangeShape.CIRCLE
                    onRangeShapeSelected(JoystickRangeShape.CIRCLE)
                },
            )
            TelemetryOptionsMenuItem(
                label = stringResource(R.string.rc_joystick_range_square),
                selected = rangeDraft == JoystickRangeShape.SQUARE,
                onClick = {
                    rangeDraft = JoystickRangeShape.SQUARE
                    onRangeShapeSelected(JoystickRangeShape.SQUARE)
                },
            )
            HorizontalDivider(color = Neo.TextSecondary.copy(alpha = 0.3f))
            draft.allowedRestPositions().forEach { position ->
                TelemetryOptionsMenuItem(
                    label = stringResource(position.restPositionLabelRes(draft.axis)),
                    selected = draft.initialPosition == position,
                    onClick = {
                        val next = draft.withInitialPosition(position)
                        draft = next
                        onModeSelected(next)
                        onDismiss()
                    },
                )
            }
        }
        }
    }
}

private fun Pair<Int, Int>.restPositionLabelRes(axis: JoystickAxis): Int {
    val combined = axis == JoystickAxis.COMBINED
    return when (this) {
        JoystickMode.UP -> if (combined) {
            R.string.rc_joystick_position_all_up
        } else {
            R.string.rc_joystick_position_up
        }
        JoystickMode.DOWN -> if (combined) {
            R.string.rc_joystick_position_all_down
        } else {
            R.string.rc_joystick_position_down
        }
        JoystickMode.LEFT -> if (combined) {
            R.string.rc_joystick_position_all_left
        } else {
            R.string.rc_joystick_position_left
        }
        JoystickMode.RIGHT -> if (combined) {
            R.string.rc_joystick_position_all_right
        } else {
            R.string.rc_joystick_position_right
        }
        else -> R.string.rc_joystick_position_center
    }
}
