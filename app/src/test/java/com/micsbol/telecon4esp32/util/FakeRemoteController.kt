package com.micsbol.telecon4esp32.util

import com.micsbol.telecon4esp32.domain.bluetooth.ConnectionResult
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteController
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteDevice
import com.micsbol.telecon4esp32.domain.bluetooth.TelemetryState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

/**
 * In-memory test double for [RemoteController].
 * All tracking flags are public so tests can assert on them.
 */
class FakeRemoteController : RemoteController {
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    private val _discoveredDevices = MutableStateFlow<List<RemoteDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<RemoteDevice>> = _discoveredDevices.asStateFlow()
    private val _savedDevices = MutableStateFlow<List<RemoteDevice>>(emptyList())
    override val savedDevices: StateFlow<List<RemoteDevice>> = _savedDevices.asStateFlow()
    private val _errors = MutableSharedFlow<String>()
    override val error: SharedFlow<String> = _errors.asSharedFlow()
    private val _telemetryState = MutableStateFlow(TelemetryState())
    override val telemetryState: StateFlow<TelemetryState> = _telemetryState.asStateFlow()

    var discoveryStarted = false
    var discoveryStopped = false
    var disconnectCalled = false
    var releaseCalled = false
    val sentData = mutableListOf<ByteArray>()
    var connectionResults: List<ConnectionResult> = emptyList()

    override fun startDiscovery() {
        discoveryStarted = true
    }

    override fun stopDiscovery() {
        discoveryStopped = true
    }

    override fun connect(device: RemoteDevice): Flow<ConnectionResult> = flow {
        connectionResults.forEach { emit(it) }
    }

    override suspend fun sendData(data: ByteArray): Boolean? {
        sentData.add(data)
        return true
    }

    override fun disconnect() {
        disconnectCalled = true
        _isConnected.update { false }
    }

    override fun release() {
        releaseCalled = true
        _isConnected.update { false }
    }

    fun setSavedDevices(devices: List<RemoteDevice>) = _savedDevices.update { devices }

    fun setDiscoveredDevices(devices: List<RemoteDevice>) = _discoveredDevices.update { devices }
}

data class FakeRemoteDevice(
    override val name: String? = "Test Device",
    override val address: String = "00:11:22:33:44:55"
) : RemoteDevice
