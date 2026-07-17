package com.micsbol.telecon4esp32.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.model.JoystickMode.Companion.toStringRepresentation
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.domain.repository.ISettingsRepository
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
    val LEFT_PANEL_UNIT = stringPreferencesKey("left_panel_unit")
    val RIGHT_PANEL_UNIT = stringPreferencesKey("right_panel_unit")
    val ANALOG_INDICATOR_UNIT = stringPreferencesKey("analog_indicator_unit")
    val BATTERY_LABEL = stringPreferencesKey("battery_label")
    val PLOT_LABEL_0 = stringPreferencesKey("plot_label_0")
    val PLOT_LABEL_1 = stringPreferencesKey("plot_label_1")
    val PLOT_LABEL_2 = stringPreferencesKey("plot_label_2")
    val PLOT_LABEL_3 = stringPreferencesKey("plot_label_3")
    val LAST_DEVICE_ADDRESS = stringPreferencesKey("last_device_address")
    val LAST_DEVICE_NAME = stringPreferencesKey("last_device_name")
    val LAST_APPLICATION_ID = stringPreferencesKey("last_application_id")
}

private fun protocolModeKey(applicationId: ApplicationId) =
    stringPreferencesKey("protocol_mode_${applicationId.name}")

private fun transportTypeKey(applicationId: ApplicationId) =
    stringPreferencesKey("transport_type_${applicationId.name}")

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
            rightKnobInitialValue = rightKnobValue,
            leftPanelUnit = preferences[PreferencesKeys.LEFT_PANEL_UNIT] ?: "",
            rightPanelUnit = preferences[PreferencesKeys.RIGHT_PANEL_UNIT] ?: "",
            analogIndicatorUnit = preferences[PreferencesKeys.ANALOG_INDICATOR_UNIT] ?: "",
            batteryLabel = preferences[PreferencesKeys.BATTERY_LABEL] ?: "",
            plotLabels = listOf(
                preferences[PreferencesKeys.PLOT_LABEL_0] ?: "",
                preferences[PreferencesKeys.PLOT_LABEL_1] ?: "",
                preferences[PreferencesKeys.PLOT_LABEL_2] ?: "",
                preferences[PreferencesKeys.PLOT_LABEL_3] ?: "",
            ),
        )
    }

    override val lastDeviceFlow: Flow<Pair<String, String?>?> = context.dataStore.data.map { preferences ->
        val address = preferences[PreferencesKeys.LAST_DEVICE_ADDRESS] ?: return@map null
        address to preferences[PreferencesKeys.LAST_DEVICE_NAME]
    }

    override val lastApplicationFlow: Flow<ApplicationId?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_APPLICATION_ID]
            ?.let { runCatching { ApplicationId.valueOf(it) }.getOrNull() }
    }

    override fun protocolModeFlow(applicationId: ApplicationId): Flow<BluetoothProtocolMode> =
        context.dataStore.data.map { preferences ->
            val stored = preferences[protocolModeKey(applicationId)]
            stored?.let { BluetoothProtocolMode.fromStored(it) }
                ?: BluetoothProtocolMode.defaultFor(applicationId)
        }

    override suspend fun saveProtocolMode(applicationId: ApplicationId, mode: BluetoothProtocolMode) {
        context.dataStore.edit { preferences ->
            preferences[protocolModeKey(applicationId)] = mode.name
        }
    }

    override fun transportTypeFlow(applicationId: ApplicationId): Flow<BluetoothTransportType> =
        context.dataStore.data.map { preferences ->
            BluetoothTransportType.fromStored(preferences[transportTypeKey(applicationId)])
        }

    override suspend fun saveTransportType(
        applicationId: ApplicationId,
        transport: BluetoothTransportType,
    ) {
        context.dataStore.edit { preferences ->
            preferences[transportTypeKey(applicationId)] = transport.name
        }
    }

    override suspend fun saveLastDevice(address: String, name: String?) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_DEVICE_ADDRESS] = address
            if (name != null) preferences[PreferencesKeys.LAST_DEVICE_NAME] = name
            else preferences.remove(PreferencesKeys.LAST_DEVICE_NAME)
        }
    }

    override suspend fun saveLastApplication(applicationId: ApplicationId) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_APPLICATION_ID] = applicationId.name
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

    override suspend fun saveLeftPanelUnit(value: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LEFT_PANEL_UNIT] = value
        }
    }

    override suspend fun saveRightPanelUnit(value: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RIGHT_PANEL_UNIT] = value
        }
    }

    override suspend fun saveAnalogIndicatorUnit(value: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ANALOG_INDICATOR_UNIT] = value
        }
    }

    override suspend fun saveBatteryLabel(value: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BATTERY_LABEL] = value
        }
    }

    override suspend fun savePlotLabel(index: Int, value: String) {
        val key = when (index) {
            0 -> PreferencesKeys.PLOT_LABEL_0
            1 -> PreferencesKeys.PLOT_LABEL_1
            2 -> PreferencesKeys.PLOT_LABEL_2
            3 -> PreferencesKeys.PLOT_LABEL_3
            else -> return
        }
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}