package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
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
import com.micsbol.telecon4esp32.domain.model.parseCalibrationFloat
import com.micsbol.telecon4esp32.domain.model.toCalibrationDraftText
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.components.mutedTextColor
import com.micsbol.telecon4esp32.ui.rc_settings.labelRes
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProGlass

@Composable
fun RcStickOptionsMenu(
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
    val showVerticalChannels = draft.axis != JoystickAxis.HORIZONTAL
    val showHorizontalChannels = draft.axis != JoystickAxis.VERTICAL
    val fieldColors = rcStickRangeFieldColors()
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(240.dp),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        RcGlassCard(
            modifier = Modifier.fillMaxWidth(),
            surfaceAlpha = RcVehicleProGlass.MENU_SURFACE_ALPHA,
            accentEdge = RcGlassAccentEdge.START,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.rc_vehicle_stick_options_title),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                RcStickOptionRow(
                    label = stringResource(R.string.rc_joystick_mode_spring),
                    selected = draft.isSpring,
                    onClick = {
                        val next = draft.withSpring(true)
                        draft = next
                        onModeSelected(next)
                    },
                )
                RcStickOptionRow(
                    label = stringResource(R.string.rc_joystick_mode_hold),
                    selected = !draft.isSpring,
                    onClick = {
                        val next = draft.withSpring(false)
                        draft = next
                        onModeSelected(next)
                    },
                )
                RcStickMenuDivider()
                RcStickOptionRow(
                    label = stringResource(R.string.rc_joystick_axis_vertical),
                    selected = draft.axis != JoystickAxis.HORIZONTAL,
                    onClick = {
                        val next = draft.togglingAxis(JoystickAxis.VERTICAL)
                        draft = next
                        onModeSelected(next)
                    },
                )
                RcStickOptionRow(
                    label = stringResource(R.string.rc_joystick_axis_horizontal),
                    selected = draft.axis != JoystickAxis.VERTICAL,
                    onClick = {
                        val next = draft.togglingAxis(JoystickAxis.HORIZONTAL)
                        draft = next
                        onModeSelected(next)
                    },
                )
                RcStickMenuDivider()
                RcStickOptionRow(
                    label = stringResource(R.string.rc_joystick_range_circle),
                    selected = rangeDraft == JoystickRangeShape.CIRCLE,
                    onClick = {
                        rangeDraft = JoystickRangeShape.CIRCLE
                        onRangeShapeSelected(JoystickRangeShape.CIRCLE)
                    },
                )
                RcStickOptionRow(
                    label = stringResource(R.string.rc_joystick_range_square),
                    selected = rangeDraft == JoystickRangeShape.SQUARE,
                    onClick = {
                        rangeDraft = JoystickRangeShape.SQUARE
                        onRangeShapeSelected(JoystickRangeShape.SQUARE)
                    },
                )
                RcStickMenuDivider()
                if (showVerticalChannels) {
                    RcStickAxisRangeFields(
                        axisLabel = stringResource(R.string.rc_joystick_axis_vertical),
                        range = channelDraft.verticalRange,
                        colors = fieldColors,
                        onRangeChange = { range ->
                            val next = channelDraft.withRange(JoystickAxis.VERTICAL, range)
                            channelDraft = next
                            onChannelLinkChange(next)
                        },
                    )
                }
                if (showHorizontalChannels) {
                    RcStickAxisRangeFields(
                        axisLabel = stringResource(R.string.rc_joystick_axis_horizontal),
                        range = channelDraft.horizontalRange,
                        colors = fieldColors,
                        onRangeChange = { range ->
                            val next = channelDraft.withRange(JoystickAxis.HORIZONTAL, range)
                            channelDraft = next
                            onChannelLinkChange(next)
                        },
                    )
                }
                RcStickMenuDivider()
                RcStickOptionRow(
                    label = stringResource(R.string.rc_vehicle_stick_use_channel),
                    selected = channelDraft.enabled,
                    showCircle = true,
                    onClick = {
                        val next = channelDraft.withEnabled(!channelDraft.enabled, draft.axis, occupiedChannels)
                        channelDraft = next
                        onChannelLinkChange(next)
                    },
                )
                if (channelDraft.enabled) {
                    if (showVerticalChannels) {
                        RcStickChannelAxisPicker(
                            label = stringResource(R.string.rc_joystick_axis_vertical),
                            selected = channelDraft.vertical,
                            occupiedChannels = occupiedChannels +
                                channelDraft.assignedChannels(JoystickAxis.VERTICAL),
                            onChannelClick = { channel ->
                                val occupied = occupiedChannels +
                                    channelDraft.assignedChannels(JoystickAxis.VERTICAL)
                                val next = channelDraft.toggling(
                                    JoystickAxis.VERTICAL,
                                    channel,
                                    occupied,
                                )
                                channelDraft = next
                                onChannelLinkChange(next)
                            },
                            onNoneClick = {
                                val next = channelDraft.selecting(JoystickAxis.VERTICAL, null)
                                channelDraft = next
                                onChannelLinkChange(next)
                            },
                        )
                    }
                    if (showHorizontalChannels) {
                        RcStickChannelAxisPicker(
                            label = stringResource(R.string.rc_joystick_axis_horizontal),
                            selected = channelDraft.horizontal,
                            occupiedChannels = occupiedChannels +
                                channelDraft.assignedChannels(JoystickAxis.HORIZONTAL),
                            onChannelClick = { channel ->
                                val occupied = occupiedChannels +
                                    channelDraft.assignedChannels(JoystickAxis.HORIZONTAL)
                                val next = channelDraft.toggling(
                                    JoystickAxis.HORIZONTAL,
                                    channel,
                                    occupied,
                                )
                                channelDraft = next
                                onChannelLinkChange(next)
                            },
                            onNoneClick = {
                                val next = channelDraft.selecting(JoystickAxis.HORIZONTAL, null)
                                channelDraft = next
                                onChannelLinkChange(next)
                            },
                        )
                    }
                }
                RcStickMenuDivider()
                draft.allowedRestPositions().forEach { position ->
                    RcStickOptionRow(
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
fun RcKnobOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    channelLink: KnobChannelLink = KnobChannelLink.DEFAULT,
    onChannelLinkChange: (KnobChannelLink) -> Unit = {},
    occupiedChannels: Set<TelemetryChannel> = emptySet(),
) {
    var channelDraft by remember(expanded) { mutableStateOf(channelLink) }
    val fieldColors = rcStickRangeFieldColors()
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(240.dp),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        RcGlassCard(
            modifier = Modifier.fillMaxWidth(),
            surfaceAlpha = RcVehicleProGlass.MENU_SURFACE_ALPHA,
            accentEdge = RcGlassAccentEdge.START,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.rc_vehicle_knob_options_title),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                RcStickAxisRangeFields(
                    axisLabel = stringResource(R.string.rc_vehicle_knob_options_title),
                    range = channelDraft.range,
                    colors = fieldColors,
                    onRangeChange = { range ->
                        val next = channelDraft.withRange(range)
                        channelDraft = next
                        onChannelLinkChange(next)
                    },
                )
                RcStickMenuDivider()
                RcStickOptionRow(
                    label = stringResource(R.string.rc_vehicle_stick_use_channel),
                    selected = channelDraft.enabled,
                    showCircle = true,
                    onClick = {
                        val next = channelDraft.withEnabled(!channelDraft.enabled, occupiedChannels)
                        channelDraft = next
                        onChannelLinkChange(next)
                    },
                )
                if (channelDraft.enabled) {
                    RcStickChannelAxisPicker(
                        label = stringResource(R.string.rc_vehicle_plot_settings_source),
                        selected = channelDraft.channel,
                        occupiedChannels = occupiedChannels,
                        onChannelClick = { channel ->
                            val next = channelDraft.toggling(channel, occupiedChannels)
                            channelDraft = next
                            onChannelLinkChange(next)
                        },
                        onNoneClick = {
                            val next = channelDraft.selecting(null)
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
private fun RcStickAxisRangeFields(
    axisLabel: String,
    range: StickAxisRange,
    colors: TextFieldColors,
    onRangeChange: (StickAxisRange) -> Unit,
) {
    Text(
        text = axisLabel,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = mutedTextColor(),
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
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
    RcStickRangeField(
        value = minText,
        onValueChange = { minText = it },
        label = stringResource(R.string.rc_plot_settings_y_min),
        colors = colors,
        imeAction = ImeAction.Next,
        onDone = commit,
    )
    RcStickRangeField(
        value = maxText,
        onValueChange = { maxText = it },
        label = stringResource(R.string.rc_plot_settings_y_max),
        colors = colors,
        imeAction = ImeAction.Done,
        onDone = commit,
    )
}

@Composable
private fun RcStickRangeField(
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
        label = { Text(text = label) },
        textStyle = MaterialTheme.typography.bodySmall,
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
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun rcStickRangeFieldColors(): TextFieldColors {
    val accent = brandPrimary()
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedBorderColor = accent,
        unfocusedBorderColor = accent.copy(alpha = 0.35f),
        focusedLabelColor = accent,
        unfocusedLabelColor = mutedTextColor(),
        cursorColor = accent,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
    )
}

@Composable
private fun RcStickMenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
        color = brandPrimary().copy(alpha = 0.28f),
    )
}

@Composable
private fun RcStickChannelAxisPicker(
    label: String,
    selected: TelemetryChannel?,
    occupiedChannels: Set<TelemetryChannel>,
    onChannelClick: (TelemetryChannel) -> Unit,
    onNoneClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = mutedTextColor(),
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RcStickChannelChip(
            label = stringResource(R.string.rc_vehicle_stick_channel_none),
            selected = selected == null,
            onClick = onNoneClick,
            modifier = Modifier.weight(1f),
        )
    }
    TelemetryChannel.ANALOG_CHANNELS.chunked(4).forEach { row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            row.forEach { channel ->
                RcStickChannelChip(
                    label = stringResource(channel.labelRes()),
                    selected = channel == selected,
                    available = channel == selected || channel !in occupiedChannels,
                    onClick = { onChannelClick(channel) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RcStickChannelChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    available: Boolean = true,
) {
    val accent = brandPrimary()
    val shape = RoundedCornerShape(10.dp)
    val takenLabel = stringResource(R.string.rc_vehicle_stick_channel_taken)
    val background = if (selected) {
        MaterialTheme.colorScheme.surface.copy(alpha = RcVehicleProGlass.ACTION_CHIP_ALPHA)
    } else {
        Color.Transparent
    }
    val borderColor = when {
        selected -> accent.copy(alpha = 0.7f)
        available -> accent.copy(alpha = 0.28f)
        else -> mutedTextColor().copy(alpha = 0.35f)
    }
    val textColor = when {
        selected -> accent
        available -> mutedTextColor()
        else -> mutedTextColor().copy(alpha = 0.7f)
    }
    Box(
        modifier = modifier
            .clip(shape)
            .alpha(if (available || selected) 1f else 0.42f)
            .background(background)
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

@Composable
private fun RcStickOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    showCircle: Boolean = false,
) {
    val accent = brandPrimary()
    val shape = RoundedCornerShape(14.dp)
    val background = if (selected) {
        MaterialTheme.colorScheme.surface.copy(alpha = RcVehicleProGlass.ACTION_CHIP_ALPHA)
    } else {
        Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .drawBehind {
                if (selected) {
                    drawRoundRect(
                        color = accent.copy(alpha = 0.45f),
                        cornerRadius = CornerRadius(14.dp.toPx()),
                        style = Stroke(width = 1.5f.dp.toPx()),
                    )
                }
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) accent else mutedTextColor(),
            modifier = Modifier.weight(1f),
        )
        if (showCircle) {
            RcStickUseChannelCircle(selected = selected, accent = accent)
        } else if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun RcStickUseChannelCircle(
    selected: Boolean,
    accent: Color,
) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .border(1.5.dp, accent.copy(alpha = if (selected) 1f else 0.55f), CircleShape)
            .then(
                if (selected) {
                    Modifier.background(accent.copy(alpha = 0.22f), CircleShape)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(12.dp),
            )
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
