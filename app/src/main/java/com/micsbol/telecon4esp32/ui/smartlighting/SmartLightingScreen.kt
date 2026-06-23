package com.micsbol.telecon4esp32.ui.smartlighting

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.micsbol.telecon4esp32.ui.applications.ApplicationSettingsIconButton
import com.micsbol.telecon4esp32.ui.smartlighting.components.AddLightingDeviceTile
import com.micsbol.telecon4esp32.ui.smartlighting.components.BedroomLightingDeviceCard
import com.micsbol.telecon4esp32.ui.smartlighting.components.ConnectedLightingDeviceCard
import com.micsbol.telecon4esp32.ui.smartlighting.components.LightingSettingToggleRow
import com.micsbol.telecon4esp32.ui.smartlighting.components.SmartLightingAllLightsButton
import com.micsbol.telecon4esp32.ui.smartlighting.components.SmartLightingSectionLabel
import com.micsbol.telecon4esp32.ui.smartlighting.components.SmartLightingTopBar
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme

@Composable
fun SmartLightingScreen(
    navController: NavController,
    viewModel: SmartLightingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    SmartLightingScreenContent(
        uiState = uiState,
        onBackClick = { navController.navigateUp() },
        onSettingToggle = viewModel::onSettingToggle,
        onDeviceToggle = viewModel::onDeviceToggle,
        onToggleAllDevices = viewModel::onToggleAllDevices,
        topBarActions = {
            ApplicationSettingsIconButton(
                applicationId = ApplicationId.SMART_LIGHTING,
                navController = navController,
            )
        },
    )
}

@Composable
fun SmartLightingScreenContent(
    uiState: SmartLightingUiState,
    onBackClick: () -> Unit,
    onSettingToggle: (String, Boolean) -> Unit,
    onDeviceToggle: (String) -> Unit,
    onToggleAllDevices: () -> Unit,
    modifier: Modifier = Modifier,
    topBarActions: @Composable () -> Unit = {},
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SmartLightingTopBar(
            onBackClick = onBackClick,
            actions = topBarActions,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SmartLightingAllLightsButton(
                allDevicesOn = uiState.allDevicesOn,
                onClick = onToggleAllDevices,
                modifier = Modifier.fillMaxWidth(),
            )

            lightingDeviceSectionOrder.forEachIndexed { index, category ->
                val devices = uiState.connectedDevices.filter { it.category == category }
                if (devices.isEmpty()) return@forEachIndexed

                SmartLightingSectionLabel(
                    text = stringResource(category.sectionTitleRes()),
                    modifier = if (index > 0) Modifier.padding(top = 8.dp) else Modifier,
                )
                devices.forEach { device ->
                    if (category == LightingDeviceCategory.BEDROOM) {
                        BedroomLightingDeviceCard(
                            device = device,
                            onClick = { onDeviceToggle(device.id) },
                        )
                    } else {
                        ConnectedLightingDeviceCard(
                            device = device,
                            onClick = { onDeviceToggle(device.id) },
                        )
                    }
                }
            }

            SmartLightingSectionLabel(
                text = stringResource(R.string.smart_lighting_section_add_device),
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                uiState.addDeviceOptions.forEach { option ->
                    AddLightingDeviceTile(
                        option = option,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            SmartLightingSectionLabel(
                text = stringResource(R.string.smart_lighting_section_settings),
                modifier = Modifier.padding(top = 8.dp),
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    uiState.settings.forEachIndexed { index, setting ->
                        LightingSettingToggleRow(
                            setting = setting,
                            onCheckedChange = { enabled ->
                                onSettingToggle(setting.id, enabled)
                            },
                            showDivider = index > 0,
                        )
                    }
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "Smart Lighting Portrait Dark",
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun SmartLightingScreenDarkPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        SmartLightingScreenContent(
            uiState = SmartLightingUiState(),
            onBackClick = {},
            onSettingToggle = { _, _ -> },
            onDeviceToggle = {},
            onToggleAllDevices = {},
        )
    }
}
