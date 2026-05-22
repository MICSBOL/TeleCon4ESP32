package com.micsbol.emitterapp.ui.bluetooth

import com.micsbol.emitterapp.domain.bluetooth.RemoteDevice

data class BluetoothUiState(
    val scannedDevices: List<RemoteDevice> = emptyList(),
    val pairedDevices: List<RemoteDevice> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isScanning: Boolean = false,
    val errorMessage: String? = null,
)
