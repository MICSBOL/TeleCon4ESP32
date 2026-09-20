package com.micsbol.telecon4esp32.ui.control_panel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.JoystickAxis
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.model.JoystickRangeShape
import com.micsbol.telecon4esp32.domain.model.KnobChannelLink
import com.micsbol.telecon4esp32.domain.model.StickAxisRange
import com.micsbol.telecon4esp32.domain.model.StickChannelLink
import com.micsbol.telecon4esp32.domain.model.TelemetryChannel
import com.micsbol.telecon4esp32.domain.model.TelemetrySink
import com.micsbol.telecon4esp32.domain.model.parseCalibrationFloat
import com.micsbol.telecon4esp32.domain.model.toCalibrationDraftText
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
    channelLink: StickChannelLink = StickChannelLink.DEFAULT,
    onChannelLinkChange: (StickChannelLink) -> Unit = {},
    occupiedChannels: Set<TelemetryChannel> = emptySet(),
) {
    var draft by remember(expanded) { mutableStateOf(selectedMode) }
    var rangeDraft by remember(expanded) { mutableStateOf(selectedRangeShape) }
    var channelDraft by remember(expanded) { mutableStateOf(channelLink) }
    val showVertical = draft.axis != JoystickAxis.HORIZONTAL
    val showHorizontal = draft.axis != JoystickAxis.VERTICAL
    val fieldColors = hudMenuOutlinedFieldColors()
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = AppGlass.DialogSurface,
    ) {
        CompositionLocalProvider(LocalContentColor provides Neo.TextPrimary) {
        Column(
            modifier = Modifier
                .heightIn(max = 360.dp)
                .width(252.dp)
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
                selected = showVertical,
                onClick = {
                    val next = draft.togglingAxis(JoystickAxis.VERTICAL)
                    draft = next
                    onModeSelected(next)
                },
            )
            TelemetryOptionsMenuItem(
                label = stringResource(R.string.rc_joystick_axis_horizontal),
                selected = showHorizontal,
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
            if (showVertical) {
                StickMenuAxisSetup(
                    axisLabel = stringResource(R.string.rc_joystick_axis_vertical),
                    range = channelDraft.verticalRange,
                    selectedChannel = channelDraft.vertical,
                    occupiedChannels = occupiedChannels +
                        channelDraft.assignedChannels(JoystickAxis.VERTICAL),
                    colors = fieldColors,
                    onRangeChange = { range ->
                        val next = channelDraft.withRange(JoystickAxis.VERTICAL, range)
                        channelDraft = next
                        onChannelLinkChange(next)
                    },
                    onChannelSelected = { channel ->
                        val occupied = occupiedChannels +
                            channelDraft.assignedChannels(JoystickAxis.VERTICAL)
                        val next = channelDraft.selecting(JoystickAxis.VERTICAL, channel, occupied)
                        channelDraft = next
                        onChannelLinkChange(next)
                    },
                )
            }
            if (showHorizontal) {
                StickMenuAxisSetup(
                    axisLabel = stringResource(R.string.rc_joystick_axis_horizontal),
                    range = channelDraft.horizontalRange,
                    selectedChannel = channelDraft.horizontal,
                    occupiedChannels = occupiedChannels +
                        channelDraft.assignedChannels(JoystickAxis.HORIZONTAL),
                    colors = fieldColors,
                    onRangeChange = { range ->
                        val next = channelDraft.withRange(JoystickAxis.HORIZONTAL, range)
                        channelDraft = next
                        onChannelLinkChange(next)
                    },
                    onChannelSelected = { channel ->
                        val occupied = occupiedChannels +
                            channelDraft.assignedChannels(JoystickAxis.HORIZONTAL)
                        val next = channelDraft.selecting(
                            JoystickAxis.HORIZONTAL,
                            channel,
                            occupied,
                        )
                        channelDraft = next
                        onChannelLinkChange(next)
                    },
                )
            }
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

@Composable
fun KnobOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    channelLink: KnobChannelLink = KnobChannelLink.DEFAULT,
    onChannelLinkChange: (KnobChannelLink) -> Unit = {},
    occupiedChannels: Set<TelemetryChannel> = emptySet(),
) {
    var channelDraft by remember(expanded) { mutableStateOf(channelLink) }
    val fieldColors = hudMenuOutlinedFieldColors()
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = AppGlass.DialogSurface,
    ) {
        CompositionLocalProvider(LocalContentColor provides Neo.TextPrimary) {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .width(252.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.rc_vehicle_knob_options_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = Neo.TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
                StickMenuRangeFields(
                    range = channelDraft.range,
                    colors = fieldColors,
                    onRangeChange = { range ->
                        val next = channelDraft.withRange(range)
                        channelDraft = next
                        onChannelLinkChange(next)
                    },
                )
                HorizontalDivider(color = Neo.TextSecondary.copy(alpha = 0.3f))
                TelemetryOptionsMenuItem(
                    label = stringResource(R.string.rc_vehicle_stick_use_channel),
                    selected = channelDraft.enabled,
                    onClick = {
                        val next = channelDraft.withEnabled(!channelDraft.enabled, occupiedChannels)
                        channelDraft = next
                        onChannelLinkChange(next)
                    },
                )
                if (channelDraft.enabled) {
                    StickMenuChannelPicker(
                        selected = channelDraft.channel,
                        occupiedChannels = occupiedChannels,
                        onChannelSelected = { channel ->
                            val next = channelDraft.selecting(channel, occupiedChannels)
                            channelDraft = next
                            onChannelLinkChange(next)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StickMenuAxisSetup(
    axisLabel: String,
    range: StickAxisRange,
    selectedChannel: TelemetryChannel?,
    occupiedChannels: Set<TelemetryChannel>,
    colors: TextFieldColors,
    onRangeChange: (StickAxisRange) -> Unit,
    onChannelSelected: (TelemetryChannel?) -> Unit,
) {
    HorizontalDivider(color = Neo.TextSecondary.copy(alpha = 0.3f))
    Text(
        text = axisLabel,
        style = MaterialTheme.typography.labelSmall,
        color = Neo.TextSecondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )
    StickMenuRangeFields(
        range = range,
        colors = colors,
        onRangeChange = onRangeChange,
    )
    StickMenuChannelPicker(
        selected = selectedChannel,
        occupiedChannels = occupiedChannels,
        onChannelSelected = onChannelSelected,
    )
}

@Composable
private fun StickMenuRangeFields(
    range: StickAxisRange,
    colors: TextFieldColors,
    onRangeChange: (StickAxisRange) -> Unit,
) {
    var minText by remember(range) { mutableStateOf(range.min.toCalibrationDraftText()) }
    var maxText by remember(range) { mutableStateOf(range.max.toCalibrationDraftText()) }
    val commit = {
        onRangeChange(
            StickAxisRange(
                min = parseCalibrationFloat(minText, range.min),
                max = parseCalibrationFloat(maxText, range.max),
            ),
        )
    }
    StickMenuRangeField(
        value = minText,
        onValueChange = { minText = it },
        label = stringResource(R.string.rc_plot_settings_y_min),
        colors = colors,
        imeAction = ImeAction.Next,
        onDone = commit,
    )
    StickMenuRangeField(
        value = maxText,
        onValueChange = { maxText = it },
        label = stringResource(R.string.rc_plot_settings_y_max),
        colors = colors,
        imeAction = ImeAction.Done,
        onDone = commit,
    )
}

@Composable
private fun StickMenuRangeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    colors: TextFieldColors,
    imeAction: ImeAction,
    onDone: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, color = Neo.TextPrimary) },
        textStyle = MaterialTheme.typography.bodySmall.copy(color = Neo.TextPrimary),
        singleLine = true,
        colors = colors,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(
            onNext = { onDone() },
            onDone = {
                keyboardController?.hide()
                focusManager.clearFocus()
                onDone()
            },
        ),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .fillMaxWidth(),
    )
}

@Composable
private fun StickMenuChannelPicker(
    selected: TelemetryChannel?,
    occupiedChannels: Set<TelemetryChannel>,
    onChannelSelected: (TelemetryChannel?) -> Unit,
) {
    Text(
        text = stringResource(R.string.rc_vehicle_stick_channel_none_hint),
        style = MaterialTheme.typography.labelSmall,
        color = Neo.TextSecondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StickMenuChannelChip(
            label = stringResource(R.string.rc_vehicle_stick_channel_none),
            selected = selected == null,
            onClick = { onChannelSelected(null) },
            modifier = Modifier.weight(1f),
        )
    }
    TelemetryChannel.ANALOG_CHANNELS.chunked(4).forEach { row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            row.forEach { channel ->
                StickMenuChannelChip(
                    label = stringResource(channel.labelRes()),
                    selected = channel == selected,
                    available = channel == selected || channel !in occupiedChannels,
                    onClick = { onChannelSelected(channel) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StickMenuChannelChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    available: Boolean = true,
) {
    val shape = RoundedCornerShape(10.dp)
    val takenLabel = stringResource(R.string.rc_vehicle_stick_channel_taken)
    val borderColor = when {
        selected -> Neo.Accent
        available -> Neo.Accent.copy(alpha = 0.28f)
        else -> Neo.TextMuted.copy(alpha = 0.28f)
    }
    val textColor = when {
        selected -> Neo.Accent
        available -> Neo.TextSecondary
        else -> Neo.TextMuted
    }
    Box(
        modifier = modifier
            .clip(shape)
            .alpha(if (available || selected) 1f else 0.42f)
            .background(if (selected) Neo.Accent.copy(alpha = 0.22f) else Color.Transparent)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = shape,
            )
            .then(
                if (available) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier.semantics { disabled() }
                },
            )
            .semantics(mergeDescendants = true) {
                if (!available) {
                    contentDescription = "$label, $takenLabel"
                }
            }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor,
        )
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
