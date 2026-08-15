package com.micsbol.telecon4esp32.ui.bluetooth

import androidx.compose.runtime.compositionLocalOf
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType

/**
 * Per-application connect affordance provided by [ApplicationBluetoothSessionHost].
 * Uses the protocol/transport selected in that application's settings.
 */
data class ApplicationBluetoothSessionUi(
    val isConnected: Boolean,
    val isConnecting: Boolean,
    val transport: BluetoothTransportType = BluetoothTransportType.CLASSIC,
    val onConnect: () -> Unit,
) {
    val usesWifiLink: Boolean
        get() = transport == BluetoothTransportType.WIFI
}

val LocalApplicationBluetoothSession = compositionLocalOf<ApplicationBluetoothSessionUi?> { null }
