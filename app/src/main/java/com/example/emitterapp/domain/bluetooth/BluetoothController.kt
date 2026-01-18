package com.example.emitterapp.domain.bluetooth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val isConnected: StateFlow<Boolean>
    val scannedDevices: StateFlow<List<BluetoothDevice>>
    val pairedDevices: StateFlow<List<BluetoothDevice>>
    val error: SharedFlow<String>
    val panelState: StateFlow<PanelState>
    val indicatorState: StateFlow<IndicatorState>
    val plotState: StateFlow<PlotState>
    fun startDiscovery()
    fun stopDiscovery()
    fun startBluetoothServer(): Flow<ConnectionResult>
    fun connectToDevice(device: BluetoothDevice): Flow<ConnectionResult>
    suspend fun trySendMessage(message: String): BluetoothMessage?
    suspend fun trySendData(data: ByteArray): Boolean?
    fun closeConnection()
    fun release()
}