package com.example.emitterapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.emitterapp.domain.model.JoystickMode
import com.example.emitterapp.domain.model.JoystickMode.Companion.toStringRepresentation
import com.example.emitterapp.domain.model.UserSettings
import com.example.emitterapp.domain.repository.ISettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private object PreferencesKeys {
    val LEFT_STICK_MODE = stringPreferencesKey("left_stick_mode")
    val RIGHT_STICK_MODE = stringPreferencesKey("right_stick_mode")
    val SWITCH_STATES = stringPreferencesKey("switch_states")
    val LEFT_KNOB_VALUE = floatPreferencesKey("left_knob_value")
    val RIGHT_KNOB_VALUE = floatPreferencesKey("right_knob_value")
    val LAST_DEVICE_ADDRESS = stringPreferencesKey("last_device_address")
    val LAST_DEVICE_NAME = stringPreferencesKey("last_device_name")
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rc_settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) : ISettingsRepository {

    override val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        val leftModeString = preferences[PreferencesKeys.LEFT_STICK_MODE]
        val rightModeString = preferences[PreferencesKeys.RIGHT_STICK_MODE]
        val switchStatesString = preferences[PreferencesKeys.SWITCH_STATES]
        val leftKnobValue = preferences[PreferencesKeys.LEFT_KNOB_VALUE] ?: 0.5f
        val rightKnobValue = preferences[PreferencesKeys.RIGHT_KNOB_VALUE] ?: 0.5f

        val switchStatesMap = switchStatesString?.split(",")?.mapIndexed { index, s ->
            index to (s.toBooleanStrictOrNull() ?: false)
        }?.toMap() ?: (0..5).associateWith { false }

        UserSettings(
            leftStickMode = JoystickMode.fromString(leftModeString),
            rightStickMode = JoystickMode.fromString(rightModeString),
            switchInitialStates = switchStatesMap,
            leftKnobInitialValue = leftKnobValue,
            rightKnobInitialValue = rightKnobValue
        )
    }

    override val lastDeviceFlow: Flow<Pair<String, String?>?> = context.dataStore.data.map { preferences ->
        val address = preferences[PreferencesKeys.LAST_DEVICE_ADDRESS] ?: return@map null
        address to preferences[PreferencesKeys.LAST_DEVICE_NAME]
    }

    override suspend fun saveLastDevice(address: String, name: String?) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_DEVICE_ADDRESS] = address
            if (name != null) preferences[PreferencesKeys.LAST_DEVICE_NAME] = name
            else preferences.remove(PreferencesKeys.LAST_DEVICE_NAME)
        }
    }

    override suspend fun saveLeftStickMode(mode: JoystickMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LEFT_STICK_MODE] = mode.toStringRepresentation()
        }
    }

    override suspend fun saveRightStickMode(mode: JoystickMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RIGHT_STICK_MODE] = mode.toStringRepresentation()
        }
    }

    override suspend fun saveSwitchState(index: Int, isOn: Boolean) {
        context.dataStore.edit { preferences ->
            val currentStateString = preferences[PreferencesKeys.SWITCH_STATES]
                ?: "false,false,false,false,false,false"
            val states = currentStateString.split(",").toMutableList()
            if (index in states.indices) states[index] = isOn.toString()
            preferences[PreferencesKeys.SWITCH_STATES] = states.joinToString(",")
        }
    }

    override suspend fun saveLeftKnobValue(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LEFT_KNOB_VALUE] = value
        }
    }

    override suspend fun saveRightKnobValue(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RIGHT_KNOB_VALUE] = value
        }
    }
}