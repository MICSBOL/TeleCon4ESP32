package com.micsbol.telecon4esp32.domain.repository

import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.JoystickMode
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
    fun boardFlow(applicationId: ApplicationId): Flow<Esp32Board>
    suspend fun saveLastDevice(address: String, name: String?)
    suspend fun saveLastApplication(applicationId: ApplicationId)
    suspend fun saveProtocolMode(applicationId: ApplicationId, mode: BluetoothProtocolMode)
    suspend fun saveTransportType(applicationId: ApplicationId, transport: BluetoothTransportType)
    suspend fun saveBoard(applicationId: ApplicationId, board: Esp32Board)
    suspend fun saveLeftStickMode(mode: JoystickMode)
    suspend fun saveRightStickMode(mode: JoystickMode)
    suspend fun saveSwitchState(index: Int, isOn: Boolean)
    suspend fun saveLeftKnobValue(value: Float)
    suspend fun saveRightKnobValue(value: Float)
    suspend fun saveLeftPanelUnit(value: String)
    suspend fun saveRightPanelUnit(value: String)
    suspend fun saveAnalogIndicatorUnit(value: String)
    suspend fun saveBatteryLabel(value: String)
    suspend fun savePlotLabel(index: Int, value: String)
}
