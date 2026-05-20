package com.example.emitterapp.util

import com.example.emitterapp.domain.model.JoystickMode
import com.example.emitterapp.domain.model.UserSettings
import com.example.emitterapp.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // ── Helpers for tests ────────────────────────────────────────────────────
    fun setSettings(settings: UserSettings) = _settings.update { settings }
    fun setLastDevice(address: String, name: String?) = _lastDevice.update { address to name }
}
