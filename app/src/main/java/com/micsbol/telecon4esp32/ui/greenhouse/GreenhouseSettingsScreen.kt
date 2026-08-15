package com.micsbol.telecon4esp32.ui.greenhouse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.linkFamily
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.SettingsUserType
import com.micsbol.telecon4esp32.domain.model.canUseAdvancedProtocol
import com.micsbol.telecon4esp32.domain.model.preferredConnectionMode
import com.micsbol.telecon4esp32.domain.model.settingsUserType
import com.micsbol.telecon4esp32.ui.applications.ApplicationSettingsSelectionGuideSection
import com.micsbol.telecon4esp32.ui.applications.applicationSettingsTitleRes
import com.micsbol.telecon4esp32.ui.components.rememberClampedSafeHudInsets
import com.micsbol.telecon4esp32.ui.entitlement.LocalEntitlement
import com.micsbol.telecon4esp32.ui.greenhouse.components.GreenhouseCard
import com.micsbol.telecon4esp32.ui.greenhouse.components.GreenhouseDeviceSettingsSection
import com.micsbol.telecon4esp32.ui.greenhouse.components.GreenhouseProtocolSettingsSection
import com.micsbol.telecon4esp32.ui.greenhouse.components.GreenhouseSubScreenTopBar
import com.micsbol.telecon4esp32.ui.greenhouse.components.GreenhouseUserTypeSettingsSection
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
    val selectedBoard by viewModel.board.collectAsState()
    val entitlement = LocalEntitlement.current
    val canUseAdvanced = entitlement.canUseAdvancedProtocol(ApplicationId.GREENHOUSE)

    if (GreenhouseEmulatorSupport.isEmulator()) {
        GreenhouseSettingsEmulatorContent(
            onBackClick = onBackClick,
            selectedMode = connectionMode,
            canUseAdvanced = canUseAdvanced,
            onModeSelected = viewModel::onConnectionModeChanged,
            selectedBoard = selectedBoard,
            onBoardSelected = viewModel::onBoardChanged,
        )
        return
    }

    GreenhouseSettingsFullContent(
        onBackClick = onBackClick,
        selectedMode = connectionMode,
        canUseAdvanced = canUseAdvanced,
        onModeSelected = viewModel::onConnectionModeChanged,
        selectedBoard = selectedBoard,
        onBoardSelected = viewModel::onBoardChanged,
    )
}

@Composable
private fun GreenhouseSettingsBody(
    selectedMode: BluetoothConnectionMode,
    canUseAdvanced: Boolean,
    onModeSelected: (BluetoothConnectionMode) -> Unit,
    selectedBoard: Esp32Board,
    onBoardSelected: (Esp32Board) -> Unit,
    includePinsSection: Boolean,
) {
    var userType by remember(selectedMode.settingsUserType) {
        mutableStateOf(selectedMode.settingsUserType)
    }

    GreenhouseCard(elevated = false) {
        GreenhouseUserTypeSettingsSection(
            selected = userType,
            canUseAdvanced = canUseAdvanced,
            onSelected = { type ->
                if (type == userType) return@GreenhouseUserTypeSettingsSection
                userType = type
                if (type == SettingsUserType.NORMAL && selectedBoard.isKitBDual) {
                    onBoardSelected(Esp32Board.CAM)
                    return@GreenhouseUserTypeSettingsSection
                }
                val preferred = ApplicationId.GREENHOUSE.preferredConnectionMode(
                    board = selectedBoard,
                    family = selectedMode.linkFamily,
                    userType = type,
                    canUseAdvanced = canUseAdvanced,
                )
                if (preferred != null && preferred != selectedMode) {
                    onModeSelected(preferred)
                }
            },
        )
    }
    GreenhouseCard(elevated = false) {
        GreenhouseDeviceSettingsSection(
            selectedBoard = selectedBoard,
            onBoardSelected = onBoardSelected,
            userType = userType,
            canUseAdvanced = canUseAdvanced,
        )
    }
    GreenhouseCard(elevated = false) {
        GreenhouseProtocolSettingsSection(
            selectedMode = selectedMode,
            userType = userType,
            canUseAdvanced = canUseAdvanced,
            onModeSelected = onModeSelected,
            selectedBoard = selectedBoard,
        )
    }
    if (includePinsSection) {
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
    GreenhouseCard(elevated = false) {
        ApplicationSettingsSelectionGuideSection(
            applicationId = ApplicationId.GREENHOUSE,
            selectedBoard = selectedBoard,
            selectedMode = selectedMode,
            titleColor = GreenhouseGlass.TextOnGlassPrimary,
            bodyColor = GreenhouseGlass.TextOnGlassSecondary,
            linkColor = GreenhouseGlass.AccentGreen,
            useNeoCard = false,
        )
    }
}

@Composable
private fun GreenhouseSettingsEmulatorContent(
    onBackClick: () -> Unit,
    selectedMode: BluetoothConnectionMode,
    canUseAdvanced: Boolean,
    onModeSelected: (BluetoothConnectionMode) -> Unit,
    selectedBoard: Esp32Board,
    onBoardSelected: (Esp32Board) -> Unit,
) {
    val edgeInsets = rememberClampedSafeHudInsets(
        includeTop = true,
        includeBottom = true,
        includeHorizontal = true,
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
                GreenhouseSettingsBody(
                    selectedMode = selectedMode,
                    canUseAdvanced = canUseAdvanced,
                    onModeSelected = onModeSelected,
                    selectedBoard = selectedBoard,
                    onBoardSelected = onBoardSelected,
                    includePinsSection = false,
                )
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
    selectedMode: BluetoothConnectionMode,
    canUseAdvanced: Boolean,
    onModeSelected: (BluetoothConnectionMode) -> Unit,
    selectedBoard: Esp32Board,
    onBoardSelected: (Esp32Board) -> Unit,
) {
    val edgeInsets = rememberClampedSafeHudInsets(
        includeTop = true,
        includeBottom = false,
        includeHorizontal = true,
    )
    val bottomInsets = rememberClampedSafeHudInsets(
        includeTop = false,
        includeBottom = true,
        includeHorizontal = false,
    )

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
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GreenhouseSettingsBody(
                    selectedMode = selectedMode,
                    canUseAdvanced = canUseAdvanced,
                    onModeSelected = onModeSelected,
                    selectedBoard = selectedBoard,
                    onBoardSelected = onBoardSelected,
                    includePinsSection = true,
                )
            }
        }
    }
}
