package com.micsbol.telecon4esp32.ui.applications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.micsbol.telecon4esp32.ui.components.TeleCon4Esp32Scaffold
import com.micsbol.telecon4esp32.ui.components.brandPrimary

@Composable
fun ApplicationProtocolSettingsScreen(
    navController: NavController,
    applicationId: ApplicationId,
    viewModel: ApplicationSettingsViewModel = hiltViewModel(),
) {
    val protocolMode by viewModel.protocolMode.collectAsStateWithLifecycle()

    TeleCon4Esp32Scaffold(
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
            item {
                ApplicationProtocolSettingsSection(
                    applicationId = applicationId,
                    selectedMode = protocolMode,
                    onModeSelected = viewModel::onProtocolModeChanged,
                )
            }
        }
    }
}

@Composable
fun ApplicationProtocolSettingsLoadingScreen(
    navController: NavController,
    applicationId: ApplicationId,
) {
    TeleCon4Esp32Scaffold(
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
            CircularProgressIndicator(color = brandPrimary())
        }
    }
}

@Composable
fun ApplicationProtocolSettingsErrorScreen(
    navController: NavController,
    applicationId: ApplicationId,
    message: String,
) {
    TeleCon4Esp32Scaffold(
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
