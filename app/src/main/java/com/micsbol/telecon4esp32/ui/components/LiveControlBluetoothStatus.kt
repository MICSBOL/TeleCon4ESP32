package com.micsbol.telecon4esp32.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.theme.StatusConnected
import com.micsbol.telecon4esp32.ui.theme.StatusDisconnected

@Composable
fun LiveControlBluetoothStatusChip(
    isConnected: Boolean,
    isConnecting: Boolean = false,
    onDisconnectedClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when {
        isConnecting -> brandPrimary().copy(alpha = 0.18f)
        isConnected -> StatusConnected.copy(alpha = 0.18f)
        else -> StatusDisconnected.copy(alpha = 0.18f)
    }
    val contentColor = when {
        isConnecting -> brandPrimary()
        isConnected -> StatusConnected
        else -> StatusDisconnected
    }
    val statusText = when {
        isConnecting -> stringResource(R.string.home_bluetooth_status_connecting)
        isConnected -> stringResource(R.string.home_bluetooth_status_connected)
        else -> stringResource(R.string.home_bluetooth_status_disconnected)
    }
    val accessibilityDescription = when {
        isConnecting -> stringResource(R.string.live_control_bluetooth_connecting_content_description)
        isConnected -> stringResource(R.string.live_control_bluetooth_connected_content_description)
        else -> stringResource(R.string.live_control_bluetooth_disconnected_content_description)
    }
    val isTappable = !isConnected && !isConnecting && onDisconnectedClick != null

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .then(
                if (isTappable) {
                    Modifier
                        .clickable(onClick = onDisconnectedClick)
                        .semantics {
                            role = Role.Button
                            contentDescription = accessibilityDescription
                        }
                } else {
                    Modifier.semantics { contentDescription = accessibilityDescription }
                },
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Bluetooth,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor,
        )
    }
}

@Composable
fun LiveControlBluetoothDisconnectedBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.live_control_bluetooth_disconnected_banner),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(StatusDisconnected.copy(alpha = 0.92f))
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
    )
}

@Composable
fun BoxScope.LiveControlBluetoothDisconnectedBannerOverlay(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (visible) {
        LiveControlBluetoothDisconnectedBanner(
            onClick = onClick,
            modifier = modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 8.dp),
        )
    }
}
