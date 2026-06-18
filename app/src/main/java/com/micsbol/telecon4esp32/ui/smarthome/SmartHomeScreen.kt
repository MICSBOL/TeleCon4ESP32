package com.micsbol.telecon4esp32.ui.smarthome

import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
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
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.applications.ApplicationSettingsIconButton
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
        topBarActions = {
            ApplicationSettingsIconButton(
                applicationId = ApplicationId.SMART_HOME,
                navController = navController,
            )
        },
    )
}

@Composable
fun SmartHomeScreenContent(
    uiState: SmartHomeUiState,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    topBarActions: @Composable () -> Unit = {},
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()
    val roomsScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SmartHomeTopBar(
            allSystemsNormal = uiState.allSystemsNormal,
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
                SmartHomeSectionHeader(
                    title = stringResource(R.string.smart_home_section_rooms),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(roomsScrollState),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    uiState.rooms.forEach { room ->
                        RoomCard(room = room)
                    }
                }

                SmartHomeSectionHeader(
                    title = stringResource(R.string.smart_home_section_systems),
                )
                if (useWideLayout) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                    EnergyUsageChart(chartData = uiState.energyChart)
                    RecentEventsList(events = uiState.recentEvents)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
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
