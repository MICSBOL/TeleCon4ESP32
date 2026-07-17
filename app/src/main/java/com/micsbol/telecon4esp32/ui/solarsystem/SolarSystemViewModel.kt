package com.micsbol.telecon4esp32.ui.solarsystem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteController
import com.micsbol.telecon4esp32.domain.bluetooth.SimpleProtocolEncoder
import com.micsbol.telecon4esp32.domain.bluetooth.sp.SpPacketEncoder
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Entitlement
import com.micsbol.telecon4esp32.domain.model.effectiveProtocolMode
import com.micsbol.telecon4esp32.domain.model.protocolPrefix
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationProtocolModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.ObserveEntitlementUseCase
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
class SolarSystemViewModel @Inject constructor(
    private val remoteController: RemoteController,
    getApplicationProtocolMode: GetApplicationProtocolModeUseCase,
    observeEntitlement: ObserveEntitlementUseCase,
) : ViewModel() {

    private val appPrefix = ApplicationId.SOLAR_POWER.protocolPrefix()

    private val storedProtocolMode = getApplicationProtocolMode(ApplicationId.SOLAR_POWER)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BluetoothProtocolMode.defaultFor(ApplicationId.SOLAR_POWER),
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
        access.effectiveProtocolMode(ApplicationId.SOLAR_POWER, stored)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BluetoothProtocolMode.defaultFor(ApplicationId.SOLAR_POWER),
    )

    private val _uiState = MutableStateFlow(SolarSystemUiState())
    val uiState = _uiState.asStateFlow()

    init {
        remoteController.isConnected
            .onEach { connected ->
                _uiState.update { current ->
                    current.copy(
                        isConnected = connected,
                        isOnline = if (connected) current.isOnline else false,
                    )
                }
                if (connected) {
                    refreshTelemetry()
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
                delay(30_000)
                refreshRelativeTimestamps()
            }
        }
    }

    fun refreshTelemetry() {
        sendSet(mapOf("refresh" to 1))
    }

    fun selectChartPeriod(period: SolarChartPeriod) {
        _uiState.update { current ->
            val labels = ConsumptionChartData.labelsFor(period)
            val barsPerGroup = ConsumptionChartData.barsPerGroupFor(period)
            val expectedBars = labels.size * barsPerGroup
            val nextChart = if (current.isOnline) {
                ConsumptionChartData(
                    groupLabels = labels,
                    producedSeries = expandChartSeries(
                        series = current.consumptionChart.producedSeries,
                        expectedBars = expectedBars,
                        barsPerGroup = barsPerGroup,
                    ),
                    consumedSeries = expandChartSeries(
                        series = current.consumptionChart.consumedSeries,
                        expectedBars = expectedBars,
                        barsPerGroup = barsPerGroup,
                    ),
                    barsPerGroup = barsPerGroup,
                )
            } else {
                ConsumptionChartData.mock(period)
            }
            current.copy(
                chartPeriod = period,
                consumptionChart = nextChart,
            )
        }
        sendSet(mapOf("period" to period.wireValue))
    }

    fun toggleInverter() {
        val next = !_uiState.value.diy.inverterOn
        _uiState.update { current ->
            current.copy(diy = current.diy.copy(inverterOn = next))
        }
        sendSet(mapOf("inverter" to if (next) 1 else 0))
    }

    fun resetDailyCounters() {
        sendSet(mapOf("reset_day" to 1))
    }

    private fun applyTelemetry(values: Map<String, String>) {
        val now = System.currentTimeMillis()
        _uiState.update { current ->
            val live = current.live.copy(
                solarW = values["solar_w"]?.toIntOrNull() ?: current.live.solarW,
                loadW = values["load_w"]?.toIntOrNull() ?: current.live.loadW,
                batteryW = values["batt_w"]?.toIntOrNull() ?: current.live.batteryW,
                gridW = values["grid_w"]?.toIntOrNull() ?: current.live.gridW,
                batteryPercent = values["batt_pct"]?.toIntOrNull() ?: current.live.batteryPercent,
                voltage = values["volt"]?.toFloatOrNull() ?: current.live.voltage,
                current = values["amp"]?.toFloatOrNull() ?: current.live.current,
            )
            val totals = current.totals.copy(
                todayKwh = values["today_kwh"]?.toFloatOrNull() ?: current.totals.todayKwh,
                monthKwh = values["month_kwh"]?.toFloatOrNull() ?: current.totals.monthKwh,
                totalKwh = values["total_kwh"]?.toFloatOrNull() ?: current.totals.totalKwh,
                consumedWeekKwh = values["cons_week_kwh"]?.toFloatOrNull()
                    ?: current.totals.consumedWeekKwh,
                consumedDayKwh = values["cons_day_kwh"]?.toFloatOrNull()
                    ?: current.totals.consumedDayKwh,
            )
            val breakdown = current.breakdown.copy(
                producedKwh = values["prod_kwh"]?.toFloatOrNull() ?: current.breakdown.producedKwh,
                exportedKwh = values["export_kwh"]?.toFloatOrNull() ?: current.breakdown.exportedKwh,
                batteryUsedKwh = values["batt_used_kwh"]?.toFloatOrNull()
                    ?: current.breakdown.batteryUsedKwh,
            )
            val distribution = current.distribution.copy(
                toHomeKwh = values["to_home_kwh"]?.toFloatOrNull() ?: current.distribution.toHomeKwh,
                toBatteryKwh = values["to_batt_kwh"]?.toFloatOrNull() ?: current.distribution.toBatteryKwh,
                toGridKwh = values["to_grid_kwh"]?.toFloatOrNull() ?: current.distribution.toGridKwh,
            )
            val batteryInfo = current.batteryInfo.copy(
                capacityKwh = values["batt_cap_kwh"]?.toFloatOrNull()
                    ?: current.batteryInfo.capacityKwh,
                chargeEtaMinutes = values["charge_eta_min"]?.toIntOrNull()
                    ?: current.batteryInfo.chargeEtaMinutes,
                totalChargedKwh = values["total_charge_kwh"]?.toFloatOrNull()
                    ?: current.batteryInfo.totalChargedKwh,
                lowBatteryThreshold = values["low_batt_pct"]?.toIntOrNull()
                    ?: current.batteryInfo.lowBatteryThreshold,
            )
            val diy = current.diy.copy(
                inverterOn = values["inverter"]?.toBooleanLike() ?: current.diy.inverterOn,
                gridMode = values["grid_mode"]?.toIntOrNull()?.toGridModeFromWire()
                    ?: live.gridW.deriveGridModeFromWatts(),
                panelEfficiencyPercent = values["panel_eff"]?.toIntOrNull()
                    ?: current.diy.panelEfficiencyPercent,
                maxSolarW = values["max_solar_w"]?.toIntOrNull() ?: current.diy.maxSolarW,
                faultCode = values["fault"]?.toIntOrNull() ?: current.diy.faultCode,
            )
            current.copy(
                isOnline = true,
                isConnected = remoteController.isConnected.value,
                deviceId = values["device"] ?: current.deviceId,
                panelName = values["panel_name"] ?: current.panelName,
                panelCount = values["panels"]?.toIntOrNull() ?: current.panelCount,
                status = values["status"]?.toIntOrNull()?.toSolarStatus() ?: current.status,
                lastTelemetryAtMs = now,
                updatedAgo = formatDurationAgo(now, now),
                live = live,
                totals = totals,
                breakdown = breakdown,
                distribution = distribution,
                batteryInfo = batteryInfo,
                diy = diy,
                consumptionChart = parseConsumptionHistory(
                    values = values,
                    period = current.chartPeriod,
                    current = current.consumptionChart,
                ),
                productionChart = parseProductionHistory(values, current.productionChart),
            )
        }
    }

    private fun refreshRelativeTimestamps() {
        val now = System.currentTimeMillis()
        _uiState.update { current ->
            current.copy(
                updatedAgo = formatDurationAgo(now, current.lastTelemetryAtMs),
            )
        }
    }

    private fun parseConsumptionHistory(
        values: Map<String, String>,
        period: SolarChartPeriod,
        current: ConsumptionChartData,
    ): ConsumptionChartData {
        val produced = values["hist_prod"]?.parseFloatSeries()
        val home = values["hist_cons_home"]?.parseFloatSeries()
        val battery = values["hist_cons_batt"]?.parseFloatSeries()
        val grid = values["hist_cons_grid"]?.parseFloatSeries()
        val consumedDirect = values["hist_cons"]?.parseFloatSeries()
        val consumed = consumedDirect ?: sumConsumptionSeries(home, battery, grid)

        if (produced == null && consumed == null) return current

        val labels = ConsumptionChartData.labelsFor(period)
        val barsPerGroup = ConsumptionChartData.barsPerGroupFor(period)
        val expectedBars = labels.size * barsPerGroup

        return ConsumptionChartData(
            groupLabels = labels,
            producedSeries = expandChartSeries(
                series = produced ?: current.producedSeries,
                expectedBars = expectedBars,
                barsPerGroup = barsPerGroup,
            ),
            consumedSeries = expandChartSeries(
                series = consumed ?: current.consumedSeries,
                expectedBars = expectedBars,
                barsPerGroup = barsPerGroup,
            ),
            barsPerGroup = barsPerGroup,
        )
    }

    private fun parseProductionHistory(
        values: Map<String, String>,
        current: ProductionChartData,
    ): ProductionChartData {
        val series = values["hist_prod"]?.parseFloatSeries() ?: return current
        return current.copy(powerSeries = series)
    }

    private fun sendSet(pairs: Map<String, Any>) {
        viewModelScope.launch {
            if (!remoteController.isConnected.value) return@launch
            when (protocolMode.value) {
                BluetoothProtocolMode.SIMPLE ->
                    remoteController.sendLine(SimpleProtocolEncoder.buildSetLine(appPrefix, pairs))
                BluetoothProtocolMode.ADVANCED ->
                    remoteController.sendData(SpPacketEncoder.buildSetPacket(pairs))
            }
        }
    }
}

private val SolarChartPeriod.wireValue: Int
    get() = when (this) {
        SolarChartPeriod.DAY -> 1
        SolarChartPeriod.WEEK -> 7
    }

private fun sumConsumptionSeries(vararg series: List<Float>?): List<Float>? {
    val present = series.filterNotNull()
    if (present.isEmpty()) return null
    val size = present.maxOf { it.size }
    return List(size) { index ->
        present.sumOf { it.getOrElse(index) { 0f }.toDouble() }.toFloat()
    }
}

private fun expandChartSeries(
    series: List<Float>,
    expectedBars: Int,
    barsPerGroup: Int,
): List<Float> {
    if (series.isEmpty()) return List(expectedBars) { 0f }
    if (series.size == expectedBars) return series
    if (series.size > expectedBars) return series.take(expectedBars)

    val groupCount = expectedBars / barsPerGroup
    if (series.size == groupCount) {
        val weights = subDailyWeights(barsPerGroup)
        val weightSum = weights.sum()
        return series.flatMap { groupTotal ->
            weights.map { groupTotal * it / weightSum }
        }
    }

    return List(expectedBars) { index ->
        series.getOrElse(index % series.size) { 0f }
    }
}

private fun subDailyWeights(barsPerGroup: Int): List<Float> = when (barsPerGroup) {
    3 -> listOf(0.75f, 1.25f, 0.90f)
    6 -> listOf(0.5f, 0.7f, 0.9f, 1.2f, 1.0f, 0.8f)
    else -> List(barsPerGroup) { 1f }
}

private fun Int.toSolarStatus(): SolarSystemStatus = when (this) {
    1 -> SolarSystemStatus.FAULT
    else -> SolarSystemStatus.NORMAL
}

private fun Int.toGridModeFromWire(): SolarGridMode = when (this) {
    1 -> SolarGridMode.EXPORT
    2 -> SolarGridMode.IMPORT
    else -> SolarGridMode.IDLE
}

private fun Int.deriveGridModeFromWatts(): SolarGridMode = when {
    this > 20 -> SolarGridMode.EXPORT
    this < -20 -> SolarGridMode.IMPORT
    else -> SolarGridMode.IDLE
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

private fun formatDurationAgo(nowMs: Long, eventMs: Long): String {
    if (eventMs <= 0L) return ""
    val seconds = ((nowMs - eventMs) / 1000).coerceAtLeast(0)
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m"
        else -> "${seconds / 3600}h"
    }
}
