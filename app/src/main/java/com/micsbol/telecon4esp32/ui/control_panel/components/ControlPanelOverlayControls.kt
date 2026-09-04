package com.micsbol.telecon4esp32.ui.control_panel.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ControlPanelCenterMode
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.control_panel.icon
import com.micsbol.telecon4esp32.ui.control_panel.titleRes
import com.micsbol.telecon4esp32.ui.theme.Neo
import com.micsbol.telecon4esp32.ui.theme.StatusConnected
import com.micsbol.telecon4esp32.ui.theme.StatusDisconnected

private val ControlPanelOverlayIconSize = 34.dp
private val ControlPanelOverlayIconSpacing = 6.dp
private val ProGold = Color(0xFFFFD54F)
private val ProGoldDeep = Color(0xFFFFB300)

@Composable
fun ControlPanelPlasticIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconTint: Color = Color(0xFFD8D8D8),
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(size * 0.26f)
    val cornerPx = size * 0.26f

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .drawBehind {
                drawPlasticRaisedRoundRect(
                    topLeft = Offset.Zero,
                    size = Size(size.toPx(), size.toPx()),
                    cornerRadius = CornerRadius(cornerPx.toPx()),
                )
            }
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

@Composable
fun ControlPanelBluetoothStatusButton(
    isConnected: Boolean,
    isConnecting: Boolean,
    onDisconnectedClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    usesWifiLink: Boolean = false,
) {
    val iconTint = when {
        isConnecting -> brandPrimary()
        isConnected -> StatusConnected
        else -> StatusDisconnected
    }
    val accessibilityDescription = when {
        usesWifiLink && isConnecting ->
            stringResource(R.string.live_control_wifi_connecting_content_description)
        usesWifiLink && isConnected ->
            stringResource(R.string.live_control_wifi_connected_content_description)
        usesWifiLink ->
            stringResource(R.string.live_control_wifi_disconnected_content_description)
        isConnecting -> stringResource(R.string.live_control_bluetooth_connecting_content_description)
        isConnected -> stringResource(R.string.live_control_bluetooth_connected_content_description)
        else -> stringResource(R.string.live_control_bluetooth_disconnected_content_description)
    }
    val isTappable = !isConnected && !isConnecting

    ControlPanelPlasticIconButton(
        onClick = onDisconnectedClick,
        contentDescription = accessibilityDescription,
        icon = if (usesWifiLink) Icons.Default.Wifi else Icons.Default.Bluetooth,
        modifier = modifier,
        size = size,
        iconTint = iconTint,
        enabled = isTappable,
    )
}

@Composable
fun ControlPanelCenterModeButton(
    mode: ControlPanelCenterMode,
    selected: Boolean,
    unlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = ControlPanelOverlayIconSize,
) {
    val shape = RoundedCornerShape(size * 0.26f)
    val cornerPx = size * 0.26f
    val modeName = stringResource(mode.titleRes)
    val accessibilityDescription = when {
        unlocked && selected -> stringResource(
            R.string.control_panel_center_mode_button_content_description,
            modeName,
        )
        unlocked -> stringResource(
            R.string.control_panel_center_mode_select_content_description,
            modeName,
        )
        mode == ControlPanelCenterMode.CAMERA ->
            stringResource(R.string.control_panel_center_camera_locked_content_description)
        mode == ControlPanelCenterMode.RADAR ->
            stringResource(R.string.control_panel_center_radar_locked_content_description)
        else -> stringResource(R.string.control_panel_center_extras_locked_content_description)
    }

    val selectedBorderAlpha = if (unlocked && selected) {
        val pulse = rememberInfiniteTransition(label = "centerModeBorder")
        val alpha by pulse.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "centerModeBorderAlpha",
        )
        alpha
    } else {
        1f
    }

    Box(
        modifier = modifier
            .size(size)
            .alpha(if (unlocked) 1f else 0.55f)
            .then(
                when {
                    unlocked && selected -> Modifier.border(
                        width = 1.dp,
                        color = Neo.Positive.copy(alpha = selectedBorderAlpha),
                        shape = shape,
                    )
                    !unlocked -> Modifier.border(
                        width = 1.5.dp,
                        color = ProGold.copy(alpha = 0.7f),
                        shape = shape,
                    )
                    else -> Modifier
                },
            )
            .clip(shape)
            .drawBehind {
                drawPlasticRaisedRoundRect(
                    topLeft = Offset.Zero,
                    size = Size(size.toPx(), size.toPx()),
                    cornerRadius = CornerRadius(cornerPx.toPx()),
                )
                if (!unlocked) {
                    drawRoundRect(
                        color = ProGoldDeep.copy(alpha = 0.18f),
                        topLeft = Offset.Zero,
                        size = Size(size.toPx(), size.toPx()),
                        cornerRadius = CornerRadius(cornerPx.toPx()),
                    )
                }
            }
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                this.contentDescription = accessibilityDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = mode.icon,
            contentDescription = null,
            tint = if (unlocked) brandPrimary() else ProGold,
            modifier = Modifier.size(size * 0.5f),
        )
        if (!unlocked) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = ProGold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(size * 0.28f),
            )
        }
    }
}

@Composable
fun ControlPanelOverlayControls(
    isBluetoothConnected: Boolean,
    isBluetoothConnecting: Boolean,
    onBackToModulesClick: () -> Unit,
    onConnectionSettingsClick: () -> Unit,
    onBluetoothDisconnectedClick: () -> Unit,
    centerMode: ControlPanelCenterMode,
    isModeUnlocked: (ControlPanelCenterMode) -> Boolean,
    onCenterModeClick: (ControlPanelCenterMode) -> Unit,
    modifier: Modifier = Modifier,
    usesWifiLink: Boolean = false,
    isSessionRecording: Boolean = false,
    isSessionRecordingUnlocked: Boolean = true,
    onToggleSessionRecording: () -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ControlPanelOverlayIconSpacing),
            ) {
                ControlPanelPlasticIconButton(
                    onClick = onBackToModulesClick,
                    contentDescription = stringResource(R.string.control_panel_back_to_modules_content_description),
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    size = ControlPanelOverlayIconSize,
                )
                ControlPanelPlasticIconButton(
                    onClick = onConnectionSettingsClick,
                    contentDescription = stringResource(
                        R.string.applications_settings_content_description,
                        stringResource(R.string.control_panel_connection_settings_title),
                    ),
                    icon = Icons.Filled.Cable,
                    size = ControlPanelOverlayIconSize,
                )
                ControlPanelBluetoothStatusButton(
                    isConnected = isBluetoothConnected,
                    isConnecting = isBluetoothConnecting,
                    onDisconnectedClick = onBluetoothDisconnectedClick,
                    size = ControlPanelOverlayIconSize,
                    usesWifiLink = usesWifiLink,
                )
                ControlPanelRecordSessionButton(
                    isRecording = isSessionRecording,
                    unlocked = isSessionRecordingUnlocked,
                    onClick = onToggleSessionRecording,
                )
            }
            Spacer(modifier = Modifier.height(ControlPanelOverlayIconSpacing))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ControlPanelOverlayIconSpacing),
            ) {
                ControlPanelCenterMode.entries.forEach { mode ->
                    ControlPanelCenterModeButton(
                        mode = mode,
                        selected = centerMode == mode,
                        unlocked = isModeUnlocked(mode),
                        onClick = { onCenterModeClick(mode) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlPanelRecordSessionButton(
    isRecording: Boolean,
    unlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = ControlPanelOverlayIconSize,
) {
    val showLock = !unlocked && !isRecording
    val shape = RoundedCornerShape(size * 0.26f)
    val contentDescription = when {
        isRecording -> stringResource(R.string.control_panel_stop_recording_content_description)
        showLock -> stringResource(R.string.control_panel_record_session_locked_content_description)
        else -> stringResource(R.string.control_panel_record_session_content_description)
    }
    Box(
        modifier = modifier
            .size(size)
            .alpha(if (showLock) 0.55f else 1f)
            .then(
                if (showLock) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = ProGold.copy(alpha = 0.7f),
                        shape = shape,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        ControlPanelPlasticIconButton(
            onClick = onClick,
            contentDescription = contentDescription,
            icon = Icons.Filled.FiberManualRecord,
            size = size,
            iconTint = when {
                isRecording -> Neo.Negative
                showLock -> ProGold
                else -> Color(0xFFD8D8D8)
            },
        )
        if (showLock) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = ProGold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(size * 0.28f),
            )
        }
    }
}
