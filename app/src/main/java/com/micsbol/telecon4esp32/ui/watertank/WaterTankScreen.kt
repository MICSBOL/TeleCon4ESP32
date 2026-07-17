package com.micsbol.telecon4esp32.ui.watertank

import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.applications.applicationSettingsTitleRes
import com.micsbol.telecon4esp32.ui.applications.navigateToApplicationSettings
import com.micsbol.telecon4esp32.ui.bluetooth.ApplicationBluetoothTopBarButton
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme
import com.micsbol.telecon4esp32.ui.watertank.components.CurrentLevelCard
import com.micsbol.telecon4esp32.ui.watertank.components.TankLevelChart
import com.micsbol.telecon4esp32.ui.watertank.components.TimeRangeSelector
import com.micsbol.telecon4esp32.ui.watertank.components.WaterTankFooter
import com.micsbol.telecon4esp32.ui.watertank.components.WaterTankMetricRow
import com.micsbol.telecon4esp32.ui.watertank.components.WaterTankStatusRow
import com.micsbol.telecon4esp32.ui.watertank.components.WaterTankTopBar

@Composable
fun WaterTankScreen(
    navController: NavController,
    viewModel: WaterTankViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    WaterTankScreenContent(
        uiState = uiState,
        onMenuClick = { navController.navigateUp() },
        onRefreshClick = { /* mock refresh */ },
        onPeriodSelected = viewModel::selectChartPeriod,
        topBarActions = {
            Row(
                modifier = Modifier.padding(start = 4.dp, end = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ApplicationBluetoothTopBarButton(accent = WaterTankGlass.AccentCyan) { onClick, enabled, content ->
                    WaterTankGlassIconButton(onClick = onClick, enabled = enabled, content = content)
                }
                WaterTankGlassIconButton(
                    onClick = { navController.navigate(Screen.WaterTankHelp.route) },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = stringResource(R.string.water_tank_help_content_description),
                        tint = WaterTankGlass.AccentCyan,
                        modifier = Modifier.size(22.dp),
                    )
                }
                WaterTankGlassIconButton(
                    onClick = { navController.navigateToApplicationSettings(ApplicationId.WATER_TANK) },
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(
                            R.string.applications_settings_content_description,
                            stringResource(applicationSettingsTitleRes(ApplicationId.WATER_TANK)),
                        ),
                        tint = WaterTankGlass.AccentCyan,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        },
    )
}

@Composable
fun WaterTankScreenContent(
    uiState: WaterTankUiState,
    onMenuClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onPeriodSelected: (TankChartPeriod) -> Unit,
    modifier: Modifier = Modifier,
    topBarActions: @Composable () -> Unit = {},
) {
    WaterTankBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            WaterTankTopBar(
                isConnected = uiState.isConnected,
                deviceId = uiState.deviceId,
                onMenuClick = onMenuClick,
                onRefreshClick = onRefreshClick,
                actions = topBarActions,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CurrentLevelCard(
                    levelPercent = uiState.levelPercent,
                    volumeLiters = uiState.volumeLiters,
                    capacityLiters = uiState.capacityLiters,
                )
                WaterTankMetricRow(
                    volumeLiters = uiState.volumeLiters,
                    capacityLiters = uiState.capacityLiters,
                    updatedAgo = uiState.updatedAgo,
                )
                WaterTankStatusRow(
                    tankStatus = uiState.tankStatus,
                    pumpOn = uiState.pumpOn,
                )
                TimeRangeSelector(
                    selectedPeriod = uiState.chartPeriod,
                    onPeriodSelected = onPeriodSelected,
                )
                TankLevelChart(chartData = uiState.chartData)
                WaterTankFooter(
                    isOnline = uiState.isOnline,
                    lastUpdatedTime = uiState.lastUpdatedTime,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "Water Tank Portrait Dark",
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun WaterTankScreenPortraitDarkPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        WaterTankScreenContent(
            uiState = WaterTankUiState(),
            onMenuClick = {},
            onRefreshClick = {},
            onPeriodSelected = {},
        )
    }
}

@Preview(
    showBackground = true,
    name = "Water Tank Portrait Light",
)
@Composable
private fun WaterTankScreenPortraitLightPreview() {
    TeleCon4Esp32Theme(darkTheme = false) {
        WaterTankScreenContent(
            uiState = WaterTankUiState(),
            onMenuClick = {},
            onRefreshClick = {},
            onPeriodSelected = {},
        )
    }
}

@Preview(
    showBackground = true,
    name = "Water Tank Landscape Dark",
    device = "spec:width=1280dp,height=800dp,dpi=160,orientation=landscape",
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun WaterTankScreenLandscapeDarkPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        WaterTankScreenContent(
            uiState = WaterTankUiState(),
            onMenuClick = {},
            onRefreshClick = {},
            onPeriodSelected = {},
        )
    }
}
