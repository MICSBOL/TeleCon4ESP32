package com.micsbol.telecon4esp32.ui.greenhouse

import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.applications.applicationSettingsTitleRes
import com.micsbol.telecon4esp32.ui.greenhouse.components.EnvironmentalChart
import com.micsbol.telecon4esp32.ui.greenhouse.components.GreenhouseControlPanel
import com.micsbol.telecon4esp32.ui.greenhouse.components.GreenhouseTopBar
import com.micsbol.telecon4esp32.ui.greenhouse.components.MainStatusCard
import com.micsbol.telecon4esp32.ui.greenhouse.components.SensorDataGrid
import com.micsbol.telecon4esp32.ui.navigation.Screen
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
                            0f to GreenhouseGlass.LeafBright.copy(alpha = 0.28f),
                            0.42f to GreenhouseGlass.AccentGreen.copy(alpha = 0.12f),
                            0.72f to GreenhouseGlass.ForestMid.copy(alpha = 0.04f),
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
        GreenhouseBackground {
            Box(
                modifier = Modifier.padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                GreenhouseIllustration(width = 148.dp)
            }
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
        onLightsToggle = viewModel::toggleLights,
        onAutoModeToggle = viewModel::toggleAutoMode,
        onVentChange = viewModel::updateVentOpenLocal,
        onVentChangeFinished = viewModel::commitVentOpen,
        onTargetTempChange = viewModel::updateTargetTempLocal,
        onTargetHumidityChange = viewModel::updateTargetHumidityLocal,
        onTargetClimateFinished = viewModel::commitTargetClimate,
        topBarActions = {
            Row(
                modifier = Modifier.padding(start = 4.dp, end = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GreenhouseGlassIconButton(
                    onClick = { navController.navigate(Screen.GreenhouseCamera.route) },
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = stringResource(R.string.greenhouse_camera_content_description),
                        tint = GreenhouseGlass.AccentGreen,
                        modifier = Modifier.size(22.dp),
                    )
                }
                GreenhouseGlassIconButton(
                    onClick = { navController.navigate(Screen.GreenhouseHelp.route) },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = stringResource(R.string.greenhouse_help_content_description),
                        tint = GreenhouseGlass.AccentGreen,
                        modifier = Modifier.size(22.dp),
                    )
                }
                GreenhouseGlassIconButton(
                    onClick = { navController.navigate(Screen.GreenhouseSettings.route) },
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(
                            R.string.applications_settings_content_description,
                            stringResource(applicationSettingsTitleRes(ApplicationId.GREENHOUSE)),
                        ),
                        tint = GreenhouseGlass.AccentGreen,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
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
    onLightsToggle: () -> Unit,
    onAutoModeToggle: () -> Unit,
    onVentChange: (Int) -> Unit,
    onVentChangeFinished: () -> Unit,
    onTargetTempChange: (Int) -> Unit,
    onTargetHumidityChange: (Int) -> Unit,
    onTargetClimateFinished: () -> Unit,
    modifier: Modifier = Modifier,
    topBarActions: @Composable () -> Unit = {},
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val useWideLayout = isLandscape && configuration.screenWidthDp >= 600
    val scrollState = rememberScrollState()
    val scrollEdgeInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
    )
    val contentBottomInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)

    GreenhouseBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (useWideLayout) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .windowInsetsPadding(scrollEdgeInsets)
                        .windowInsetsPadding(contentBottomInsets)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    GreenhouseTopBar(
                        isOnline = uiState.isOnline,
                        isAutoMode = uiState.isAutoMode,
                        deviceId = uiState.deviceId,
                        updatedAgo = uiState.updatedAgo,
                        onMenuClick = onMenuClick,
                        actions = topBarActions,
                    )
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        WideLayoutContent(
                            uiState = uiState,
                            onFanToggle = onFanToggle,
                            onHeaterToggle = onHeaterToggle,
                            onPumpToggle = onPumpToggle,
                            onLightsToggle = onLightsToggle,
                            onAutoModeToggle = onAutoModeToggle,
                            onVentChange = onVentChange,
                            onVentChangeFinished = onVentChangeFinished,
                            onTargetTempChange = onTargetTempChange,
                            onTargetHumidityChange = onTargetHumidityChange,
                            onTargetClimateFinished = onTargetClimateFinished,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .windowInsetsPadding(scrollEdgeInsets),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    GreenhouseTopBar(
                        isOnline = uiState.isOnline,
                        isAutoMode = uiState.isAutoMode,
                        deviceId = uiState.deviceId,
                        updatedAgo = uiState.updatedAgo,
                        onMenuClick = onMenuClick,
                        actions = topBarActions,
                    )
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
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
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                GreenhouseControlPanel(
                    fanOn = uiState.fanOn,
                    heaterOn = uiState.heaterOn,
                    pumpOn = uiState.pumpOn,
                    lightsOn = uiState.lightsOn,
                    isAutoMode = uiState.isAutoMode,
                    ventOpenPercent = uiState.ventOpenPercent,
                    targetTempC = uiState.targetTempC,
                    targetHumidityPercent = uiState.targetHumidityPercent,
                    onFanToggle = onFanToggle,
                    onHeaterToggle = onHeaterToggle,
                    onPumpToggle = onPumpToggle,
                    onLightsToggle = onLightsToggle,
                    onAutoModeToggle = onAutoModeToggle,
                    onVentChange = onVentChange,
                    onVentChangeFinished = onVentChangeFinished,
                    onTargetTempChange = onTargetTempChange,
                    onTargetHumidityChange = onTargetHumidityChange,
                    onTargetClimateFinished = onTargetClimateFinished,
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(scrollEdgeInsets.only(WindowInsetsSides.Horizontal))
                        .padding(horizontal = 16.dp)
                        .windowInsetsPadding(contentBottomInsets)
                        .padding(bottom = 12.dp, top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun WideLayoutContent(
    uiState: GreenhouseUiState,
    onFanToggle: () -> Unit,
    onHeaterToggle: () -> Unit,
    onPumpToggle: () -> Unit,
    onLightsToggle: () -> Unit,
    onAutoModeToggle: () -> Unit,
    onVentChange: (Int) -> Unit,
    onVentChangeFinished: () -> Unit,
    onTargetTempChange: (Int) -> Unit,
    onTargetHumidityChange: (Int) -> Unit,
    onTargetClimateFinished: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(0.4f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            EnvironmentalChart(chartData = uiState.chartData)
            GreenhouseControlPanel(
                fanOn = uiState.fanOn,
                heaterOn = uiState.heaterOn,
                pumpOn = uiState.pumpOn,
                lightsOn = uiState.lightsOn,
                isAutoMode = uiState.isAutoMode,
                ventOpenPercent = uiState.ventOpenPercent,
                targetTempC = uiState.targetTempC,
                targetHumidityPercent = uiState.targetHumidityPercent,
                onFanToggle = onFanToggle,
                onHeaterToggle = onHeaterToggle,
                onPumpToggle = onPumpToggle,
                onLightsToggle = onLightsToggle,
                onAutoModeToggle = onAutoModeToggle,
                onVentChange = onVentChange,
                onVentChangeFinished = onVentChangeFinished,
                onTargetTempChange = onTargetTempChange,
                onTargetHumidityChange = onTargetHumidityChange,
                onTargetClimateFinished = onTargetClimateFinished,
            )
        }
    }
}

@Composable
private fun GreenhouseScreenPreviewContent(
    uiState: GreenhouseUiState = GreenhouseUiState(isOnline = true, updatedAgo = "2m"),
) {
    GreenhouseScreenContent(
        uiState = uiState,
        onMenuClick = {},
        onFanToggle = {},
        onHeaterToggle = {},
        onPumpToggle = {},
        onLightsToggle = {},
        onAutoModeToggle = {},
        onVentChange = {},
        onVentChangeFinished = {},
        onTargetTempChange = {},
        onTargetHumidityChange = {},
        onTargetClimateFinished = {},
    )
}

@Preview(
    showBackground = true,
    name = "Greenhouse Portrait Dark",
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun GreenhouseScreenPortraitDarkPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        GreenhouseScreenPreviewContent()
    }
}

@Preview(
    showBackground = true,
    name = "Greenhouse Portrait Light",
)
@Composable
private fun GreenhouseScreenPortraitLightPreview() {
    TeleCon4Esp32Theme(darkTheme = false) {
        GreenhouseScreenPreviewContent()
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
        GreenhouseScreenPreviewContent()
    }
}
