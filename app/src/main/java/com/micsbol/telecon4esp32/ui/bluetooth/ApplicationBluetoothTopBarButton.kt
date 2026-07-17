package com.micsbol.telecon4esp32.ui.bluetooth

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Top-bar Bluetooth connect control for application screens.
 * Reads [LocalApplicationBluetoothSession] at composition time so the action stays current.
 */
@Composable
fun ApplicationBluetoothTopBarButton(
    accent: Color,
    button: @Composable (
        onClick: () -> Unit,
        enabled: Boolean,
        content: @Composable () -> Unit,
    ) -> Unit,
) {
    val session = LocalApplicationBluetoothSession.current ?: return
    val canConnect = !session.isConnected && !session.isConnecting
    button(session.onConnect, canConnect) {
        ApplicationBluetoothToolbarIcon(
            isConnected = session.isConnected,
            isConnecting = session.isConnecting,
            accent = accent,
        )
    }
}
