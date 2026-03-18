package com.example.emitterapp.ui.bluetooth

import com.example.emitterapp.domain.bluetooth.BluetoothDevice
import com.example.emitterapp.domain.bluetooth.BluetoothMessage
import com.example.emitterapp.domain.bluetooth.RemoteDevice
import com.example.emitterapp.domain.bluetooth.TelemetryState

data class BluetoothUiState(
    val scannedDevices: List<RemoteDevice> = emptyList(),
    val pairedDevices: List<RemoteDevice> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isScanning: Boolean = false,
    val errorMessage: String? = null,
    val messages: List<BluetoothMessage> = emptyList(),
    val telemetryState: TelemetryState? = null
)
