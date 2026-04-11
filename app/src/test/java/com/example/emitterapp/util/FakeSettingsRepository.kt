package com.example.emitterapp.util
import com.example.emitterapp.data.repository.ISettingsRepository
import com.example.emitterapp.domain.model.UserSettings
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
    // ── Helpers for tests ─────────────────────────────────────────────────────
    fun setSettings(settings: UserSettings) = _settings.update { settings }
    fun setLastDevice(address: String, name: String?) = _lastDevice.update { address to name }
}
