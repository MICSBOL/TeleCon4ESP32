package com.micsbol.telecon4esp32.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.camera.SoftApHudProcessingRate
import com.micsbol.telecon4esp32.domain.camera.SoftApPerformancePreset
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.ControlPanelCenterMode
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.model.JoystickMode.Companion.toStringRepresentation
import com.micsbol.telecon4esp32.domain.model.ChannelRouting
import com.micsbol.telecon4esp32.domain.model.PlotCalibration
import com.micsbol.telecon4esp32.domain.model.RcVehicleProControlSettings
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
    val LEFT_PANEL_ON = booleanPreferencesKey("left_panel_on")
    val RIGHT_PANEL_ON = booleanPreferencesKey("right_panel_on")
    val LEFT_PANEL_COLOR_GREEN = booleanPreferencesKey("left_panel_color_green")
    val RIGHT_PANEL_COLOR_GREEN = booleanPreferencesKey("right_panel_color_green")
    val ANALOG_INDICATOR_UNIT = stringPreferencesKey("analog_indicator_unit")
    val BATTERY_LABEL = stringPreferencesKey("battery_label")
    val PLOT_LABEL_0 = stringPreferencesKey("plot_label_0")
    val PLOT_LABEL_1 = stringPreferencesKey("plot_label_1")
    val PLOT_LABEL_2 = stringPreferencesKey("plot_label_2")
    val PLOT_LABEL_3 = stringPreferencesKey("plot_label_3")
    val PLOT_OFFSET_0 = floatPreferencesKey("plot_offset_0")
    val PLOT_OFFSET_1 = floatPreferencesKey("plot_offset_1")
    val PLOT_OFFSET_2 = floatPreferencesKey("plot_offset_2")
    val PLOT_OFFSET_3 = floatPreferencesKey("plot_offset_3")
    val PLOT_SPAN_0 = floatPreferencesKey("plot_span_0")
    val PLOT_SPAN_1 = floatPreferencesKey("plot_span_1")
    val PLOT_SPAN_2 = floatPreferencesKey("plot_span_2")
    val PLOT_SPAN_3 = floatPreferencesKey("plot_span_3")
    val PLOT_UNIT_0 = stringPreferencesKey("plot_unit_0")
    val PLOT_UNIT_1 = stringPreferencesKey("plot_unit_1")
    val PLOT_UNIT_2 = stringPreferencesKey("plot_unit_2")
    val PLOT_UNIT_3 = stringPreferencesKey("plot_unit_3")
    val CHANNEL_ROUTING = stringPreferencesKey("channel_routing")
    val LAST_DEVICE_ADDRESS = stringPreferencesKey("last_device_address")
    val LAST_DEVICE_NAME = stringPreferencesKey("last_device_name")
    val LAST_APPLICATION_ID = stringPreferencesKey("last_application_id")
    val CONTROL_PANEL_CENTER_MODE = stringPreferencesKey("control_panel_center_mode")

    val RC_VP_THROTTLE_HOLD = booleanPreferencesKey("rc_vehicle_pro_throttle_hold")
    val RC_VP_STEERING_HOLD = booleanPreferencesKey("rc_vehicle_pro_steering_hold")
    val RC_VP_LEFT_STICK_MODE = stringPreferencesKey("rc_vehicle_pro_left_stick_mode")
    val RC_VP_RIGHT_STICK_MODE = stringPreferencesKey("rc_vehicle_pro_right_stick_mode")
    val RC_VP_STEER_TRIM = floatPreferencesKey("rc_vehicle_pro_steer_trim")
    val RC_VP_LEFT_TRIM_X = floatPreferencesKey("rc_vehicle_pro_left_trim_x")
    val RC_VP_LEFT_TRIM_Y = floatPreferencesKey("rc_vehicle_pro_left_trim_y")
    val RC_VP_RIGHT_TRIM_X = floatPreferencesKey("rc_vehicle_pro_right_trim_x")
    val RC_VP_RIGHT_TRIM_Y = floatPreferencesKey("rc_vehicle_pro_right_trim_y")
    val RC_VP_THROTTLE_TRAVEL = floatPreferencesKey("rc_vehicle_pro_throttle_travel")
    val RC_VP_STEER_TRAVEL = floatPreferencesKey("rc_vehicle_pro_steer_travel")
    val RC_VP_REVERSE_THROTTLE = booleanPreferencesKey("rc_vehicle_pro_reverse_throttle")
    val RC_VP_REVERSE_STEER = booleanPreferencesKey("rc_vehicle_pro_reverse_steer")
    val RC_VP_STEER_EXPO = floatPreferencesKey("rc_vehicle_pro_steer_expo")
    val RC_VP_THROTTLE_EXPO = floatPreferencesKey("rc_vehicle_pro_throttle_expo")
    val RC_VP_DEADZONE = floatPreferencesKey("rc_vehicle_pro_deadzone")
}

private fun protocolModeKey(applicationId: ApplicationId) =
    stringPreferencesKey("protocol_mode_${applicationId.name}")

private fun transportTypeKey(applicationId: ApplicationId) =
    stringPreferencesKey("transport_type_${applicationId.name}")

private fun connectionModeKey(applicationId: ApplicationId) =
    stringPreferencesKey("connection_mode_${applicationId.name}")

private fun boardKey(applicationId: ApplicationId) =
    stringPreferencesKey("esp32_board_${applicationId.name}")

private fun useSoftApCameraKey(applicationId: ApplicationId) =
    booleanPreferencesKey("use_softap_camera_${applicationId.name}")

private fun advancedSettingsRevealedKey(applicationId: ApplicationId) =
    booleanPreferencesKey("advanced_settings_revealed_${applicationId.name}")

private fun softApPerformancePresetKey(applicationId: ApplicationId) =
    stringPreferencesKey("softap_perf_preset_${applicationId.name}")

private fun softApHudProcessingRateKey(applicationId: ApplicationId) =
    stringPreferencesKey("softap_hud_processing_rate_${applicationId.name}")

private fun softApStreamQualitySeededKey(applicationId: ApplicationId) =
    booleanPreferencesKey("softap_stream_quality_seeded_${applicationId.name}")

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
            plotCalibrations = listOf(
                PlotCalibration(
                    offset = preferences[PreferencesKeys.PLOT_OFFSET_0] ?: PlotCalibration.DEFAULT_OFFSET,
                    span = preferences[PreferencesKeys.PLOT_SPAN_0] ?: PlotCalibration.DEFAULT_SPAN,
                    unit = preferences[PreferencesKeys.PLOT_UNIT_0] ?: "",
                ),
                PlotCalibration(
                    offset = preferences[PreferencesKeys.PLOT_OFFSET_1] ?: PlotCalibration.DEFAULT_OFFSET,
                    span = preferences[PreferencesKeys.PLOT_SPAN_1] ?: PlotCalibration.DEFAULT_SPAN,
                    unit = preferences[PreferencesKeys.PLOT_UNIT_1] ?: "",
                ),
                PlotCalibration(
                    offset = preferences[PreferencesKeys.PLOT_OFFSET_2] ?: PlotCalibration.DEFAULT_OFFSET,
                    span = preferences[PreferencesKeys.PLOT_SPAN_2] ?: PlotCalibration.DEFAULT_SPAN,
                    unit = preferences[PreferencesKeys.PLOT_UNIT_2] ?: "",
                ),
                PlotCalibration(
                    offset = preferences[PreferencesKeys.PLOT_OFFSET_3] ?: PlotCalibration.DEFAULT_OFFSET,
                    span = preferences[PreferencesKeys.PLOT_SPAN_3] ?: PlotCalibration.DEFAULT_SPAN,
                    unit = preferences[PreferencesKeys.PLOT_UNIT_3] ?: "",
                ),
            ),
            channelRouting = ChannelRouting.decode(
                preferences[PreferencesKeys.CHANNEL_ROUTING],
            ),
            leftPanelOn = preferences[PreferencesKeys.LEFT_PANEL_ON] ?: true,
            rightPanelOn = preferences[PreferencesKeys.RIGHT_PANEL_ON] ?: true,
            leftPanelColorGreen = preferences[PreferencesKeys.LEFT_PANEL_COLOR_GREEN] ?: true,
            rightPanelColorGreen = preferences[PreferencesKeys.RIGHT_PANEL_COLOR_GREEN] ?: true,
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

    override fun connectionModeFlow(applicationId: ApplicationId): Flow<BluetoothConnectionMode?> =
        context.dataStore.data.map { preferences ->
            BluetoothConnectionMode.fromStored(preferences[connectionModeKey(applicationId)])
        }

    override suspend fun saveConnectionMode(
        applicationId: ApplicationId,
        mode: BluetoothConnectionMode,
    ) {
        context.dataStore.edit { preferences ->
            preferences[connectionModeKey(applicationId)] = mode.name
            preferences[transportTypeKey(applicationId)] = mode.transport.name
            preferences[protocolModeKey(applicationId)] = mode.protocolMode.name
        }
    }

    override fun boardFlow(applicationId: ApplicationId): Flow<Esp32Board> =
        context.dataStore.data.map { preferences ->
            val stored = preferences[boardKey(applicationId)]
            val board = stored?.let { Esp32Board.fromStored(it) }
                ?: Esp32Board.defaultFor(applicationId)
            board.normalizedControlBoard()
        }

    override suspend fun saveBoard(applicationId: ApplicationId, board: Esp32Board) {
        context.dataStore.edit { preferences ->
            val previous = preferences[boardKey(applicationId)]
                ?.let { Esp32Board.fromStored(it) }
            if (previous == Esp32Board.CAM_AND_DEV_KIT &&
                preferences[useSoftApCameraKey(applicationId)] == null
            ) {
                preferences[useSoftApCameraKey(applicationId)] = true
            }
            preferences[boardKey(applicationId)] = board.normalizedControlBoard().name
        }
    }

    override fun useSoftApCameraFlow(applicationId: ApplicationId): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[useSoftApCameraKey(applicationId)]
                ?: (preferences[boardKey(applicationId)]?.let { Esp32Board.fromStored(it) }
                    == Esp32Board.CAM_AND_DEV_KIT)
        }

    override suspend fun saveUseSoftApCamera(applicationId: ApplicationId, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[useSoftApCameraKey(applicationId)] = enabled
        }
    }

    override fun advancedSettingsRevealedFlow(applicationId: ApplicationId): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[advancedSettingsRevealedKey(applicationId)] ?: true
        }

    override suspend fun saveAdvancedSettingsRevealed(
        applicationId: ApplicationId,
        revealed: Boolean,
    ) {
        context.dataStore.edit { preferences ->
            preferences[advancedSettingsRevealedKey(applicationId)] = revealed
        }
    }

    override fun controlPanelCenterModeFlow(): Flow<ControlPanelCenterMode> =
        context.dataStore.data.map { preferences ->
            ControlPanelCenterMode.fromStored(
                preferences[PreferencesKeys.CONTROL_PANEL_CENTER_MODE],
            )
        }

    override suspend fun saveControlPanelCenterMode(mode: ControlPanelCenterMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONTROL_PANEL_CENTER_MODE] = mode.name
        }
    }

    override fun softApPerformancePresetFlow(
        applicationId: ApplicationId,
    ): Flow<SoftApPerformancePreset> =
        context.dataStore.data.map { preferences ->
            SoftApPerformancePreset.fromStored(
                preferences[softApPerformancePresetKey(applicationId)],
                SoftApPerformancePreset.defaultFor(applicationId),
            )
        }

    override suspend fun saveSoftApPerformancePreset(
        applicationId: ApplicationId,
        preset: SoftApPerformancePreset,
    ) {
        context.dataStore.edit { preferences ->
            preferences[softApPerformancePresetKey(applicationId)] = preset.name
        }
    }

    override fun softApHudProcessingRateFlow(
        applicationId: ApplicationId,
    ): Flow<SoftApHudProcessingRate> =
        context.dataStore.data.map { preferences ->
            SoftApHudProcessingRate.fromStored(
                preferences[softApHudProcessingRateKey(applicationId)],
            )
        }

    override suspend fun saveSoftApHudProcessingRate(
        applicationId: ApplicationId,
        rate: SoftApHudProcessingRate,
    ) {
        context.dataStore.edit { preferences ->
            preferences[softApHudProcessingRateKey(applicationId)] = rate.name
        }
    }

    override suspend fun ensureSoftApStreamQualityDefaultsForAtRiskDevice(
        applicationId: ApplicationId,
    ) {
        context.dataStore.edit { preferences ->
            if (preferences[softApStreamQualitySeededKey(applicationId)] == true) {
                return@edit
            }
            preferences[softApPerformancePresetKey(applicationId)] =
                SoftApPerformancePreset.SMOOTH.name
            preferences[softApHudProcessingRateKey(applicationId)] =
                SoftApHudProcessingRate.FPS_8.name
            preferences[softApStreamQualitySeededKey(applicationId)] = true
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

    override suspend fun saveLeftPanelOn(isOn: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LEFT_PANEL_ON] = isOn
        }
    }

    override suspend fun saveRightPanelOn(isOn: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RIGHT_PANEL_ON] = isOn
        }
    }

    override suspend fun saveLeftPanelColorGreen(isGreen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LEFT_PANEL_COLOR_GREEN] = isGreen
        }
    }

    override suspend fun saveRightPanelColorGreen(isGreen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RIGHT_PANEL_COLOR_GREEN] = isGreen
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

    override suspend fun saveChannelRouting(routing: ChannelRouting) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CHANNEL_ROUTING] = routing.padded().encode()
        }
    }

    override suspend fun savePlotCalibration(index: Int, calibration: PlotCalibration) {
        val offsetKey = when (index) {
            0 -> PreferencesKeys.PLOT_OFFSET_0
            1 -> PreferencesKeys.PLOT_OFFSET_1
            2 -> PreferencesKeys.PLOT_OFFSET_2
            3 -> PreferencesKeys.PLOT_OFFSET_3
            else -> return
        }
        val spanKey = when (index) {
            0 -> PreferencesKeys.PLOT_SPAN_0
            1 -> PreferencesKeys.PLOT_SPAN_1
            2 -> PreferencesKeys.PLOT_SPAN_2
            3 -> PreferencesKeys.PLOT_SPAN_3
            else -> return
        }
        val unitKey = when (index) {
            0 -> PreferencesKeys.PLOT_UNIT_0
            1 -> PreferencesKeys.PLOT_UNIT_1
            2 -> PreferencesKeys.PLOT_UNIT_2
            3 -> PreferencesKeys.PLOT_UNIT_3
            else -> return
        }
        val span = if (calibration.span == 0f || !calibration.span.isFinite()) {
            PlotCalibration.DEFAULT_SPAN
        } else {
            calibration.span
        }
        val offset = if (calibration.offset.isFinite()) {
            calibration.offset
        } else {
            PlotCalibration.DEFAULT_OFFSET
        }
        context.dataStore.edit { preferences ->
            preferences[offsetKey] = offset
            preferences[spanKey] = span
            preferences[unitKey] = calibration.unit
        }
    }

    override fun rcVehicleProControlSettingsFlow(): Flow<RcVehicleProControlSettings> =
        context.dataStore.data.map { preferences ->
            val defaults = RcVehicleProControlSettings.DEFAULT
            RcVehicleProControlSettings(
                leftStickMode = RcVehicleProControlSettings.leftStickModeFromPersisted(
                    stored = preferences[PreferencesKeys.RC_VP_LEFT_STICK_MODE],
                    throttleHold = preferences[PreferencesKeys.RC_VP_THROTTLE_HOLD],
                ),
                rightStickMode = RcVehicleProControlSettings.rightStickModeFromPersisted(
                    stored = preferences[PreferencesKeys.RC_VP_RIGHT_STICK_MODE],
                    steeringHold = preferences[PreferencesKeys.RC_VP_STEERING_HOLD],
                ),
                leftTrimX = preferences[PreferencesKeys.RC_VP_LEFT_TRIM_X] ?: 0f,
                leftTrimY = preferences[PreferencesKeys.RC_VP_LEFT_TRIM_Y] ?: 0f,
                rightTrimX = preferences[PreferencesKeys.RC_VP_RIGHT_TRIM_X]
                    ?: preferences[PreferencesKeys.RC_VP_STEER_TRIM]
                    ?: defaults.rightTrimX,
                rightTrimY = preferences[PreferencesKeys.RC_VP_RIGHT_TRIM_Y] ?: 0f,
                throttleTravel = preferences[PreferencesKeys.RC_VP_THROTTLE_TRAVEL]
                    ?: defaults.throttleTravel,
                steerTravel = preferences[PreferencesKeys.RC_VP_STEER_TRAVEL]
                    ?: defaults.steerTravel,
                reverseThrottle = preferences[PreferencesKeys.RC_VP_REVERSE_THROTTLE]
                    ?: defaults.reverseThrottle,
                reverseSteer = preferences[PreferencesKeys.RC_VP_REVERSE_STEER]
                    ?: defaults.reverseSteer,
                steerExpo = preferences[PreferencesKeys.RC_VP_STEER_EXPO] ?: defaults.steerExpo,
                throttleExpo = preferences[PreferencesKeys.RC_VP_THROTTLE_EXPO]
                    ?: defaults.throttleExpo,
                deadzone = preferences[PreferencesKeys.RC_VP_DEADZONE] ?: defaults.deadzone,
            )
        }

    override suspend fun saveRcVehicleProControlSettings(settings: RcVehicleProControlSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RC_VP_LEFT_STICK_MODE] =
                settings.leftStickMode.toStringRepresentation()
            preferences[PreferencesKeys.RC_VP_RIGHT_STICK_MODE] =
                settings.rightStickMode.toStringRepresentation()
            // Keep hold flags in sync so older builds still restore spring vs hold.
            preferences[PreferencesKeys.RC_VP_THROTTLE_HOLD] = !settings.leftStickMode.isSpring
            preferences[PreferencesKeys.RC_VP_STEERING_HOLD] = !settings.rightStickMode.isSpring
            preferences[PreferencesKeys.RC_VP_LEFT_TRIM_X] = settings.leftTrimX
            preferences[PreferencesKeys.RC_VP_LEFT_TRIM_Y] = settings.leftTrimY
            preferences[PreferencesKeys.RC_VP_RIGHT_TRIM_X] = settings.rightTrimX
            preferences[PreferencesKeys.RC_VP_RIGHT_TRIM_Y] = settings.rightTrimY
            // Keep the old steer-trim key in sync for rollback / car firmware center.
            preferences[PreferencesKeys.RC_VP_STEER_TRIM] = settings.rightTrimX
            preferences[PreferencesKeys.RC_VP_THROTTLE_TRAVEL] = settings.throttleTravel
            preferences[PreferencesKeys.RC_VP_STEER_TRAVEL] = settings.steerTravel
            preferences[PreferencesKeys.RC_VP_REVERSE_THROTTLE] = settings.reverseThrottle
            preferences[PreferencesKeys.RC_VP_REVERSE_STEER] = settings.reverseSteer
            preferences[PreferencesKeys.RC_VP_STEER_EXPO] = settings.steerExpo
            preferences[PreferencesKeys.RC_VP_THROTTLE_EXPO] = settings.throttleExpo
            preferences[PreferencesKeys.RC_VP_DEADZONE] = settings.deadzone
        }
    }
}