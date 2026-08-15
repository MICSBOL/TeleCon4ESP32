package com.micsbol.telecon4esp32.ui.smarthome

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MotionPhotosAuto
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Window
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteController
import com.micsbol.telecon4esp32.domain.bluetooth.SimpleProtocolEncoder
import com.micsbol.telecon4esp32.domain.bluetooth.sh.ShBinaryProtocol
import com.micsbol.telecon4esp32.domain.bluetooth.sh.ShPacketEncoder
import com.micsbol.telecon4esp32.domain.bluetooth.sh.SmartHomeProtocol
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Entitlement
import com.micsbol.telecon4esp32.domain.model.effectiveProtocolMode
import com.micsbol.telecon4esp32.domain.model.protocolPrefix
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationProtocolModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.ObserveEntitlementUseCase
import com.micsbol.telecon4esp32.ui.greenhouse.formatDurationAgo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmartHomeViewModel @Inject constructor(
    private val remoteController: RemoteController,
    getApplicationProtocolMode: GetApplicationProtocolModeUseCase,
    observeEntitlement: ObserveEntitlementUseCase,
) : ViewModel() {

    private val appPrefix = ApplicationId.SMART_HOME.protocolPrefix()

    private val storedProtocolMode = getApplicationProtocolMode(ApplicationId.SMART_HOME)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BluetoothProtocolMode.defaultFor(ApplicationId.SMART_HOME),
        )

    private val entitlement: StateFlow<Entitlement> = observeEntitlement()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Entitlement.Free,
        )

    val protocolMode: StateFlow<BluetoothProtocolMode> = combine(
        storedProtocolMode,
        entitlement,
    ) { stored, access ->
        access.effectiveProtocolMode(ApplicationId.SMART_HOME, stored)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BluetoothProtocolMode.defaultFor(ApplicationId.SMART_HOME),
    )

    private val _uiState = MutableStateFlow(SmartHomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        remoteController.isConnected
            .onEach { connected ->
                _uiState.update { current ->
                    current.copy(isOnline = connected || current.lastTelemetryAtMs > 0L)
                }
            }
            .launchIn(viewModelScope)

        remoteController.messages
            .onEach { message ->
                if (message.app == appPrefix && message.type == "DATA") {
                    applyTelemetry(message.values)
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                refreshRelativeTimestamps()
            }
        }
    }

    fun toggleRoomDevice(roomId: String, deviceId: String) {
        val room = _uiState.value.rooms.firstOrNull { it.id == roomId } ?: return
        val device = room.devices.firstOrNull { it.id == deviceId && it.isControllable } ?: return
        val nextOn = !device.isOn
        val roomIndex = SmartHomeProtocol.roomIndex(roomId)
        val deviceByte = SmartHomeProtocol.deviceIdByte(deviceId)
        if (roomIndex < 0 || deviceByte == 0) return

        _uiState.update { state ->
            state.copy(
                rooms = state.rooms.map { candidate ->
                    if (candidate.id == roomId) candidate.withToggledDevice(deviceId) else candidate
                },
            )
        }

        sendSet(
            simple = mapOf(
                "room" to roomId,
                "device" to deviceId,
                "state" to if (nextOn) 1 else 0,
            ),
            advanced = mapOf(
                "room_id" to roomIndex,
                "device_id" to deviceByte,
                "state" to if (nextOn) 1 else 0,
            ),
        )
    }

    /** @deprecated Prefer [toggleRoomDevice]; kept for call-site clarity on light chips. */
    fun toggleRoomLight(roomId: String) {
        toggleRoomDevice(roomId, SmartHomeProtocol.DEVICE_LIGHT)
    }

    fun applySceneAllLightsOff() {
        _uiState.update { state ->
            state.copy(
                rooms = state.rooms.map { room ->
                    room.withAllControllable(on = false) { it.isLight }
                },
            )
        }
        sendScene(SmartHomeProtocol.SCENE_ALL_LIGHTS_OFF)
    }

    fun applySceneAway() {
        _uiState.update { state ->
            state.copy(
                rooms = state.rooms.map { room ->
                    room.withAllControllable(on = false) { device ->
                        device.isLight ||
                            device.id == SmartHomeProtocol.DEVICE_AMBIENCE ||
                            device.id == SmartHomeProtocol.DEVICE_OUTLET ||
                            device.id == SmartHomeProtocol.DEVICE_APPLIANCE ||
                            device.id == SmartHomeProtocol.DEVICE_WATER
                    }.let { updated ->
                        if (updated.id == SmartHomeProtocol.ROOM_GARAGE) {
                            updated.withDeviceStates(mapOf(SmartHomeProtocol.DEVICE_LOCK to true))
                        } else {
                            updated
                        }
                    }
                },
            )
        }
        sendScene(SmartHomeProtocol.SCENE_AWAY)
    }

    fun requestRefresh() {
        sendSet(
            simple = mapOf("refresh" to 1),
            advanced = mapOf("refresh" to 1),
        )
    }

    private fun sendScene(scene: String) {
        sendSet(
            simple = mapOf("scene" to scene),
            advanced = mapOf(
                "scene" to when (scene) {
                    SmartHomeProtocol.SCENE_AWAY -> ShBinaryProtocol.SCENE_AWAY
                    else -> ShBinaryProtocol.SCENE_ALL_LIGHTS_OFF
                },
            ),
        )
    }

    private fun sendSet(
        simple: Map<String, Any>,
        advanced: Map<String, Any> = simple,
    ) {
        viewModelScope.launch {
            if (!remoteController.isConnected.value) return@launch
            when (protocolMode.value) {
                BluetoothProtocolMode.SIMPLE ->
                    remoteController.sendLine(SimpleProtocolEncoder.buildSetLine(appPrefix, simple))
                BluetoothProtocolMode.ADVANCED ->
                    remoteController.sendData(ShPacketEncoder.buildSetPacket(advanced))
            }
        }
    }

    private fun applyTelemetry(values: Map<String, String>) {
        val now = System.currentTimeMillis()
        _uiState.update { current ->
            val rooms = applyRoomTelemetry(current.rooms, values)
            val systems = applySystemTelemetry(current.systems, values)
            val energyChart = applyEnergyTelemetry(current.energyChart, values)
            val events = parseEvents(values) ?: current.recentEvents
            val statusCode = values["status"]?.toIntOrNull()
            val allNormal = when (statusCode) {
                null -> rooms.none { it.statusBadge == RoomStatusBadge.OPEN } &&
                    values["security_status"]?.toIntOrNull() != 1
                0 -> true
                else -> false
            }

            current.copy(
                isOnline = true,
                lastTelemetryAtMs = now,
                updatedAgo = formatDurationAgo(now, now),
                deviceId = values["device"] ?: current.deviceId,
                allSystemsNormal = allNormal,
                rooms = rooms,
                systems = systems,
                energyChart = energyChart,
                recentEvents = events,
            )
        }
    }

    private fun applyRoomTelemetry(
        rooms: List<RoomUiModel>,
        values: Map<String, String>,
    ): List<RoomUiModel> {
        return rooms.map { room ->
            var next = room
            val prefix = room.id
            val deviceStates = buildMap {
                room.devices.filter { it.isControllable }.forEach { device ->
                    values["${prefix}_${device.id}"]?.toBooleanLike()?.let { put(device.id, it) }
                }
            }
            if (deviceStates.isNotEmpty()) {
                next = next.withDeviceStates(deviceStates)
            }

            values["${prefix}_temp"]?.toFloatOrNull()?.let { temp ->
                next = next.copy(temperatureC = temp)
            }

            val alert = values["${prefix}_alert"]
            next = when (alert?.lowercase()) {
                null, "", "none" -> next.copy(
                    statusBadge = when {
                        next.onCount > 0 -> RoomStatusBadge.ON_COUNT
                        else -> RoomStatusBadge.OFF
                    },
                    alertTextRes = null,
                )
                "window" -> next.copy(
                    statusBadge = RoomStatusBadge.OPEN,
                    alertTextRes = R.string.smart_home_room_window_open,
                )
                "door" -> next.copy(
                    statusBadge = RoomStatusBadge.OPEN,
                    alertTextRes = R.string.smart_home_room_door_open,
                )
                else -> next
            }

            values["${prefix}_on"]?.toIntOrNull()?.let { onCount ->
                val total = values["${prefix}_total"]?.toIntOrNull() ?: next.totalDeviceCount
                if (deviceStates.isEmpty()) {
                    next = next.copy(
                        onCount = onCount.coerceIn(0, total.coerceAtLeast(0)),
                        totalDeviceCount = total.coerceAtLeast(next.totalDeviceCount),
                        statusBadge = when {
                            next.statusBadge == RoomStatusBadge.OPEN -> RoomStatusBadge.OPEN
                            onCount > 0 -> RoomStatusBadge.ON_COUNT
                            else -> RoomStatusBadge.OFF
                        },
                    )
                }
            }
            next
        }
    }

    private fun applySystemTelemetry(
        systems: List<SystemTileUiModel>,
        values: Map<String, String>,
    ): List<SystemTileUiModel> {
        return systems.map { system ->
            when (system.id) {
                "climate" -> {
                    val temp = values["climate_temp"]?.toFloatOrNull()
                    val statusCode = values["climate_status"]?.toIntOrNull()
                    system.copy(
                        value = temp?.let { String.format("%.0f°C", it) } ?: system.value,
                        statusRes = climateStatusRes(statusCode) ?: system.statusRes,
                        statusColor = when (statusCode) {
                            1, 2 -> SmartHomeGlass.AccentWarm
                            else -> SmartHomeGlass.AccentGreenBright
                        },
                        iconTint = when (statusCode) {
                            1, 2 -> SmartHomeGlass.AccentWarm
                            else -> SmartHomeGlass.AccentGreenBright
                        },
                    )
                }
                "energy" -> {
                    val kw = values["energy_kw"]?.toFloatOrNull()
                        ?: values["power_kw"]?.toFloatOrNull()
                    system.copy(
                        value = kw?.let { String.format("%.1f kW", it) } ?: system.value,
                    )
                }
                "security" -> {
                    val code = values["security_status"]?.toIntOrNull()
                    system.copy(
                        statusRes = when (code) {
                            1 -> R.string.smart_home_system_security_open
                            0 -> R.string.smart_home_system_security_status
                            else -> system.statusRes
                        },
                        statusColor = when (code) {
                            1 -> SmartHomeGlass.AccentOrange
                            else -> SmartHomeGlass.AccentGreenBright
                        },
                        iconTint = when (code) {
                            1 -> SmartHomeGlass.AccentOrange
                            else -> SmartHomeGlass.AccentGreenBright
                        },
                    )
                }
                "water" -> {
                    val liters = values["water_l"]?.toIntOrNull()
                    system.copy(
                        value = liters?.let { "$it L" } ?: system.value,
                    )
                }
                else -> system
            }
        }
    }

    private fun applyEnergyTelemetry(
        current: EnergyChartData,
        values: Map<String, String>,
    ): EnergyChartData {
        val powerKw = values["power_kw"]?.toFloatOrNull()
            ?: values["energy_kw"]?.toFloatOrNull()
        val history = values["hist_power"]?.parseFloatSeries()
        return when {
            history != null -> current.copy(
                powerSeries = history,
                currentPowerKw = powerKw ?: history.lastOrNull() ?: current.currentPowerKw,
            )
            powerKw != null -> current.copy(currentPowerKw = powerKw)
            else -> current
        }
    }

    private fun parseEvents(values: Map<String, String>): List<RecentEventUiModel>? {
        val parsed = (0 until 5).mapNotNull { index ->
            val raw = values["event$index"] ?: return@mapNotNull null
            val code: String
            val time: String
            if ('|' in raw) {
                code = raw.substringBefore('|').trim()
                time = raw.substringAfter('|').trim()
            } else {
                code = raw.trim()
                time = values["event${index}_t"]
                    ?: values["event${index}_time"]
                    ?: return@mapNotNull null
            }
            if (code.isEmpty() || time.isEmpty()) return@mapNotNull null
            eventFromCode(code, time)
        }
        return parsed.takeIf { it.isNotEmpty() }
    }

    private fun eventFromCode(rawCode: String, time: String): RecentEventUiModel? {
        val code = rawCode.trim().lowercase()
        val displayTime = time.ifBlank { "—" }
        return when (code) {
            "door_locked" -> RecentEventUiModel(
                id = "door_locked",
                titleRes = R.string.smart_home_event_front_door_locked,
                time = displayTime,
                icon = Icons.Default.Lock,
                iconTint = SmartHomeGlass.AccentGreenBright,
            )
            "light_on" -> RecentEventUiModel(
                id = "light_on",
                titleRes = R.string.smart_home_event_living_room_light_on,
                time = displayTime,
                icon = Icons.Default.Lightbulb,
                iconTint = SmartHomeGlass.AccentWarm,
            )
            "motion" -> RecentEventUiModel(
                id = "motion",
                titleRes = R.string.smart_home_event_motion_driveway,
                time = displayTime,
                icon = Icons.Default.MotionPhotosAuto,
                iconTint = SmartHomeGlass.AccentGreenBright,
            )
            "windows" -> RecentEventUiModel(
                id = "windows",
                titleRes = R.string.smart_home_event_windows_living_room,
                time = displayTime,
                icon = Icons.Default.Window,
                iconTint = SmartHomeGlass.AccentOrange,
            )
            "water" -> RecentEventUiModel(
                id = "water",
                titleRes = R.string.smart_home_event_water_usage,
                time = displayTime,
                icon = Icons.Default.WaterDrop,
                iconTint = SmartHomeGlass.AccentWarm,
            )
            else -> null
        }
    }

    @StringRes
    private fun climateStatusRes(code: Int?): Int? = when (code) {
        0 -> R.string.smart_home_system_climate_status
        1 -> R.string.smart_home_system_climate_heating
        2 -> R.string.smart_home_system_climate_cooling
        else -> null
    }

    private fun refreshRelativeTimestamps() {
        val now = System.currentTimeMillis()
        _uiState.update { current ->
            current.copy(
                updatedAgo = formatDurationAgo(now, current.lastTelemetryAtMs),
                isOnline = remoteController.isConnected.value ||
                    (current.lastTelemetryAtMs > 0L && now - current.lastTelemetryAtMs < 15_000L),
            )
        }
    }
}

private fun String.toBooleanLike(): Boolean = when (lowercase()) {
    "1", "true", "on", "yes" -> true
    "0", "false", "off", "no" -> false
    else -> toIntOrNull()?.let { it != 0 } ?: false
}

private fun String.parseFloatSeries(): List<Float>? =
    split("|")
        .mapNotNull { it.trim().toFloatOrNull() }
        .takeIf { it.isNotEmpty() }
