package com.micsbol.telecon4esp32.util

import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.camera.SoftApHudProcessingRate
import com.micsbol.telecon4esp32.domain.camera.SoftApPerformancePreset
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.model.RcVehicleProControlSettings
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
    private val _lastApplication = MutableStateFlow<ApplicationId?>(null)
    override val lastApplicationFlow: Flow<ApplicationId?> = _lastApplication.asStateFlow()
    private val _protocolModes = MutableStateFlow<Map<ApplicationId, BluetoothProtocolMode>>(emptyMap())

    /** Records the last call to saveLastDevice so tests can assert on it. */
    var savedDevice: Pair<String, String?>? = null
    override suspend fun saveLastDevice(address: String, name: String?) {
        savedDevice = address to name
        _lastDevice.update { address to name }
    }

    override suspend fun saveLastApplication(applicationId: ApplicationId) {
        _lastApplication.update { applicationId }
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

    private val _transportTypes =
        MutableStateFlow<Map<ApplicationId, BluetoothTransportType>>(emptyMap())

    override fun transportTypeFlow(applicationId: ApplicationId): Flow<BluetoothTransportType> =
        _transportTypes.map { transports ->
            transports[applicationId] ?: BluetoothTransportType.CLASSIC
        }

    override suspend fun saveTransportType(
        applicationId: ApplicationId,
        transport: BluetoothTransportType,
    ) {
        _transportTypes.update { it + (applicationId to transport) }
    }

    private val _connectionModes =
        MutableStateFlow<Map<ApplicationId, BluetoothConnectionMode>>(emptyMap())

    override fun connectionModeFlow(applicationId: ApplicationId): Flow<BluetoothConnectionMode?> =
        _connectionModes.map { modes -> modes[applicationId] }

    override suspend fun saveConnectionMode(
        applicationId: ApplicationId,
        mode: BluetoothConnectionMode,
    ) {
        _connectionModes.update { it + (applicationId to mode) }
    }

    private val _boards = MutableStateFlow<Map<ApplicationId, Esp32Board>>(emptyMap())

    override fun boardFlow(applicationId: ApplicationId): Flow<Esp32Board> =
        _boards.map { boards ->
            boards[applicationId] ?: Esp32Board.defaultFor(applicationId)
        }

    override suspend fun saveBoard(applicationId: ApplicationId, board: Esp32Board) {
        _boards.update { it + (applicationId to board) }
    }

    private val _softApPresets =
        MutableStateFlow<Map<ApplicationId, SoftApPerformancePreset>>(emptyMap())

    override fun softApPerformancePresetFlow(
        applicationId: ApplicationId,
    ): Flow<SoftApPerformancePreset> =
        _softApPresets.map { presets ->
            presets[applicationId] ?: SoftApPerformancePreset.DEFAULT
        }

    override suspend fun saveSoftApPerformancePreset(
        applicationId: ApplicationId,
        preset: SoftApPerformancePreset,
    ) {
        _softApPresets.update { it + (applicationId to preset) }
    }

    private val _softApHudRates =
        MutableStateFlow<Map<ApplicationId, SoftApHudProcessingRate>>(emptyMap())

    override fun softApHudProcessingRateFlow(
        applicationId: ApplicationId,
    ): Flow<SoftApHudProcessingRate> =
        _softApHudRates.map { rates ->
            rates[applicationId] ?: SoftApHudProcessingRate.DEFAULT
        }

    override suspend fun saveSoftApHudProcessingRate(
        applicationId: ApplicationId,
        rate: SoftApHudProcessingRate,
    ) {
        _softApHudRates.update { it + (applicationId to rate) }
    }

    private val _rcVehicleProControl =
        MutableStateFlow(RcVehicleProControlSettings.DEFAULT)

    override fun rcVehicleProControlSettingsFlow(): Flow<RcVehicleProControlSettings> =
        _rcVehicleProControl.asStateFlow()

    override suspend fun saveRcVehicleProControlSettings(settings: RcVehicleProControlSettings) {
        _rcVehicleProControl.update { settings }
    }

    // ── Helpers for tests ────────────────────────────────────────────────────
    fun setSettings(settings: UserSettings) = _settings.update { settings }
    fun setLastDevice(address: String, name: String?) = _lastDevice.update { address to name }
    fun setLastApplication(applicationId: ApplicationId?) = _lastApplication.update { applicationId }
}
