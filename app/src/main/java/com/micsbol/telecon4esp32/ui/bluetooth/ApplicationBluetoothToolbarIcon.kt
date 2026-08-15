package com.micsbol.telecon4esp32.ui.bluetooth

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.theme.StatusConnected
import com.micsbol.telecon4esp32.ui.theme.StatusDisconnected

/**
 * Shared connect glyph for application top-bar buttons (Bluetooth or Wi‑Fi SoftAP).
 */
@Composable
fun ApplicationBluetoothToolbarIcon(
    isConnected: Boolean,
    isConnecting: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    usesWifiLink: Boolean = false,
) {
    val tint = when {
        isConnecting -> accent
        isConnected -> StatusConnected
        else -> StatusDisconnected
    }
    val description = when {
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
    Icon(
        imageVector = if (usesWifiLink) Icons.Default.Wifi else Icons.Default.Bluetooth,
        contentDescription = description,
        tint = tint,
        modifier = modifier.size(size),
    )
}
