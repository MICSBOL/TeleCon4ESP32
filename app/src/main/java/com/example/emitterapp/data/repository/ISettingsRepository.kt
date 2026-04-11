package com.example.emitterapp.data.repository

import com.example.emitterapp.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over the concrete DataStore-backed [SettingsRepository].
 * Allows unit tests to inject a lightweight fake without any Android context.
 */
interface ISettingsRepository {
    val settingsFlow: Flow<UserSettings>
    val lastDeviceFlow: Flow<Pair<String, String?>?>
    suspend fun saveLastDevice(address: String, name: String?)
}

