package com.micsbol.telecon4esp32.ui.control_panel.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.theme.StatusConnected
import com.micsbol.telecon4esp32.ui.theme.StatusDisconnected

private val ControlPanelOverlayIconSize = 20.dp
private val ControlPanelOverlayIconSpacing = 2.dp

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
) {
    val iconTint = when {
        isConnecting -> brandPrimary()
        isConnected -> StatusConnected
        else -> StatusDisconnected
    }
    val accessibilityDescription = when {
        isConnecting -> stringResource(R.string.live_control_bluetooth_connecting_content_description)
        isConnected -> stringResource(R.string.live_control_bluetooth_connected_content_description)
        else -> stringResource(R.string.live_control_bluetooth_disconnected_content_description)
    }
    val isTappable = !isConnected && !isConnecting

    ControlPanelPlasticIconButton(
        onClick = onDisconnectedClick,
        contentDescription = accessibilityDescription,
        icon = Icons.Default.Bluetooth,
        modifier = modifier,
        size = size,
        iconTint = iconTint,
        enabled = isTappable,
    )
}

@Composable
fun ControlPanelOverlayControls(
    isBluetoothConnected: Boolean,
    isBluetoothConnecting: Boolean,
    onBackToModulesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBluetoothDisconnectedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ControlPanelOverlayIconSpacing),
    ) {
        ControlPanelPlasticIconButton(
            onClick = onBackToModulesClick,
            contentDescription = stringResource(R.string.control_panel_back_to_modules_content_description),
            icon = Icons.Default.Apps,
            size = ControlPanelOverlayIconSize,
        )
        ControlPanelPlasticIconButton(
            onClick = onSettingsClick,
            contentDescription = stringResource(
                R.string.applications_settings_content_description,
                stringResource(R.string.app_control_panel_settings_title),
            ),
            icon = Icons.Default.Settings,
            size = ControlPanelOverlayIconSize,
        )
        ControlPanelBluetoothStatusButton(
            isConnected = isBluetoothConnected,
            isConnecting = isBluetoothConnecting,
            onDisconnectedClick = onBluetoothDisconnectedClick,
            size = ControlPanelOverlayIconSize,
        )
    }
}
