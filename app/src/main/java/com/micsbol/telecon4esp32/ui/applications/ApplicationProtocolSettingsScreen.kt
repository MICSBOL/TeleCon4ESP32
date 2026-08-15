package com.micsbol.telecon4esp32.ui.applications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.Esp32DevKitSoftApDefaults
import com.micsbol.telecon4esp32.domain.bluetooth.linkFamily
import com.micsbol.telecon4esp32.domain.camera.Esp32CameraDefaults
import com.micsbol.telecon4esp32.domain.camera.showSoftApPerformanceSettings
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.canUseAdvancedProtocol
import com.micsbol.telecon4esp32.domain.model.preferredConnectionMode
import com.micsbol.telecon4esp32.domain.model.protocolPrefix
import com.micsbol.telecon4esp32.domain.model.SettingsUserType
import com.micsbol.telecon4esp32.domain.model.settingsUserType
import com.micsbol.telecon4esp32.domain.model.usesCamera
import com.micsbol.telecon4esp32.ui.components.NeoCard
import com.micsbol.telecon4esp32.ui.components.NeoScaffold
import com.micsbol.telecon4esp32.ui.components.NeoSectionTitle
import com.micsbol.telecon4esp32.ui.entitlement.LocalEntitlement
import com.micsbol.telecon4esp32.ui.theme.HudCyan
import com.micsbol.telecon4esp32.ui.theme.Neo

@Composable
fun ApplicationProtocolSettingsScreen(
    navController: NavController,
    applicationId: ApplicationId,
    viewModel: ApplicationSettingsViewModel = hiltViewModel(),
) {
    val connectionMode by viewModel.connectionMode.collectAsStateWithLifecycle()
    val selectedBoard by viewModel.board.collectAsStateWithLifecycle()
    val softApPerformancePreset by viewModel.softApPerformancePreset.collectAsStateWithLifecycle()
    val softApHudProcessingRate by viewModel.softApHudProcessingRate.collectAsStateWithLifecycle()
    val entitlement = LocalEntitlement.current
    val canUseAdvanced = entitlement.canUseAdvancedProtocol(applicationId)
    var userType by remember(connectionMode.settingsUserType) {
        mutableStateOf(connectionMode.settingsUserType)
    }
    val showCamSoftApHardwareInfo =
        applicationId.usesCamera() &&
            (
                (selectedBoard == Esp32Board.CAM && connectionMode.isSoftApTcp) ||
                    selectedBoard.isKitBDual
                )
    val showDevKitSoftApCredentials =
        selectedBoard == Esp32Board.DEV_KIT &&
            (
                connectionMode == BluetoothConnectionMode.WIFI_SIMPLE ||
                    connectionMode == BluetoothConnectionMode.WIFI_BINARY
                )

    NeoScaffold(
        title = stringResource(applicationSettingsTitleRes(applicationId)),
        subtitle = stringResource(R.string.app_settings_communication_subtitle),
        onNavigateBack = { navController.navigateUp() },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                SettingsUserTypeSection(
                    selected = userType,
                    canUseAdvanced = canUseAdvanced,
                    onSelected = { type ->
                        if (type == userType) return@SettingsUserTypeSection
                        userType = type
                        if (type == SettingsUserType.NORMAL && selectedBoard.isKitBDual) {
                            viewModel.onBoardChanged(Esp32Board.CAM)
                            return@SettingsUserTypeSection
                        }
                        val preferred = applicationId.preferredConnectionMode(
                            board = selectedBoard,
                            family = connectionMode.linkFamily,
                            userType = type,
                            canUseAdvanced = canUseAdvanced,
                        )
                        if (preferred != null && preferred != connectionMode) {
                            viewModel.onConnectionModeChanged(preferred)
                        }
                    },
                )
            }
            if (applicationId.usesCamera()) {
                item {
                    ApplicationDeviceSettingsSection(
                        selectedBoard = selectedBoard,
                        onBoardSelected = viewModel::onBoardChanged,
                        userType = userType,
                        canUseAdvanced = canUseAdvanced,
                    )
                }
            }
            if (
                applicationId == ApplicationId.RC_VEHICLE_PRO &&
                showSoftApPerformanceSettings(applicationId, selectedBoard, connectionMode)
            ) {
                item {
                    SoftApPerformanceSettingsSection(
                        selectedPreset = softApPerformancePreset,
                        onPresetSelected = viewModel::onSoftApPerformancePresetChanged,
                        selectedHudRate = softApHudProcessingRate,
                        onHudRateSelected = viewModel::onSoftApHudProcessingRateChanged,
                    )
                }
            }
            item {
                ApplicationProtocolSettingsSection(
                    applicationId = applicationId,
                    selectedMode = connectionMode,
                    onModeSelected = viewModel::onConnectionModeChanged,
                    userType = userType,
                    canUseAdvanced = canUseAdvanced,
                    selectedBoard = selectedBoard,
                    belowConnectionTypeContent = when {
                        showCamSoftApHardwareInfo -> {
                            { RcVehicleCameraWifiSettingsSection(connectionMode = connectionMode) }
                        }
                        showDevKitSoftApCredentials -> {
                            {
                                DevKitWifiSoftApSettingsSection(
                                    applicationId = applicationId,
                                    connectionMode = connectionMode,
                                )
                            }
                        }
                        else -> null
                    },
                )
            }
            item {
                ApplicationSettingsSelectionGuideSection(
                    applicationId = applicationId,
                    selectedBoard = selectedBoard,
                    selectedMode = connectionMode,
                )
            }
        }
    }
}

@Composable
fun DevKitWifiSoftApSettingsSection(
    applicationId: ApplicationId,
    connectionMode: BluetoothConnectionMode,
    modifier: Modifier = Modifier,
) {
    val ssid = Esp32DevKitSoftApDefaults.softApSsid(
        applicationId.protocolPrefix(),
        connectionMode,
    )
    val password = Esp32DevKitSoftApDefaults.SOFTAP_PASSWORD
    SoftApCredentialsCard(
        title = stringResource(R.string.app_settings_devkit_wifi_section_title),
        body = stringResource(R.string.app_settings_devkit_wifi_section_body, ssid, password),
        ssid = ssid,
        password = password,
        modifier = modifier,
    )
}

@Composable
fun RcVehicleCameraWifiSettingsSection(
    connectionMode: BluetoothConnectionMode = BluetoothConnectionMode.WIFI_BINARY,
    modifier: Modifier = Modifier,
) {
    val ssid = when (connectionMode) {
        BluetoothConnectionMode.WIFI_CAM_STARTER -> Esp32CameraDefaults.STARTER_SOFTAP_SSID
        else -> Esp32CameraDefaults.SOFTAP_SSID
    }
    val password = Esp32CameraDefaults.SOFTAP_PASSWORD
    SoftApCredentialsCard(
        title = stringResource(R.string.rc_vehicle_camera_wifi_section_title),
        body = stringResource(R.string.rc_vehicle_camera_wifi_section_body, ssid, password),
        ssid = ssid,
        password = password,
        modifier = modifier,
    )
}

@Composable
private fun SoftApCredentialsCard(
    title: String,
    body: String,
    ssid: String,
    password: String,
    modifier: Modifier = Modifier,
) {
    val highlightedBody = remember(body, ssid, password) {
        highlightWifiCredentials(
            text = body,
            ssid = ssid,
            password = password,
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        NeoSectionTitle(text = title)
        Spacer(modifier = Modifier.height(8.dp))
        NeoCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = highlightedBody,
                style = MaterialTheme.typography.bodySmall,
                color = Neo.TextSecondary,
            )
        }
    }
}

private fun highlightWifiCredentials(
    text: String,
    ssid: String,
    password: String,
) = buildAnnotatedString {
    var index = 0
    while (index < text.length) {
        val ssidIndex = text.indexOf(ssid, index)
        val passwordIndex = text.indexOf(password, index)
        val nextHighlight = when {
            ssidIndex < 0 && passwordIndex < 0 -> -1
            ssidIndex < 0 -> passwordIndex
            passwordIndex < 0 -> ssidIndex
            else -> minOf(ssidIndex, passwordIndex)
        }
        if (nextHighlight < 0) {
            append(text.substring(index))
            break
        }
        if (nextHighlight > index) {
            append(text.substring(index, nextHighlight))
        }
        if (nextHighlight == ssidIndex) {
            withStyle(
                SpanStyle(
                    color = Neo.Accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
            ) {
                append(ssid)
            }
            index = nextHighlight + ssid.length
        } else {
            withStyle(
                SpanStyle(
                    color = HudCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
            ) {
                append(password)
            }
            index = nextHighlight + password.length
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
