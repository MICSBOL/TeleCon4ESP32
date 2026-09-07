package com.micsbol.telecon4esp32.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.theme.StatusConnected
import com.micsbol.telecon4esp32.ui.theme.StatusDisconnected

/**
 * Lets application screens push the disconnected banner above bottom chrome
 * without overlapping content.
 */
class DisconnectedBannerInsets {
    var bottomClearance by mutableStateOf(16.dp)
    /** When true, the host overlay is hidden so the screen can place the banner in-flow. */
    var suppressHostOverlay by mutableStateOf(false)
}

val LocalDisconnectedBannerInsets = staticCompositionLocalOf { DisconnectedBannerInsets() }

/** How the live-control link status chip should present connection state. */
enum class LiveControlLinkKind {
    Bluetooth,
    WifiSoftAp,
    /** SoftAP HTTP camera overlay (video-only; Wi‑Fi join required). */
    CameraSoftAp,
}

@Composable
fun LiveControlBluetoothStatusChip(
    isConnected: Boolean,
    isConnecting: Boolean = false,
    onDisconnectedClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    linkKind: LiveControlLinkKind = LiveControlLinkKind.Bluetooth,
    /** SoftAP: camera stream reachable but control TCP not yet connected. */
    isWifiCameraOnly: Boolean = false,
) {
    val wifiCameraOnly = linkKind == LiveControlLinkKind.WifiSoftAp &&
        isWifiCameraOnly &&
        !isConnected &&
        !isConnecting

    val backgroundColor = when {
        isConnecting -> brandPrimary().copy(alpha = 0.18f)
        isConnected -> StatusConnected.copy(alpha = 0.18f)
        wifiCameraOnly -> brandPrimary().copy(alpha = 0.18f)
        else -> StatusDisconnected.copy(alpha = 0.18f)
    }
    val contentColor = when {
        isConnecting -> brandPrimary()
        isConnected -> StatusConnected
        wifiCameraOnly -> brandPrimary()
        else -> StatusDisconnected
    }
    val accessibilityDescription = when {
        isConnecting -> stringResource(R.string.live_control_bluetooth_connecting_content_description)
        isConnected && linkKind == LiveControlLinkKind.CameraSoftAp ->
            stringResource(R.string.rc_vehicle_cam_online)
        isConnected && linkKind == LiveControlLinkKind.WifiSoftAp ->
            stringResource(R.string.live_control_wifi_connected_content_description)
        isConnected -> stringResource(R.string.live_control_bluetooth_connected_content_description)
        wifiCameraOnly -> stringResource(R.string.live_control_wifi_camera_only_content_description)
        linkKind == LiveControlLinkKind.CameraSoftAp ->
            stringResource(R.string.rc_vehicle_cam_offline)
        linkKind == LiveControlLinkKind.WifiSoftAp ->
            stringResource(R.string.live_control_wifi_disconnected_content_description)
        else -> stringResource(R.string.live_control_bluetooth_disconnected_content_description)
    }
    val statusIcon: ImageVector = when (linkKind) {
        LiveControlLinkKind.CameraSoftAp,
        LiveControlLinkKind.WifiSoftAp -> Icons.Default.Wifi
        LiveControlLinkKind.Bluetooth -> Icons.Default.Bluetooth
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
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = statusIcon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
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
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun BoxScope.LiveControlBluetoothDisconnectedBannerOverlay(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bottomClearance: Dp = LocalDisconnectedBannerInsets.current.bottomClearance,
) {
    if (visible) {
        val isLandscape =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val clearance = if (isLandscape) {
            maxOf(bottomClearance, 96.dp)
        } else {
            bottomClearance
        }
        LiveControlBluetoothDisconnectedBanner(
            onClick = onClick,
            modifier = modifier
                // Keep clear of bottom control rows / sticky panels.
                .align(Alignment.BottomCenter)
                .safeHudPadding(
                    includeTop = false,
                    includeBottom = true,
                    includeHorizontal = true,
                )
                .padding(bottom = clearance),
        )
    }
}
