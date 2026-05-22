package com.micsbol.emitterapp.domain.bluetooth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface RemoteController {
    val isConnected: StateFlow<Boolean>
    val discoveredDevices: StateFlow<List<RemoteDevice>>
    val savedDevices: StateFlow<List<RemoteDevice>>
    val error: SharedFlow<String>
    val telemetryState: StateFlow<TelemetryState>

    fun startDiscovery()
    fun stopDiscovery()

    fun connect(device: RemoteDevice): Flow<ConnectionResult>

    suspend fun sendData(data: ByteArray): Boolean?

    fun disconnect()
    fun release()
}

interface RemoteDevice {
    val name: String?
    val address: String
}
