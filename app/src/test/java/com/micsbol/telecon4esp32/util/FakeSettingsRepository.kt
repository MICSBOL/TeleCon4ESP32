package com.micsbol.telecon4esp32.util

import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory test double for [ISettingsRepository].
 * No Android context or DataStore needed.
 */
class FakeSettingsRepository : ISettingsRepository {
    private val _settings = MutableStateFlow(UserSettings())
    override val settingsFlow: Flow<UserSettings> = _settings.asStateFlow()
    private val _lastDevice = MutableStateFlow<Pair<String, String?>?>(null)
    override val lastDeviceFlow: Flow<Pair<String, String?>?> = _lastDevice.asStateFlow()
    private val _protocolModes = MutableStateFlow<Map<ApplicationId, BluetoothProtocolMode>>(emptyMap())

    /** Records the last call to saveLastDevice so tests can assert on it. */
    var savedDevice: Pair<String, String?>? = null
    override suspend fun saveLastDevice(address: String, name: String?) {
        savedDevice = address to name
        _lastDevice.update { address to name }
    }
    override suspend fun saveLeftStickMode(mode: JoystickMode) {
        _settings.update { it.copy(leftStickMode = mode) }
    }
    override suspend fun saveRightStickMode(mode: JoystickMode) {
        _settings.update { it.copy(rightStickMode = mode) }
    }
    override suspend fun saveSwitchState(index: Int, isOn: Boolean) {
        _settings.update { it.copy(switchInitialStates = it.switchInitialStates + (index to isOn)) }
    }
    override suspend fun saveLeftKnobValue(value: Float) {
        _settings.update { it.copy(leftKnobInitialValue = value) }
    }
    override suspend fun saveRightKnobValue(value: Float) {
        _settings.update { it.copy(rightKnobInitialValue = value) }
    }

    override suspend fun saveLeftPanelUnit(value: String) {
        _settings.update { it.copy(leftPanelUnit = value) }
    }

    override suspend fun saveRightPanelUnit(value: String) {
        _settings.update { it.copy(rightPanelUnit = value) }
    }

    override suspend fun saveAnalogIndicatorUnit(value: String) {
        _settings.update { it.copy(analogIndicatorUnit = value) }
    }

    override suspend fun saveBatteryLabel(value: String) {
        _settings.update { it.copy(batteryLabel = value) }
    }

    override suspend fun savePlotLabel(index: Int, value: String) {
        if (index !in 0 until UserSettings.PLOT_LABEL_COUNT) return
        _settings.update { settings ->
            val updated = settings.plotLabels.toMutableList()
            updated[index] = value
            settings.copy(plotLabels = updated)
        }
    }

    override fun protocolModeFlow(applicationId: ApplicationId): Flow<BluetoothProtocolMode> =
        _protocolModes.map { modes ->
            modes[applicationId] ?: BluetoothProtocolMode.defaultFor(applicationId)
        }

    override suspend fun saveProtocolMode(applicationId: ApplicationId, mode: BluetoothProtocolMode) {
        _protocolModes.update { it + (applicationId to mode) }
    }

    // ── Helpers for tests ────────────────────────────────────────────────────
    fun setSettings(settings: UserSettings) = _settings.update { settings }
    fun setLastDevice(address: String, name: String?) = _lastDevice.update { address to name }
}
