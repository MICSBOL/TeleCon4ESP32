package com.micsbol.telecon4esp32.ui.greenhouse

import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalConfiguration
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.applications.ApplicationSettingsIconButton
import com.micsbol.telecon4esp32.ui.greenhouse.components.EnvironmentalChart
import com.micsbol.telecon4esp32.ui.greenhouse.components.GreenhouseControlPanel
import com.micsbol.telecon4esp32.ui.greenhouse.components.GreenhouseTopBar
import com.micsbol.telecon4esp32.ui.greenhouse.components.MainStatusCard
import com.micsbol.telecon4esp32.ui.greenhouse.components.SensorDataGrid
import com.micsbol.telecon4esp32.ui.theme.TechBlueBright
import com.micsbol.telecon4esp32.ui.theme.TechCyanBright
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme

private const val GreenhouseIllustrationAspectRatio = 368f / 331f

@Composable
fun GreenhouseIllustration(
    modifier: Modifier = Modifier,
    width: Dp,
    glowScale: Float = 1.18f,
) {
    val height = width / GreenhouseIllustrationAspectRatio
    val glowSize = maxOf(width, height) * glowScale

    Box(
        modifier = modifier.size(glowSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to TechCyanBright.copy(alpha = 0.28f),
                            0.42f to TechBlueBright.copy(alpha = 0.12f),
                            0.72f to TechBlueBright.copy(alpha = 0.04f),
                            1f to Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )
        Image(
            painter = painterResource(R.drawable.greenhouse_illustration_03),
            contentDescription = stringResource(R.string.greenhouse_illustration_content_description),
            modifier = Modifier.size(width = width, height = height),
            contentScale = ContentScale.Fit,
        )
    }
}

@Preview(
    showBackground = true,
    name = "Greenhouse Illustration",
)
@Composable
private fun GreenhouseIllustrationPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            GreenhouseIllustration(width = 148.dp)
        }
    }
}

@Composable
fun GreenhouseScreen(
    navController: NavController,
    viewModel: GreenhouseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    GreenhouseScreenContent(
        uiState = uiState,
        onMenuClick = { navController.navigateUp() },
        onFanToggle = viewModel::toggleFan,
        onHeaterToggle = viewModel::toggleHeater,
        onPumpToggle = viewModel::togglePump,
        topBarActions = {
            ApplicationSettingsIconButton(
                applicationId = ApplicationId.GREENHOUSE,
                navController = navController,
            )
        },
    )
}

@Composable
fun GreenhouseScreenContent(
    uiState: GreenhouseUiState,
    onMenuClick: () -> Unit,
    onFanToggle: () -> Unit,
    onHeaterToggle: () -> Unit,
    onPumpToggle: () -> Unit,
    modifier: Modifier = Modifier,
    topBarActions: @Composable () -> Unit = {},
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        GreenhouseTopBar(
            isOnline = uiState.isOnline,
            isAutoMode = uiState.isAutoMode,
            deviceId = uiState.deviceId,
            updatedAgo = uiState.updatedAgo,
            onMenuClick = onMenuClick,
            actions = topBarActions,
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            val useWideLayout = isLandscape && maxWidth >= 600.dp
            val scrollState = rememberScrollState()

            if (useWideLayout) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(0.4f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            MainStatusCard(
                                temperatureC = uiState.temperatureC,
                                humidityPercent = uiState.humidityPercent,
                                vpdKpa = uiState.vpdKpa,
                                isStable = uiState.isStable,
                            )
                            SensorDataGrid(
                                soilPercent = uiState.soilPercent,
                                soilTargetMin = uiState.soilTargetMin,
                                soilTargetMax = uiState.soilTargetMax,
                                lightLux = uiState.lightLux,
                                lightsOn = uiState.lightsOn,
                                deltaTempC = uiState.deltaTempC,
                                ventOpenPercent = uiState.ventOpenPercent,
                                tankPercent = uiState.tankPercent,
                                lastIrrigationAgo = uiState.lastIrrigationAgo,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(0.6f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            EnvironmentalChart(chartData = uiState.chartData)
                            GreenhouseControlPanel(
                                fanOn = uiState.fanOn,
                                heaterOn = uiState.heaterOn,
                                pumpOn = uiState.pumpOn,
                                targetTempC = uiState.targetTempC,
                                targetHumidityPercent = uiState.targetHumidityPercent,
                                onFanToggle = onFanToggle,
                                onHeaterToggle = onHeaterToggle,
                                onPumpToggle = onPumpToggle,
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MainStatusCard(
                        temperatureC = uiState.temperatureC,
                        humidityPercent = uiState.humidityPercent,
                        vpdKpa = uiState.vpdKpa,
                        isStable = uiState.isStable,
                    )
                    SensorDataGrid(
                        soilPercent = uiState.soilPercent,
                        soilTargetMin = uiState.soilTargetMin,
                        soilTargetMax = uiState.soilTargetMax,
                        lightLux = uiState.lightLux,
                        lightsOn = uiState.lightsOn,
                        deltaTempC = uiState.deltaTempC,
                        ventOpenPercent = uiState.ventOpenPercent,
                        tankPercent = uiState.tankPercent,
                        lastIrrigationAgo = uiState.lastIrrigationAgo,
                    )
                    EnvironmentalChart(chartData = uiState.chartData)
                    GreenhouseControlPanel(
                        fanOn = uiState.fanOn,
                        heaterOn = uiState.heaterOn,
                        pumpOn = uiState.pumpOn,
                        targetTempC = uiState.targetTempC,
                        targetHumidityPercent = uiState.targetHumidityPercent,
                        onFanToggle = onFanToggle,
                        onHeaterToggle = onHeaterToggle,
                        onPumpToggle = onPumpToggle,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "Greenhouse Portrait Dark",
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun GreenhouseScreenPortraitDarkPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        GreenhouseScreenContent(
            uiState = GreenhouseUiState(),
            onMenuClick = {},
            onFanToggle = {},
            onHeaterToggle = {},
            onPumpToggle = {},
        )
    }
}

@Preview(
    showBackground = true,
    name = "Greenhouse Portrait Light",
)
@Composable
private fun GreenhouseScreenPortraitLightPreview() {
    TeleCon4Esp32Theme(darkTheme = false) {
        GreenhouseScreenContent(
            uiState = GreenhouseUiState(),
            onMenuClick = {},
            onFanToggle = {},
            onHeaterToggle = {},
            onPumpToggle = {},
        )
    }
}

@Preview(
    showBackground = true,
    name = "Greenhouse Landscape Dark",
    device = "spec:width=1280dp,height=800dp,dpi=160,orientation=landscape",
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun GreenhouseScreenLandscapeDarkPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        GreenhouseScreenContent(
            uiState = GreenhouseUiState(),
            onMenuClick = {},
            onFanToggle = {},
            onHeaterToggle = {},
            onPumpToggle = {},
        )
    }
}
