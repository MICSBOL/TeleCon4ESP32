package com.example.emitterapp.ui.bluetooth

import com.example.emitterapp.domain.bluetooth.BluetoothDevice

data class BluetoothUiState(
    val scannedDevices: List<BluetoothDevice> = emptyList(),
    val pairedDevices: List<BluetoothDevice> = emptyList(),
)
