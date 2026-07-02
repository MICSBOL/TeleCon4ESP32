package com.micsbol.telecon4esp32.ui.solarsystem

import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.applications.ApplicationSettingsIconButton
import com.micsbol.telecon4esp32.ui.cyber.components.CyberBackground
import com.micsbol.telecon4esp32.ui.solarsystem.components.DailyEnergyCard
import com.micsbol.telecon4esp32.ui.solarsystem.components.LivePowerFlow
import com.micsbol.telecon4esp32.ui.solarsystem.components.ProductionChart
import com.micsbol.telecon4esp32.ui.solarsystem.components.SolarMetricGrid
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
        topBarActions = {
            ApplicationSettingsIconButton(
                applicationId = ApplicationId.SOLAR_POWER,
                navController = navController,
            )
        },
    )
}

@Composable
fun SolarSystemScreenContent(
    uiState: SolarSystemUiState,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    topBarActions: @Composable () -> Unit = {},
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        CyberBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            SolarSystemTopBar(
                energyBalanceW = uiState.energyBalanceW,
                onMenuClick = onMenuClick,
                actions = topBarActions,
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                val useWideLayout = isLandscape && maxWidth >= 600.dp

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (useWideLayout) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            LivePowerFlow(
                                powerFlow = uiState.powerFlow,
                                modifier = Modifier.weight(1.2f),
                            )
                            SolarMetricGrid(
                                metrics = uiState.metrics,
                                modifier = Modifier.weight(0.8f),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ProductionChart(
                                chartData = uiState.productionChart,
                                modifier = Modifier.weight(2f),
                            )
                            DailyEnergyCard(
                                dailyEnergyKwh = uiState.dailyEnergyKwh,
                                progress = uiState.dailyEnergyProgress,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        LivePowerFlow(powerFlow = uiState.powerFlow)
                        SolarMetricGrid(metrics = uiState.metrics)
                        ProductionChart(chartData = uiState.productionChart)
                        DailyEnergyCard(
                            dailyEnergyKwh = uiState.dailyEnergyKwh,
                            progress = uiState.dailyEnergyProgress,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "Solar System Portrait Dark",
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun SolarSystemScreenPortraitDarkPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        SolarSystemScreenContent(
            uiState = SolarSystemUiState(),
            onMenuClick = {},
        )
    }
}

@Preview(
    showBackground = true,
    name = "Solar System Portrait Light",
)
@Composable
private fun SolarSystemScreenPortraitLightPreview() {
    TeleCon4Esp32Theme(darkTheme = false) {
        SolarSystemScreenContent(
            uiState = SolarSystemUiState(),
            onMenuClick = {},
        )
    }
}

@Preview(
    showBackground = true,
    name = "Solar System Landscape Dark",
    device = "spec:width=1280dp,height=800dp,dpi=160,orientation=landscape",
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun SolarSystemScreenLandscapeDarkPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        SolarSystemScreenContent(
            uiState = SolarSystemUiState(),
            onMenuClick = {},
        )
    }
}
