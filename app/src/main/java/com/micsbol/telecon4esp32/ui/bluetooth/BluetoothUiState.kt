package com.micsbol.telecon4esp32.ui.bluetooth

import com.micsbol.telecon4esp32.domain.bluetooth.ActiveBluetoothSession
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectFailure
import com.micsbol.telecon4esp32.domain.bluetooth.HandshakeFailure
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteDevice

data class BluetoothUiState(
    val scannedDevices: List<RemoteDevice> = emptyList(),
    val pairedDevices: List<RemoteDevice> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isScanning: Boolean = false,
    val errorMessage: String? = null,
    val connectFailure: BluetoothConnectFailure? = null,
    val handshakeFailure: HandshakeFailure? = null,
    val activeSession: ActiveBluetoothSession? = null,
)
