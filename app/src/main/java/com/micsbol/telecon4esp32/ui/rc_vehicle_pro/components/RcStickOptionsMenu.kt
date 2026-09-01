package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.JoystickAxis
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.components.mutedTextColor
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProGlass

@Composable
fun RcStickOptionsMenu(
    expanded: Boolean,
    selectedMode: JoystickMode,
    onModeSelected: (JoystickMode) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(expanded) { mutableStateOf(selectedMode) }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(228.dp),
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
                    .heightIn(max = 280.dp)
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
private fun RcStickMenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
        color = brandPrimary().copy(alpha = 0.28f),
    )
}

@Composable
private fun RcStickOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
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
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
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
