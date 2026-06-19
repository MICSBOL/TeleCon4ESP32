package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    negativeLabel: String,
    positiveLabel: String,
    stickPosition: Pair<Float, Float>,
    mode: JoystickMode,
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    accentEdge: RcGlassAccentEdge = RcGlassAccentEdge.START,
    joystickSize: Dp = RcVehicleProLayout.JoystickSize,
    onStopClick: (() -> Unit)? = null,
    onBuzzerClick: (() -> Unit)? = null,
) {
    RcGlassCard(
        modifier = modifier,
        surfaceAlpha = RcVehicleProGlass.JOYSTICK_ZONE_ALPHA,
        accentEdge = accentEdge,
        contentPadding = RcVehicleProLayout.ControlZoneContentPadding,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(
                    R.string.rc_vehicle_control_axis_hint,
                    negativeLabel,
                    positiveLabel,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = mutedTextColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(joystickSize),
        ) {
            RcTransparentJoystick(
                stickPosition = stickPosition,
                mode = mode,
                onMove = onMove,
                size = joystickSize,
                modifier = Modifier.align(Alignment.Center),
            )
            if (onBuzzerClick != null) {
                RcBuzzerButton(
                    onClick = onBuzzerClick,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp),
                )
            }
            if (onStopClick != null) {
                RcMotorStopButton(
                    onClick = onStopClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                )
            }
        }
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
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RcActionBar(
            isRecording = isRecording,
            lightsOn = lightsOn,
            onPhotoClick = onPhotoClick,
            onRecordClick = onRecordClick,
            onLightsClick = onLightsClick,
        )
    }
}

@Composable
fun RcCameraPanPanel(
    value: Float,
    onValueChange: (Float) -> Unit,
    onFrontClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RcGlassCard(
        modifier = modifier,
        surfaceAlpha = RcVehicleProGlass.SURFACE_ALPHA,
        accentEdge = RcGlassAccentEdge.END,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row (verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
//            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RcCameraKnobControl(
                value = value,
                onValueChange = onValueChange,
            )
            RcCameraFrontChip(onClick = onFrontClick)
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

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(chipBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                )
                .padding(5.dp),
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = label,
                tint = brandPrimary(),
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun RcCameraKnobControl(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            RcCameraRotationKnob(
                value = value,
                onValueChange = onValueChange,
                knobSize = RcVehicleProLayout.ControlZoneKnobSize,
            )
        }
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
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RcActionChip(
            label = stringResource(R.string.rc_vehicle_action_lights),
            icon = Icons.Default.Lightbulb,
            iconActive = lightsOn,
            onClick = onLightsClick,
        )
        RcActionChip(
            label = stringResource(R.string.rc_vehicle_action_photo),
            icon = Icons.Default.CameraAlt,
            iconActive = false,
            onClick = onPhotoClick,
        )
        RcActionChip(
            label = stringResource(R.string.rc_vehicle_action_record),
            icon = Icons.Default.FiberManualRecord,
            iconActive = isRecording,
            onClick = onRecordClick,
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
            .size(44.dp)
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
            modifier = Modifier.size(22.dp),
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
        )
    }
}
