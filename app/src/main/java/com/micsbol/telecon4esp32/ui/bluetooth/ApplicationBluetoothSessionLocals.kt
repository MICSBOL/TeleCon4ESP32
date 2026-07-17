package com.micsbol.telecon4esp32.ui.bluetooth

import androidx.compose.runtime.compositionLocalOf

/**
 * Per-application Bluetooth connect affordance provided by [ApplicationBluetoothSessionHost].
 * Uses the protocol/transport selected in that application's settings.
 */
data class ApplicationBluetoothSessionUi(
    val isConnected: Boolean,
    val isConnecting: Boolean,
    val onConnect: () -> Unit,
)

val LocalApplicationBluetoothSession = compositionLocalOf<ApplicationBluetoothSessionUi?> { null }
