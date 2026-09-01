package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.JoystickAxis
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.components.brandSecondary
import com.micsbol.telecon4esp32.ui.components.mutedTextColor
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProGlass
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProLayout
import com.micsbol.telecon4esp32.ui.theme.AccentRed
import com.micsbol.telecon4esp32.ui.theme.TechOnPrimary

@Composable
fun RcControlZone(
    title: String,
    stickPosition: Pair<Float, Float>,
    mode: JoystickMode,
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    accentEdge: RcGlassAccentEdge = RcGlassAccentEdge.START,
    joystickSize: Dp = RcVehicleProLayout.JoystickSize,
    showTitle: Boolean = true,
    onStopClick: (() -> Unit)? = null,
    onBuzzerClick: (() -> Unit)? = null,
    onStickModeChange: ((JoystickMode) -> Unit)? = null,
    settingsSyncGeneration: Int = 0,
    stickConfigContentDescription: String? = null,
    trimMode: Boolean = false,
    trimX: Int = 0,
    trimY: Int = 0,
    onTrimModeToggle: (() -> Unit)? = null,
    onTrimNudge: ((JoystickAxis, Int) -> Unit)? = null,
    onTrimConfirm: (() -> Unit)? = null,
    collapsibleToNearestEdge: Boolean = false,
    chromeExpanded: Boolean = true,
    onChromeExpandedChange: (Boolean) -> Unit = {},
) {
    val showTrim = onTrimModeToggle != null
    var menuExpanded by remember { mutableStateOf(false) }
    val axisHint = axisHintForMode(mode)
    val trimHint = trimHintForMode(mode, trimX, trimY)
    val trimAlignment = if (accentEdge == RcGlassAccentEdge.START) {
        Alignment.TopEnd
    } else {
        Alignment.TopStart
    }

    val zoneCard: @Composable (Modifier) -> Unit = { cardModifier ->
    RcGlassCard(
        modifier = cardModifier,
        surfaceAlpha = RcVehicleProGlass.JOYSTICK_ZONE_ALPHA,
        accentEdge = accentEdge,
        contentPadding = RcVehicleProLayout.ControlZoneContentPadding,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (showTitle) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = if (trimMode) trimHint else axisHint,
                style = MaterialTheme.typography.labelSmall,
                color = if (trimMode) brandPrimary() else mutedTextColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (showTitle) TextAlign.End else TextAlign.Start,
                modifier = Modifier.weight(1f, fill = !showTitle),
            )
            if (showTrim && !trimMode) {
                Spacer(modifier = Modifier.width(6.dp))
                RcStickTrimControls(
                    axis = mode.axis,
                    trimMode = false,
                    onToggleMode = onTrimModeToggle,
                    onNudge = onTrimNudge,
                    onConfirm = onTrimConfirm,
                )
            }
        }
        if (showTrim && trimMode) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = trimAlignment,
            ) {
                RcStickTrimControls(
                    axis = mode.axis,
                    trimMode = true,
                    onToggleMode = onTrimModeToggle,
                    onNudge = onTrimNudge,
                    onConfirm = onTrimConfirm,
                )
            }
        }
        Spacer(modifier = Modifier.height(1.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(joystickSize),
            contentAlignment = Alignment.Center,
        ) {
            RcTransparentJoystick(
                stickPosition = stickPosition,
                mode = mode,
                onMove = onMove,
                size = joystickSize,
                // Deadzone/expo/travel applied in RcStickMapping before TX.
                deadzone = 0f,
                settingsSyncGeneration = settingsSyncGeneration,
                onDoubleTap = onStickModeChange?.let { { menuExpanded = true } },
                contentDescription = stickConfigContentDescription,
            )
            if (onStickModeChange != null) {
                RcStickOptionsMenu(
                    expanded = menuExpanded,
                    selectedMode = mode,
                    onModeSelected = onStickModeChange,
                    onDismiss = { menuExpanded = false },
                )
            }
        }
        if (onStopClick != null || onBuzzerClick != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (onBuzzerClick != null) {
                    RcBuzzerButton(onClick = onBuzzerClick)
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                if (onStopClick != null) {
                    RcMotorStopButton(onClick = onStopClick)
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
            }
        }
    }
    }

    if (collapsibleToNearestEdge) {
        RcHudCollapsibleToEdge(
            towardEnd = accentEdge == RcGlassAccentEdge.END,
            showContentDescription = stringResource(R.string.rc_vehicle_hud_show_stick),
            hideContentDescription = stringResource(R.string.rc_vehicle_hud_hide_stick),
            expanded = chromeExpanded,
            onExpandedChange = onChromeExpandedChange,
            modifier = modifier,
            content = { zoneCard(Modifier.fillMaxWidth()) },
        )
    } else {
        zoneCard(modifier)
    }
}

@Composable
private fun axisHintForMode(mode: JoystickMode): String {
    return when (mode.axis) {
        JoystickAxis.VERTICAL -> stringResource(
            R.string.rc_vehicle_control_axis_hint,
            stringResource(R.string.rc_vehicle_control_reverse),
            stringResource(R.string.rc_vehicle_control_forward),
        )
        JoystickAxis.HORIZONTAL -> stringResource(
            R.string.rc_vehicle_control_axis_hint,
            stringResource(R.string.rc_vehicle_control_left),
            stringResource(R.string.rc_vehicle_control_right),
        )
        JoystickAxis.COMBINED -> stringResource(R.string.rc_vehicle_control_combined_hint)
    }
}

@Composable
private fun trimHintForMode(mode: JoystickMode, trimX: Int, trimY: Int): String {
    return when (mode.axis) {
        JoystickAxis.HORIZONTAL -> stringResource(R.string.rc_vehicle_trim_value_horizontal, trimX)
        JoystickAxis.VERTICAL -> stringResource(R.string.rc_vehicle_trim_value_vertical, trimY)
        JoystickAxis.COMBINED -> stringResource(R.string.rc_vehicle_trim_value_combined, trimX, trimY)
    }
}

@Composable
private fun RcStickTrimControls(
    axis: JoystickAxis,
    trimMode: Boolean,
    onToggleMode: (() -> Unit)?,
    onNudge: ((JoystickAxis, Int) -> Unit)?,
    onConfirm: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val toggle = onToggleMode ?: return
    if (!trimMode || onNudge == null || onConfirm == null) {
        RcRoundIconButton(
            onClick = toggle,
            label = stringResource(R.string.rc_vehicle_trim_enable),
            icon = Icons.Default.Tune,
            modifier = modifier,
        )
        return
    }
    when (axis) {
        JoystickAxis.HORIZONTAL -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                RcRoundIconButton(
                    onClick = { onNudge(JoystickAxis.HORIZONTAL, -1) },
                    label = stringResource(R.string.rc_vehicle_trim_left),
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                )
                RcRoundIconButton(
                    onClick = onConfirm,
                    label = stringResource(R.string.rc_vehicle_trim_confirm),
                    icon = Icons.Default.Check,
                )
                RcRoundIconButton(
                    onClick = { onNudge(JoystickAxis.HORIZONTAL, 1) },
                    label = stringResource(R.string.rc_vehicle_trim_right),
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                )
            }
        }
        JoystickAxis.VERTICAL -> {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                RcRoundIconButton(
                    onClick = { onNudge(JoystickAxis.VERTICAL, 1) },
                    label = stringResource(R.string.rc_vehicle_trim_up),
                    icon = Icons.Default.KeyboardArrowUp,
                )
                RcRoundIconButton(
                    onClick = onConfirm,
                    label = stringResource(R.string.rc_vehicle_trim_confirm),
                    icon = Icons.Default.Check,
                )
                RcRoundIconButton(
                    onClick = { onNudge(JoystickAxis.VERTICAL, -1) },
                    label = stringResource(R.string.rc_vehicle_trim_down),
                    icon = Icons.Default.KeyboardArrowDown,
                )
            }
        }
        JoystickAxis.COMBINED -> {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                RcRoundIconButton(
                    onClick = { onNudge(JoystickAxis.VERTICAL, 1) },
                    label = stringResource(R.string.rc_vehicle_trim_up),
                    icon = Icons.Default.KeyboardArrowUp,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    RcRoundIconButton(
                        onClick = { onNudge(JoystickAxis.HORIZONTAL, -1) },
                        label = stringResource(R.string.rc_vehicle_trim_left),
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    )
                    RcRoundIconButton(
                        onClick = onConfirm,
                        label = stringResource(R.string.rc_vehicle_trim_confirm),
                        icon = Icons.Default.Check,
                    )
                    RcRoundIconButton(
                        onClick = { onNudge(JoystickAxis.HORIZONTAL, 1) },
                        label = stringResource(R.string.rc_vehicle_trim_right),
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    )
                }
                RcRoundIconButton(
                    onClick = { onNudge(JoystickAxis.VERTICAL, -1) },
                    label = stringResource(R.string.rc_vehicle_trim_down),
                    icon = Icons.Default.KeyboardArrowDown,
                )
            }
        }
    }
}

@Composable
private fun RcRoundIconButton(
    onClick: () -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    val background = MaterialTheme.colorScheme.surface.copy(alpha = RcVehicleProGlass.ACTION_CHIP_ALPHA)
    val ringColor = brandPrimary().copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .size(36.dp)
            .semantics { contentDescription = label }
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(background)
            .drawBehind {
                drawCircle(
                    color = ringColor,
                    radius = size.minDimension / 2f,
                    style = Stroke(width = 1.5f.dp.toPx()),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = brandPrimary(),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun RcCenterControls(
    isRecording: Boolean,
    lightsOn: Boolean,
    onPhotoClick: () -> Unit,
    onRecordClick: () -> Unit,
    onLightsClick: () -> Unit,
    modifier: Modifier = Modifier,
    throttleTravelPercent: Int = 100,
    steerTravelPercent: Int = 100,
    onCycleThrottleTravel: (() -> Unit)? = null,
    onCycleSteerTravel: (() -> Unit)? = null,
    onOpenDriveAssist: (() -> Unit)? = null,
    expanded: Boolean = true,
    onExpandedChange: (Boolean) -> Unit = {},
) {
    val chromeEnter = expandVertically(
        expandFrom = Alignment.Bottom,
        animationSpec = tween(RcVehicleProLayout.HUD_CHROME_ANIM_MS),
    ) + fadeIn(animationSpec = tween(RcVehicleProLayout.HUD_CHROME_FADE_MS))
    val chromeExit = shrinkVertically(
        shrinkTowards = Alignment.Bottom,
        animationSpec = tween(RcVehicleProLayout.HUD_CHROME_ANIM_MS),
    ) + fadeOut(animationSpec = tween(RcVehicleProLayout.HUD_CHROME_FADE_MS))

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = chromeEnter,
            exit = chromeExit,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RcHudBareChevronHandle(
                    direction = RcHudChevronDirection.Down,
                    contentDescription = stringResource(R.string.rc_vehicle_hud_hide_center_controls),
                    modifier = Modifier.hudVerticalSwipe(
                        expanded = true,
                        onExpandedChange = onExpandedChange,
                        hideTowardBottom = true,
                    ),
                )
                RcCenterControlsContent(
                    isRecording = isRecording,
                    lightsOn = lightsOn,
                    onPhotoClick = onPhotoClick,
                    onRecordClick = onRecordClick,
                    onLightsClick = onLightsClick,
                    throttleTravelPercent = throttleTravelPercent,
                    steerTravelPercent = steerTravelPercent,
                    onCycleThrottleTravel = onCycleThrottleTravel,
                    onCycleSteerTravel = onCycleSteerTravel,
                    onOpenDriveAssist = onOpenDriveAssist,
                )
            }
        }
        AnimatedVisibility(
            visible = !expanded,
            enter = chromeEnter,
            exit = chromeExit,
        ) {
            RcHudBareChevronHandle(
                direction = RcHudChevronDirection.Up,
                contentDescription = stringResource(R.string.rc_vehicle_hud_show_center_controls),
                onClick = { onExpandedChange(true) },
                modifier = Modifier.hudVerticalSwipe(
                    expanded = false,
                    onExpandedChange = onExpandedChange,
                    hideTowardBottom = true,
                ),
            )
        }
    }
}

@Composable
private fun RcCenterControlsContent(
    isRecording: Boolean,
    lightsOn: Boolean,
    onPhotoClick: () -> Unit,
    onRecordClick: () -> Unit,
    onLightsClick: () -> Unit,
    throttleTravelPercent: Int,
    steerTravelPercent: Int,
    onCycleThrottleTravel: (() -> Unit)?,
    onCycleSteerTravel: (() -> Unit)?,
    onOpenDriveAssist: (() -> Unit)?,
) {
    if (onCycleThrottleTravel != null || onCycleSteerTravel != null || onOpenDriveAssist != null) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onCycleThrottleTravel != null) {
                RcTravelChip(
                    label = stringResource(
                        R.string.rc_vehicle_travel_throttle_chip,
                        throttleTravelPercent,
                    ),
                    onClick = onCycleThrottleTravel,
                )
            }
            if (onCycleSteerTravel != null) {
                RcTravelChip(
                    label = stringResource(
                        R.string.rc_vehicle_travel_steer_chip,
                        steerTravelPercent,
                    ),
                    onClick = onCycleSteerTravel,
                )
            }
            if (onOpenDriveAssist != null) {
                RcRoundIconButton(
                    onClick = onOpenDriveAssist,
                    label = stringResource(R.string.rc_vehicle_drive_assist_title),
                    icon = Icons.Default.Tune,
                )
            }
        }
    }
    RcActionBar(
        isRecording = isRecording,
        lightsOn = lightsOn,
        onPhotoClick = onPhotoClick,
        onRecordClick = onRecordClick,
        onLightsClick = onLightsClick,
    )
}

@Composable
private fun RcTravelChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = MaterialTheme.colorScheme.surface.copy(alpha = RcVehicleProGlass.ACTION_CHIP_ALPHA)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(background)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = brandPrimary(),
            maxLines = 1,
        )
    }
}

@Composable
fun RcCameraPanPanel(
    value: Float,
    onValueChange: (Float) -> Unit,
    onFrontClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    onExpandedChange: (Boolean) -> Unit = {},
) {
    RcHudCollapsibleToEdge(
        towardEnd = true,
        showContentDescription = stringResource(R.string.rc_vehicle_hud_show_camera_pan),
        hideContentDescription = stringResource(R.string.rc_vehicle_hud_hide_camera_pan),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        fillWidth = false,
    ) {
        RcGlassCard(
            surfaceAlpha = RcVehicleProGlass.SURFACE_ALPHA,
            accentEdge = RcGlassAccentEdge.END,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
            fillWidth = false,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RcCameraKnobControl(
                    value = value,
                    onValueChange = onValueChange,
                )
                RcCameraFrontChip(onClick = onFrontClick)
            }
        }
    }
}

@Composable
private fun RcCameraFrontChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.rc_vehicle_action_camera_front)
    val chipBackground = MaterialTheme.colorScheme.surface.copy(alpha = RcVehicleProGlass.ACTION_CHIP_ALPHA)

    Box(
        modifier = modifier
            .semantics { contentDescription = label }
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(chipBackground)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.CenterFocusStrong,
            contentDescription = null,
            tint = brandPrimary(),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun RcCameraKnobControl(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val panRaw = RcVehicleProLayout.cameraPanRaw(value)
    val panDescription = stringResource(R.string.rc_vehicle_camera_pan_value, panRaw)
    Box(
        modifier = modifier.semantics { contentDescription = panDescription },
        contentAlignment = Alignment.Center,
    ) {
        RcCameraRotationKnob(
            value = value,
            onValueChange = onValueChange,
            knobSize = RcVehicleProLayout.ControlZoneKnobSize,
        )
    }
}

@Composable
fun RcActionBar(
    isRecording: Boolean,
    lightsOn: Boolean,
    onPhotoClick: () -> Unit,
    onRecordClick: () -> Unit,
    onLightsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RcActionChip(
            label = stringResource(R.string.rc_vehicle_action_lights),
            icon = Icons.Default.Lightbulb,
            iconActive = lightsOn,
            onClick = onLightsClick,
            modifier = Modifier.weight(1f),
        )
        RcActionChip(
            label = stringResource(R.string.rc_vehicle_action_photo),
            icon = Icons.Default.CameraAlt,
            iconActive = false,
            onClick = onPhotoClick,
            modifier = Modifier.weight(1f),
        )
        RcActionChip(
            label = stringResource(R.string.rc_vehicle_action_record),
            icon = Icons.Default.FiberManualRecord,
            iconActive = isRecording,
            onClick = onRecordClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RcBuzzerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.rc_vehicle_action_buzzer)
    val background = MaterialTheme.colorScheme.surface.copy(alpha = RcVehicleProGlass.ACTION_CHIP_ALPHA)
    val ringColor = brandPrimary().copy(alpha = 0.35f)
    val iconColor = brandPrimary()

    Box(
        modifier = modifier
            .size(40.dp)
            .semantics { contentDescription = label }
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(background)
            .drawBehind {
                drawCircle(
                    color = ringColor,
                    radius = size.minDimension / 2f,
                    style = Stroke(width = 1.5f.dp.toPx()),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun RcMotorStopButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.rc_vehicle_action_stop)
    val outerRed = AccentRed.copy(alpha = RcVehicleProGlass.STOP_BUTTON_ALPHA)
    val innerRed = Color(0xFF9B1C1C)

    Box(
        modifier = modifier
            .size(36.dp)
            .semantics { contentDescription = label }
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        outerRed,
                        innerRed.copy(alpha = 0.92f),
                    ),
                ),
            )
            .drawBehind {
                drawCircle(
                    color = TechOnPrimary.copy(alpha = 0.22f),
                    radius = size.minDimension * 0.22f,
                    center = Offset(size.width * 0.35f, size.height * 0.32f),
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.28f),
                    radius = size.minDimension / 2f,
                    style = Stroke(width = 2f.dp.toPx()),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.PowerSettingsNew,
            contentDescription = null,
            tint = TechOnPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun RcStopButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (compact) {
        RcMotorStopButton(onClick = onClick, modifier = modifier)
        return
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(if (compact) 20.dp else 28.dp))
            .clickable(onClick = onClick)
            .background(AccentRed.copy(alpha = RcVehicleProGlass.STOP_BUTTON_ALPHA))
            .padding(
                horizontal = if (compact) 10.dp else 22.dp,
                vertical = if (compact) 7.dp else 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Stop,
            contentDescription = stringResource(R.string.rc_vehicle_action_stop),
            tint = TechOnPrimary,
            modifier = Modifier.size(if (compact) 16.dp else 20.dp),
        )
        Text(
            text = stringResource(R.string.rc_vehicle_action_stop),
            style = if (compact) {
                MaterialTheme.typography.labelMedium
            } else {
                MaterialTheme.typography.labelLarge
            },
            fontWeight = FontWeight.Bold,
            color = TechOnPrimary,
        )
    }
}

@Composable
private fun RcActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chipBackground = MaterialTheme.colorScheme.surface.copy(alpha = RcVehicleProGlass.ACTION_CHIP_ALPHA)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(chipBackground)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (iconActive) {
                        Brush.linearGradient(listOf(brandPrimary(), brandSecondary()))
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                            ),
                        )
                    },
                )
                .padding(7.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (iconActive) TechOnPrimary else brandPrimary(),
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}
