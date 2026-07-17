package com.micsbol.telecon4esp32.ui.greenhouse

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.canUseAdvancedProtocol
import com.micsbol.telecon4esp32.ui.applications.applicationSettingsTitleRes
import com.micsbol.telecon4esp32.ui.entitlement.LocalEntitlement
import com.micsbol.telecon4esp32.ui.greenhouse.components.GreenhouseCard
import com.micsbol.telecon4esp32.ui.greenhouse.components.GreenhouseProtocolSettingsSection
import com.micsbol.telecon4esp32.ui.greenhouse.components.GreenhouseSubScreenTopBar
import com.micsbol.telecon4esp32.ui.navigation.Screen

@Composable
fun GreenhouseSettingsScreen(
    navController: NavController,
    viewModel: GreenhouseViewModel = hiltViewModel(),
) {
    val onBackClick = {
        GreenhouseEmulatorNavigation.backToGreenhouse(navController, Screen.GreenhouseSettings.route)
    }
    val connectionMode by viewModel.connectionMode.collectAsState()
    val entitlement = LocalEntitlement.current
    val canUseAdvanced = entitlement.canUseAdvancedProtocol(ApplicationId.GREENHOUSE)

    if (GreenhouseEmulatorSupport.isEmulator()) {
        GreenhouseSettingsEmulatorContent(
            onBackClick = onBackClick,
            selectedMode = connectionMode,
            canUseAdvanced = canUseAdvanced,
            onModeSelected = viewModel::onConnectionModeChanged,
        )
        return
    }

    GreenhouseSettingsFullContent(
        onBackClick = onBackClick,
        selectedMode = connectionMode,
        canUseAdvanced = canUseAdvanced,
        onModeSelected = viewModel::onConnectionModeChanged,
    )
}

@Composable
private fun GreenhouseSettingsEmulatorContent(
    onBackClick: () -> Unit,
    selectedMode: com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode,
    canUseAdvanced: Boolean,
    onModeSelected: (com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode) -> Unit,
) {
    val edgeInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Top + WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
    )
    val scrollState = rememberScrollState()
    val title = stringResource(applicationSettingsTitleRes(ApplicationId.GREENHOUSE))

    GreenhouseBackground(showPhoto = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(edgeInsets)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GreenhouseGlassIconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.greenhouse_settings_back),
                        tint = GreenhouseGlass.AccentGreen,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GreenhouseGlass.TextPrimary,
                )
            }
            Text(
                text = stringResource(R.string.greenhouse_settings_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = GreenhouseGlass.TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GreenhouseCard(elevated = false) {
                    GreenhouseProtocolSettingsSection(
                        selectedMode = selectedMode,
                        canUseAdvanced = canUseAdvanced,
                        onModeSelected = onModeSelected,
                    )
                }
                Text(
                    text = stringResource(R.string.greenhouse_emulator_settings_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = GreenhouseGlass.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun GreenhouseSettingsFullContent(
    onBackClick: () -> Unit,
    selectedMode: com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode,
    canUseAdvanced: Boolean,
    onModeSelected: (com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode) -> Unit,
) {
    val edgeInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
    )
    val bottomInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)

    GreenhouseBackground(showPhoto = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(edgeInsets)
                .windowInsetsPadding(bottomInsets),
        ) {
            GreenhouseSubScreenTopBar(
                title = stringResource(applicationSettingsTitleRes(ApplicationId.GREENHOUSE)),
                subtitle = stringResource(R.string.greenhouse_settings_subtitle),
                onBackClick = onBackClick,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GreenhouseCard(elevated = false) {
                    GreenhouseProtocolSettingsSection(
                        selectedMode = selectedMode,
                        canUseAdvanced = canUseAdvanced,
                        onModeSelected = onModeSelected,
                    )
                }
                GreenhouseCard(elevated = false) {
                    Text(
                        text = stringResource(R.string.greenhouse_settings_pins_section_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenhouseGlass.TextOnGlassPrimary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.greenhouse_settings_pins_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = GreenhouseGlass.TextOnGlassSecondary,
                    )
                }
            }
        }
    }
}
