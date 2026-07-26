package com.micsbol.telecon4esp32.ui.applications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.canUseAdvancedProtocol
import com.micsbol.telecon4esp32.domain.model.usesCamera
import com.micsbol.telecon4esp32.ui.components.NeoCard
import com.micsbol.telecon4esp32.ui.components.NeoSectionTitle
import com.micsbol.telecon4esp32.ui.entitlement.LocalEntitlement
import com.micsbol.telecon4esp32.ui.components.NeoScaffold
import com.micsbol.telecon4esp32.ui.theme.Neo

@Composable
fun ApplicationProtocolSettingsScreen(
    navController: NavController,
    applicationId: ApplicationId,
    viewModel: ApplicationSettingsViewModel = hiltViewModel(),
) {
    val connectionMode by viewModel.connectionMode.collectAsStateWithLifecycle()
    val selectedBoard by viewModel.board.collectAsStateWithLifecycle()
    val entitlement = LocalEntitlement.current
    val canUseAdvanced = entitlement.canUseAdvancedProtocol(applicationId)

    NeoScaffold(
        title = stringResource(applicationSettingsTitleRes(applicationId)),
        subtitle = stringResource(R.string.app_settings_communication_subtitle),
        onNavigateBack = { navController.navigateUp() },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (applicationId.usesCamera()) {
                item {
                    ApplicationDeviceSettingsSection(
                        selectedBoard = selectedBoard,
                        onBoardSelected = viewModel::onBoardChanged,
                    )
                }
            }
            if (applicationId == ApplicationId.RC_VEHICLE_PRO) {
                item {
                    RcVehicleCameraWifiSettingsSection()
                }
            }
            item {
                ApplicationProtocolSettingsSection(
                    applicationId = applicationId,
                    selectedMode = connectionMode,
                    onModeSelected = viewModel::onConnectionModeChanged,
                    canUseAdvanced = canUseAdvanced,
                    includeWifiSoftAp = applicationId == ApplicationId.RC_VEHICLE_PRO,
                )
            }
        }
    }
}

@Composable
private fun RcVehicleCameraWifiSettingsSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        NeoSectionTitle(text = stringResource(R.string.rc_vehicle_camera_wifi_section_title))
        Spacer(modifier = Modifier.height(8.dp))
        NeoCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.rc_vehicle_camera_wifi_section_body),
                style = MaterialTheme.typography.bodySmall,
                color = Neo.TextSecondary,
            )
        }
    }
}

@Composable
fun ApplicationProtocolSettingsLoadingScreen(
    navController: NavController,
    applicationId: ApplicationId,
) {
    NeoScaffold(
        title = stringResource(applicationSettingsTitleRes(applicationId)),
        subtitle = stringResource(R.string.app_settings_communication_subtitle),
        onNavigateBack = { navController.navigateUp() },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = Neo.Accent)
        }
    }
}

@Composable
fun ApplicationProtocolSettingsErrorScreen(
    navController: NavController,
    applicationId: ApplicationId,
    message: String,
) {
    NeoScaffold(
        title = stringResource(applicationSettingsTitleRes(applicationId)),
        subtitle = stringResource(R.string.app_settings_communication_subtitle),
        onNavigateBack = { navController.navigateUp() },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
