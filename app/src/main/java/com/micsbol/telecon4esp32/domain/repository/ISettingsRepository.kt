package com.micsbol.telecon4esp32.domain.repository

import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.camera.SoftApHudProcessingRate
import com.micsbol.telecon4esp32.domain.camera.SoftApPerformancePreset
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.ControlPanelCenterMode
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.model.JoystickRangeShape
import com.micsbol.telecon4esp32.domain.model.RcVehicleProControlSettings
import com.micsbol.telecon4esp32.domain.model.ChannelRouting
import com.micsbol.telecon4esp32.domain.model.PlotCalibration
import com.micsbol.telecon4esp32.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for persisting user settings and device memory.
 * Lives in domain/ so use-cases and ViewModels can depend on the
 * abstraction without importing the data layer.
 */
interface ISettingsRepository {
    val settingsFlow: Flow<UserSettings>
    val lastDeviceFlow: Flow<Pair<String, String?>?>
    val lastApplicationFlow: Flow<ApplicationId?>
    fun protocolModeFlow(applicationId: ApplicationId): Flow<BluetoothProtocolMode>
    fun transportTypeFlow(applicationId: ApplicationId): Flow<BluetoothTransportType>
    fun connectionModeFlow(applicationId: ApplicationId): Flow<BluetoothConnectionMode?>
    fun boardFlow(applicationId: ApplicationId): Flow<Esp32Board>
    fun useSoftApCameraFlow(applicationId: ApplicationId): Flow<Boolean>
    fun advancedSettingsRevealedFlow(applicationId: ApplicationId): Flow<Boolean>
    fun controlPanelCenterModeFlow(): Flow<ControlPanelCenterMode>
    fun softApPerformancePresetFlow(applicationId: ApplicationId): Flow<SoftApPerformancePreset>
    fun softApHudProcessingRateFlow(applicationId: ApplicationId): Flow<SoftApHudProcessingRate>
    suspend fun saveLastDevice(address: String, name: String?)
    suspend fun saveLastApplication(applicationId: ApplicationId)
    suspend fun saveProtocolMode(applicationId: ApplicationId, mode: BluetoothProtocolMode)
    suspend fun saveTransportType(applicationId: ApplicationId, transport: BluetoothTransportType)
    suspend fun saveConnectionMode(applicationId: ApplicationId, mode: BluetoothConnectionMode)
    suspend fun saveBoard(applicationId: ApplicationId, board: Esp32Board)
    suspend fun saveUseSoftApCamera(applicationId: ApplicationId, enabled: Boolean)
    suspend fun saveAdvancedSettingsRevealed(applicationId: ApplicationId, revealed: Boolean)
    suspend fun saveControlPanelCenterMode(mode: ControlPanelCenterMode)
    suspend fun saveSoftApPerformancePreset(
        applicationId: ApplicationId,
        preset: SoftApPerformancePreset,
    )
    suspend fun saveSoftApHudProcessingRate(
        applicationId: ApplicationId,
        rate: SoftApHudProcessingRate,
    )
    /**
     * One-time seed for mid/low-end phones: Smooth preset + 10 FPS HUD rate.
     * No-op when defaults were already seeded for [applicationId].
     */
    suspend fun ensureSoftApStreamQualityDefaultsForAtRiskDevice(applicationId: ApplicationId)
    suspend fun saveLeftStickMode(mode: JoystickMode)
    suspend fun saveRightStickMode(mode: JoystickMode)
    suspend fun saveLeftStickRangeShape(shape: JoystickRangeShape)
    suspend fun saveRightStickRangeShape(shape: JoystickRangeShape)
    suspend fun saveSwitchState(index: Int, isOn: Boolean)
    suspend fun saveLeftKnobValue(value: Float)
    suspend fun saveRightKnobValue(value: Float)
    suspend fun saveLeftPanelUnit(value: String)
    suspend fun saveRightPanelUnit(value: String)
    suspend fun saveAnalogIndicatorUnit(value: String)
    suspend fun saveBatteryLabel(value: String)
    suspend fun savePlotLabel(index: Int, value: String)
    suspend fun savePlotCalibration(index: Int, calibration: PlotCalibration)
    suspend fun saveChannelRouting(routing: ChannelRouting)
    suspend fun saveLeftPanelOn(isOn: Boolean)
    suspend fun saveRightPanelOn(isOn: Boolean)
    suspend fun saveLeftPanelColorGreen(isGreen: Boolean)
    suspend fun saveRightPanelColorGreen(isGreen: Boolean)

    fun rcVehicleProControlSettingsFlow(): Flow<RcVehicleProControlSettings>
    suspend fun saveRcVehicleProControlSettings(settings: RcVehicleProControlSettings)

    fun rcVehicleProHudPlotChromeFlow(): Flow<String>
    suspend fun saveRcVehicleProHudPlotChrome(encoded: String)

    fun controlPanelPlotDisplayFlow(): Flow<String>
    suspend fun saveControlPanelPlotDisplay(encoded: String)
}
