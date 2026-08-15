package com.micsbol.telecon4esp32.ui.solarsystem

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.ui.components.rememberClampedSafeHudInsets
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.applications.applicationSettingsTitleRes
import com.micsbol.telecon4esp32.ui.bluetooth.ApplicationBluetoothTopBarButton
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.solarsystem.components.SolarBatterySummaryCard
import com.micsbol.telecon4esp32.ui.solarsystem.components.SolarConsumptionCard
import com.micsbol.telecon4esp32.ui.solarsystem.components.SolarDiyStatusCard
import com.micsbol.telecon4esp32.ui.solarsystem.components.SolarLivePowerRow
import com.micsbol.telecon4esp32.ui.solarsystem.components.SolarPanelOverviewCard
import com.micsbol.telecon4esp32.ui.solarsystem.components.SolarSystemTopBar
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme

@Composable
fun SolarSystemScreen(
    navController: NavController,
    viewModel: SolarSystemViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    SolarSystemScreenContent(
        uiState = uiState,
        onMenuClick = { navController.navigateUp() },
        onRefreshClick = viewModel::refreshTelemetry,
        onChartPeriodSelected = viewModel::selectChartPeriod,
        onInverterToggle = viewModel::toggleInverter,
        onResetDaily = viewModel::resetDailyCounters,
        topBarActions = {
            Row(
                modifier = Modifier.padding(start = 4.dp, end = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ApplicationBluetoothTopBarButton(accent = SolarGlass.AccentAmber) { onClick, enabled, content ->
                    SolarGlassIconButton(onClick = onClick, enabled = enabled, content = content)
                }
                SolarGlassIconButton(
                    onClick = { navController.navigate(Screen.SolarHelp.route) },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = stringResource(R.string.solar_system_help_content_description),
                        tint = SolarGlass.AccentAmber,
                        modifier = Modifier.size(22.dp),
                    )
                }
                SolarGlassIconButton(
                    onClick = {
                        navController.navigate(
                            Screen.ApplicationSettings.createRoute(ApplicationId.SOLAR_POWER),
                        )
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(
                            R.string.applications_settings_content_description,
                            stringResource(applicationSettingsTitleRes(ApplicationId.SOLAR_POWER)),
                        ),
                        tint = SolarGlass.AccentAmber,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        },
    )
}

@Composable
fun SolarSystemScreenContent(
    uiState: SolarSystemUiState,
    onMenuClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onChartPeriodSelected: (SolarChartPeriod) -> Unit,
    onInverterToggle: () -> Unit,
    onResetDaily: () -> Unit,
    modifier: Modifier = Modifier,
    topBarActions: @Composable () -> Unit = {},
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()
    val scrollEdgeInsets = rememberClampedSafeHudInsets(
        includeTop = true,
        includeBottom = false,
        includeHorizontal = true,
    )
    val contentBottomInsets = rememberClampedSafeHudInsets(
        includeTop = false,
        includeBottom = true,
        includeHorizontal = false,
    )

    SolarBackground(
        solarPowerW = uiState.live.solarW,
        maxSolarW = uiState.diy.maxSolarW,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .windowInsetsPadding(scrollEdgeInsets)
                    .windowInsetsPadding(contentBottomInsets)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SolarSystemTopBar(
                    isConnected = uiState.isConnected,
                    isOnline = uiState.isOnline,
                    deviceId = uiState.deviceId,
                    panelName = uiState.panelName,
                    updatedAgo = uiState.updatedAgo,
                    gridMode = uiState.diy.gridMode,
                    onMenuClick = onMenuClick,
                    onRefreshClick = onRefreshClick,
                    actions = topBarActions,
                )
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    val useWideLayout = isLandscape && maxWidth >= 600.dp

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (useWideLayout) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SolarPanelOverviewCard(
                                    live = uiState.live,
                                    totals = uiState.totals,
                                    modifier = Modifier.weight(1f),
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    SolarConsumptionCard(
                                        consumedKwh = when (uiState.chartPeriod) {
                                            SolarChartPeriod.DAY -> uiState.totals.consumedDayKwh
                                            SolarChartPeriod.WEEK -> uiState.totals.consumedWeekKwh
                                        },
                                        chartData = uiState.consumptionChart,
                                        breakdown = uiState.breakdown,
                                        chartPeriod = uiState.chartPeriod,
                                        onPeriodSelected = onChartPeriodSelected,
                                    )
                                    SolarBatterySummaryCard(
                                        batteryInfo = uiState.batteryInfo,
                                        live = uiState.live,
                                    )
                                }
                            }
                        } else {
                            SolarPanelOverviewCard(
                                live = uiState.live,
                                totals = uiState.totals,
                            )
                            SolarConsumptionCard(
                                consumedKwh = when (uiState.chartPeriod) {
                                    SolarChartPeriod.DAY -> uiState.totals.consumedDayKwh
                                    SolarChartPeriod.WEEK -> uiState.totals.consumedWeekKwh
                                },
                                chartData = uiState.consumptionChart,
                                breakdown = uiState.breakdown,
                                chartPeriod = uiState.chartPeriod,
                                onPeriodSelected = onChartPeriodSelected,
                            )
                            SolarBatterySummaryCard(
                                batteryInfo = uiState.batteryInfo,
                                live = uiState.live,
                            )
                        }

                        SolarDiyStatusCard(
                            diy = uiState.diy,
                            distribution = uiState.distribution,
                            lowBatteryWarning = uiState.lowBatteryWarning,
                            hasFault = uiState.hasFault,
                            onInverterToggle = onInverterToggle,
                            onResetDaily = onResetDaily,
                        )
                        SolarLivePowerRow(live = uiState.live)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Solar System Portrait")
@Composable
private fun SolarSystemScreenPortraitPreview() {
    TeleCon4Esp32Theme(darkTheme = false) {
        SolarSystemScreenContent(
            uiState = SolarSystemUiState(isOnline = true, updatedAgo = "2m"),
            onMenuClick = {},
            onRefreshClick = {},
            onChartPeriodSelected = {},
            onInverterToggle = {},
            onResetDaily = {},
        )
    }
}
