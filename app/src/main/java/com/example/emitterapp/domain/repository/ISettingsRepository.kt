package com.example.emitterapp.domain.repository

import com.example.emitterapp.domain.model.JoystickMode
import com.example.emitterapp.domain.model.RcUiStyle
import com.example.emitterapp.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for persisting user settings and device memory.
 * Lives in domain/ so use-cases and ViewModels can depend on the
 * abstraction without importing the data layer.
 */
interface ISettingsRepository {
    val settingsFlow: Flow<UserSettings>
    val lastDeviceFlow: Flow<Pair<String, String?>?>
    suspend fun saveLastDevice(address: String, name: String?)
    suspend fun saveLeftStickMode(mode: JoystickMode)
    suspend fun saveRightStickMode(mode: JoystickMode)
    suspend fun saveSwitchState(index: Int, isOn: Boolean)
    suspend fun saveLeftKnobValue(value: Float)
    suspend fun saveRightKnobValue(value: Float)
    suspend fun saveRcUiStyle(style: RcUiStyle)
}
