package com.micsbol.telecon4esp32.ui.smarthome

import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.micsbol.telecon4esp32.ui.components.safeHudPadding
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.applications.applicationSettingsTitleRes
import com.micsbol.telecon4esp32.ui.applications.navigateToApplicationSettings
import com.micsbol.telecon4esp32.ui.bluetooth.ApplicationBluetoothTopBarButton
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.smarthome.components.EnergyUsageChart
import com.micsbol.telecon4esp32.ui.smarthome.components.RecentEventsList
import com.micsbol.telecon4esp32.ui.smarthome.components.RoomCard
import com.micsbol.telecon4esp32.ui.smarthome.components.SmartHomeSectionHeader
import com.micsbol.telecon4esp32.ui.smarthome.components.SmartHomeTopBar
import com.micsbol.telecon4esp32.ui.smarthome.components.SystemTile
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme

@Composable
fun SmartHomeScreen(
    navController: NavController,
    viewModel: SmartHomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    SmartHomeScreenContent(
        uiState = uiState,
        onMenuClick = { navController.navigateUp() },
        onRoomDeviceToggle = viewModel::toggleRoomDevice,
        onSceneAllLightsOff = viewModel::applySceneAllLightsOff,
        onSceneAway = viewModel::applySceneAway,
        topBarActions = {
            Row(
                modifier = Modifier.padding(start = 4.dp, end = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ApplicationBluetoothTopBarButton(accent = SmartHomeGlass.AccentWarm) { onClick, enabled, content ->
                    SmartHomeGlassIconButton(onClick = onClick, enabled = enabled, content = content)
                }
                SmartHomeGlassIconButton(
                    onClick = { navController.navigate(Screen.SmartHomeHelp.route) },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = stringResource(R.string.smart_home_help_content_description),
                        tint = SmartHomeGlass.AccentWarm,
                        modifier = Modifier.size(22.dp),
                    )
                }
                SmartHomeGlassIconButton(
                    onClick = { navController.navigateToApplicationSettings(ApplicationId.SMART_HOME) },
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(
                            R.string.applications_settings_content_description,
                            stringResource(applicationSettingsTitleRes(ApplicationId.SMART_HOME)),
                        ),
                        tint = SmartHomeGlass.AccentWarm,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        },
    )
}

@Composable
fun SmartHomeScreenContent(
    uiState: SmartHomeUiState,
    onMenuClick: () -> Unit,
    onRoomDeviceToggle: (roomId: String, deviceId: String) -> Unit = { _, _ -> },
    onSceneAllLightsOff: () -> Unit = {},
    onSceneAway: () -> Unit = {},
    modifier: Modifier = Modifier,
    topBarActions: @Composable () -> Unit = {},
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()

    SmartHomeBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            SmartHomeTopBar(
                allSystemsNormal = uiState.allSystemsNormal,
                isOnline = uiState.isOnline,
                deviceId = uiState.deviceId,
                updatedAgo = uiState.updatedAgo,
                onMenuClick = onMenuClick,
                actions = topBarActions,
            )
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
            ) {
                val useWideLayout = isLandscape && maxWidth >= 600.dp
                val sectionPadding = Modifier.padding(horizontal = 16.dp)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeHudPadding(
                            includeTop = false,
                            includeBottom = true,
                            includeHorizontal = true,
                        )
                        .verticalScroll(scrollState)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    SmartHomeSectionHeader(
                        title = stringResource(R.string.smart_home_section_rooms),
                        modifier = sectionPadding,
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(
                            items = uiState.rooms,
                            key = { it.id },
                        ) { room ->
                            RoomCard(
                                room = room,
                                onDeviceToggle = { deviceId ->
                                    onRoomDeviceToggle(room.id, deviceId)
                                },
                            )
                        }
                    }

                    SmartHomeSectionHeader(
                        title = stringResource(R.string.smart_home_section_scenes),
                        modifier = sectionPadding,
                    )
                    Row(
                        modifier = sectionPadding
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SmartHomeSceneChip(
                            label = stringResource(R.string.smart_home_scene_all_lights_off),
                            onClick = onSceneAllLightsOff,
                        )
                        SmartHomeSceneChip(
                            label = stringResource(R.string.smart_home_scene_away),
                            onClick = onSceneAway,
                        )
                    }

                    SmartHomeSectionHeader(
                        title = stringResource(R.string.smart_home_section_systems),
                        modifier = sectionPadding,
                    )
                    if (useWideLayout) {
                        Row(
                            modifier = sectionPadding.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            uiState.systems.forEach { system ->
                                SystemTile(
                                    system = system,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = sectionPadding
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            uiState.systems.forEach { system ->
                                SystemTile(
                                    system = system,
                                    modifier = Modifier.fillMaxWidth(0.42f),
                                )
                            }
                        }
                    }

                    if (useWideLayout) {
                        Row(
                            modifier = sectionPadding.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            EnergyUsageChart(
                                chartData = uiState.energyChart,
                                modifier = Modifier.weight(1f),
                            )
                            RecentEventsList(
                                events = uiState.recentEvents,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        Column(
                            modifier = sectionPadding,
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            EnergyUsageChart(chartData = uiState.energyChart)
                            RecentEventsList(events = uiState.recentEvents)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SmartHomeSceneChip(
    label: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .background(
                color = SmartHomeGlass.CardSurface.copy(alpha = SmartHomeGlass.ChipSurfaceAlpha),
                shape = SmartHomeGlass.PillShape,
            ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = SmartHomeGlass.AccentWarm,
        )
    }
}

@Preview(
    showBackground = true,
    name = "Smart Home Portrait Dark",
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun SmartHomeScreenPortraitDarkPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        SmartHomeScreenContent(
            uiState = SmartHomeUiState(),
            onMenuClick = {},
        )
    }
}

@Preview(
    showBackground = true,
    name = "Smart Home Portrait Light",
)
@Composable
private fun SmartHomeScreenPortraitLightPreview() {
    TeleCon4Esp32Theme(darkTheme = false) {
        SmartHomeScreenContent(
            uiState = SmartHomeUiState(),
            onMenuClick = {},
        )
    }
}

@Preview(
    showBackground = true,
    name = "Smart Home Landscape Dark",
    device = "spec:width=1280dp,height=800dp,dpi=160,orientation=landscape",
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun SmartHomeScreenLandscapeDarkPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        SmartHomeScreenContent(
            uiState = SmartHomeUiState(),
            onMenuClick = {},
        )
    }
}
