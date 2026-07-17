package com.micsbol.telecon4esp32.domain.bluetooth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface RemoteController {
    val isConnected: StateFlow<Boolean>
    val discoveredDevices: StateFlow<List<RemoteDevice>>
    val savedDevices: StateFlow<List<RemoteDevice>>
    val error: SharedFlow<String>
    val telemetryState: StateFlow<TelemetryState>
    val messages: SharedFlow<EspMessage>

    fun startDiscovery(transport: BluetoothTransportType = BluetoothTransportType.CLASSIC)
    fun stopDiscovery()

    fun connect(
        device: RemoteDevice,
        transport: BluetoothTransportType = BluetoothTransportType.CLASSIC,
    ): Flow<ConnectionResult>

    suspend fun sendData(data: ByteArray): Boolean?

    /** Sends a line-protocol message. A trailing newline is appended when missing. */
    suspend fun sendLine(line: String): Boolean?

    fun disconnect()
    fun release()
}

interface RemoteDevice {
    val name: String?
    val address: String
}
