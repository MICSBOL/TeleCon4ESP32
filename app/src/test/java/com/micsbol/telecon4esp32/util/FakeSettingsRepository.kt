package com.micsbol.telecon4esp32.util

import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.camera.SoftApHudProcessingRate
import com.micsbol.telecon4esp32.domain.camera.SoftApPerformancePreset
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.ChannelRouting
import com.micsbol.telecon4esp32.domain.model.ControlPanelCenterMode
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.model.PlotCalibration
import com.micsbol.telecon4esp32.domain.model.RcVehicleProControlSettings
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.domain.repository.ISettingsRepository
import com.micsbol.telecon4esp32.domain.use_case.SaveAnalogIndicatorUnitUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveBatteryLabelUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveChannelRoutingUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveLeftKnobValueUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveLeftPanelColorGreenUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveLeftPanelOnUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveLeftPanelUnitUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveLeftStickModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.SavePlotCalibrationUseCase
import com.micsbol.telecon4esp32.domain.use_case.SavePlotLabelUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveRightKnobValueUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveRightPanelColorGreenUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveRightPanelOnUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveRightPanelUnitUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveRightStickModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveSettingsUseCases
import com.micsbol.telecon4esp32.domain.use_case.SaveSwitchStateUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    override suspend fun saveLeftPanelOn(isOn: Boolean) {
        _settings.update { it.copy(leftPanelOn = isOn) }
    }

    override suspend fun saveRightPanelOn(isOn: Boolean) {
        _settings.update { it.copy(rightPanelOn = isOn) }
    }

    override suspend fun saveLeftPanelColorGreen(isGreen: Boolean) {
        _settings.update { it.copy(leftPanelColorGreen = isGreen) }
    }

    override suspend fun saveRightPanelColorGreen(isGreen: Boolean) {
        _settings.update { it.copy(rightPanelColorGreen = isGreen) }
    }

    override suspend fun savePlotLabel(index: Int, value: String) {
        if (index !in 0 until UserSettings.PLOT_LABEL_COUNT) return
        _settings.update { settings ->
            val updated = settings.plotLabels.toMutableList()
            updated[index] = value
            settings.copy(plotLabels = updated)
        }
    }

    override suspend fun savePlotCalibration(index: Int, calibration: PlotCalibration) {
        if (index !in 0 until UserSettings.PLOT_LABEL_COUNT) return
        _settings.update { settings ->
            val updated = PlotCalibration.padded(settings.plotCalibrations).toMutableList()
            updated[index] = calibration
            settings.copy(plotCalibrations = updated)
        }
    }

    override suspend fun saveChannelRouting(routing: ChannelRouting) {
        _settings.update { it.copy(channelRouting = routing.padded()) }
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
            (boards[applicationId] ?: Esp32Board.defaultFor(applicationId))
                .normalizedControlBoard()
        }

    override suspend fun saveBoard(applicationId: ApplicationId, board: Esp32Board) {
        _boards.update { boards ->
            val previous = boards[applicationId]
            if (previous == Esp32Board.CAM_AND_DEV_KIT &&
                applicationId !in _useSoftApCamera.value
            ) {
                _useSoftApCamera.update { it + (applicationId to true) }
            }
            boards + (applicationId to board.normalizedControlBoard())
        }
    }

    private val _useSoftApCamera = MutableStateFlow<Map<ApplicationId, Boolean>>(emptyMap())

    override fun useSoftApCameraFlow(applicationId: ApplicationId): Flow<Boolean> =
        combine(_useSoftApCamera, _boards) { overlay, boards ->
            overlay[applicationId]
                ?: (boards[applicationId] == Esp32Board.CAM_AND_DEV_KIT)
        }

    override suspend fun saveUseSoftApCamera(applicationId: ApplicationId, enabled: Boolean) {
        _useSoftApCamera.update { it + (applicationId to enabled) }
    }

    private val _advancedSettingsRevealed =
        MutableStateFlow<Map<ApplicationId, Boolean>>(emptyMap())

    override fun advancedSettingsRevealedFlow(applicationId: ApplicationId): Flow<Boolean> =
        _advancedSettingsRevealed.map { revealed ->
            revealed[applicationId] ?: true
        }

    override suspend fun saveAdvancedSettingsRevealed(
        applicationId: ApplicationId,
        revealed: Boolean,
    ) {
        _advancedSettingsRevealed.update { it + (applicationId to revealed) }
    }

    private val _controlPanelCenterMode =
        MutableStateFlow(ControlPanelCenterMode.PLOTS)

    override fun controlPanelCenterModeFlow(): Flow<ControlPanelCenterMode> =
        _controlPanelCenterMode.asStateFlow()

    override suspend fun saveControlPanelCenterMode(mode: ControlPanelCenterMode) {
        _controlPanelCenterMode.update { mode }
    }

    private val _softApPresets =
        MutableStateFlow<Map<ApplicationId, SoftApPerformancePreset>>(emptyMap())

    override fun softApPerformancePresetFlow(
        applicationId: ApplicationId,
    ): Flow<SoftApPerformancePreset> =
        _softApPresets.map { presets ->
            presets[applicationId] ?: SoftApPerformancePreset.defaultFor(applicationId)
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

    private val _softApStreamQualitySeeded =
        MutableStateFlow<Set<ApplicationId>>(emptySet())

    override suspend fun ensureSoftApStreamQualityDefaultsForAtRiskDevice(
        applicationId: ApplicationId,
    ) {
        if (applicationId in _softApStreamQualitySeeded.value) return
        _softApPresets.update { it + (applicationId to SoftApPerformancePreset.SMOOTH) }
        _softApHudRates.update { it + (applicationId to SoftApHudProcessingRate.FPS_8) }
        _softApStreamQualitySeeded.update { it + applicationId }
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

    fun toSaveSettingsUseCases() = SaveSettingsUseCases(
        saveLeftStickMode = SaveLeftStickModeUseCase(this),
        saveRightStickMode = SaveRightStickModeUseCase(this),
        saveSwitchState = SaveSwitchStateUseCase(this),
        saveLeftKnobValue = SaveLeftKnobValueUseCase(this),
        saveRightKnobValue = SaveRightKnobValueUseCase(this),
        saveLeftPanelUnit = SaveLeftPanelUnitUseCase(this),
        saveRightPanelUnit = SaveRightPanelUnitUseCase(this),
        saveAnalogIndicatorUnit = SaveAnalogIndicatorUnitUseCase(this),
        saveBatteryLabel = SaveBatteryLabelUseCase(this),
        savePlotLabel = SavePlotLabelUseCase(this),
        savePlotCalibration = SavePlotCalibrationUseCase(this),
        saveChannelRouting = SaveChannelRoutingUseCase(this),
        saveLeftPanelOn = SaveLeftPanelOnUseCase(this),
        saveRightPanelOn = SaveRightPanelOnUseCase(this),
        saveLeftPanelColorGreen = SaveLeftPanelColorGreenUseCase(this),
        saveRightPanelColorGreen = SaveRightPanelColorGreenUseCase(this),
    )
}
