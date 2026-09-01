package com.micsbol.telecon4esp32.ui.applications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.micsbol.telecon4esp32.BuildConfig
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.ConnectionLinkFamily
import com.micsbol.telecon4esp32.domain.bluetooth.Esp32DevKitSoftApDefaults
import com.micsbol.telecon4esp32.domain.bluetooth.linkFamily
import com.micsbol.telecon4esp32.domain.camera.Esp32CameraDefaults
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.CameraHardwareRole
import com.micsbol.telecon4esp32.domain.model.PremiumFeature
import com.micsbol.telecon4esp32.domain.model.canUseAdvancedProtocol
import com.micsbol.telecon4esp32.domain.model.coerceForUserType
import com.micsbol.telecon4esp32.domain.model.preferredConnectionMode
import com.micsbol.telecon4esp32.domain.model.protocolPrefix
import com.micsbol.telecon4esp32.domain.model.SettingsUserType
import com.micsbol.telecon4esp32.domain.model.resolveCameraHardwareRole
import com.micsbol.telecon4esp32.domain.model.settingsUserType
import com.micsbol.telecon4esp32.domain.model.usesCamera
import com.micsbol.telecon4esp32.domain.model.usesCoinEconomy
import com.micsbol.telecon4esp32.ui.components.NeoCard
import com.micsbol.telecon4esp32.ui.components.NeoScaffold
import com.micsbol.telecon4esp32.ui.components.NeoSectionTitle
import com.micsbol.telecon4esp32.ui.control_panel.ControlPanelCenterFeatureUnlockDialogs
import com.micsbol.telecon4esp32.ui.entitlement.LocalEntitlement
import com.micsbol.telecon4esp32.ui.theme.HudCyan
import com.micsbol.telecon4esp32.ui.theme.Neo
import com.micsbol.telecon4esp32.ui.wallet.LocalWallet

@Composable
fun ApplicationProtocolSettingsScreen(
    navController: NavController,
    applicationId: ApplicationId,
    viewModel: ApplicationSettingsViewModel = hiltViewModel(),
) {
    val connectionMode by viewModel.connectionMode.collectAsStateWithLifecycle()
    val selectedBoard by viewModel.board.collectAsStateWithLifecycle()
    val useSoftApCamera by viewModel.useSoftApCamera.collectAsStateWithLifecycle()
    val userTypeOverride by viewModel.settingsUserTypeOverride.collectAsStateWithLifecycle()
    val configurationResetEpoch by viewModel.configurationResetEpoch.collectAsStateWithLifecycle()
    val advancedSettingsRevealed by viewModel.advancedSettingsRevealed.collectAsStateWithLifecycle()
    val entitlement = LocalEntitlement.current
    val wallet = LocalWallet.current
    val requiresCoinEntry = entitlement.usesCoinEconomy() || BuildConfig.DEBUG
    val canUseAdvanced = entitlement.canUseAdvancedProtocol(
        applicationId = applicationId,
        wallet = wallet,
        requiresCoinEntry = requiresCoinEntry,
    )
    val showAdvancedUi = canUseAdvanced && advancedSettingsRevealed
    val inferredUserType = connectionMode.settingsUserType
    val userType = if (showAdvancedUi) {
        userTypeOverride ?: inferredUserType
    } else {
        SettingsUserType.NORMAL
    }
    var showAdvancedInfo by remember { mutableStateOf(false) }
    var unlockAdvanced by remember { mutableStateOf(false) }
    LaunchedEffect(canUseAdvanced, userType) {
        if (!canUseAdvanced && userType == SettingsUserType.ADVANCED) {
            viewModel.onSettingsUserTypeSelected(SettingsUserType.NORMAL)
        }
    }
    val storedRole = resolveCameraHardwareRole(selectedBoard, useSoftApCamera)
    val cameraRole = storedRole.coerceForUserType(userType)
    val allowTwoDevices = true
    val showCamSoftApHardwareInfo = cameraRole == CameraHardwareRole.ONE_CAM
    val showOverlayCameraCredentials = cameraRole == CameraHardwareRole.TWO_DEVICES
    val showDevKitSoftApCredentials =
        cameraRole == CameraHardwareRole.NO_CAM &&
            (
                connectionMode == BluetoothConnectionMode.WIFI_SIMPLE ||
                    connectionMode == BluetoothConnectionMode.WIFI_BINARY
                )

    if (showAdvancedInfo) {
        AdvancedSettingsInfoDialog(
            onDismiss = { showAdvancedInfo = false },
            onUnlockClick = {
                showAdvancedInfo = false
                unlockAdvanced = true
            },
        )
    }
    if (unlockAdvanced) {
        ControlPanelCenterFeatureUnlockDialogs(
            unlockFeature = PremiumFeature.ADVANCED_PROTOCOL,
            featureTitle = stringResource(R.string.app_settings_advanced_unlock_title),
            navController = navController,
            onDismiss = { unlockAdvanced = false },
        )
    }

    NeoScaffold(
        title = stringResource(applicationSettingsTitleRes(applicationId)),
        subtitle = stringResource(R.string.app_settings_communication_subtitle),
        onNavigateBack = { navController.navigateUp() },
        actions = {
            if (!showAdvancedUi) {
                AdvancedSettingsTitleLockAction(
                    onClick = {
                        if (canUseAdvanced) {
                            viewModel.revealAdvancedSettings()
                        } else {
                            showAdvancedInfo = true
                        }
                    },
                )
            } else {
                ResetDefaultConfigurationTitleAction(
                    onReset = viewModel::resetToDefaultConfiguration,
                )
            }
        },
    ) { paddingValues ->
        key(configurationResetEpoch) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
            if (applicationId.usesCamera() && applicationId != ApplicationId.CONTROL_PANEL) {
                item {
                    CameraHardwareRoleSettingsSection(
                        selected = storedRole,
                        onRoleSelected = viewModel::onCameraHardwareRoleSelected,
                        allowTwoDevices = allowTwoDevices,
                        userType = userType,
                    )
                }
            }
            if (showAdvancedUi) {
                item {
                    SettingsUserTypeSection(
                        selected = userType,
                        canUseAdvanced = canUseAdvanced,
                        onRequestAdvancedUnlock = { unlockAdvanced = true },
                        onSelected = { type ->
                            if (type == userType) return@SettingsUserTypeSection
                            viewModel.onSettingsUserTypeSelected(type)
                            val family = if (cameraRole == CameraHardwareRole.TWO_DEVICES) {
                                ConnectionLinkFamily.BLUETOOTH
                            } else {
                                connectionMode.linkFamily
                            }
                            val preferred = applicationId.preferredConnectionMode(
                                board = selectedBoard,
                                family = family,
                                userType = type,
                                canUseAdvanced = canUseAdvanced,
                            )
                            if (preferred != null && preferred != connectionMode) {
                                viewModel.onConnectionModeChanged(preferred)
                            }
                        },
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
                    allowWifiControl = cameraRole != CameraHardwareRole.TWO_DEVICES,
                )
            }
            when {
                showCamSoftApHardwareInfo -> item {
                    RcVehicleCameraWifiSettingsSection(connectionMode = connectionMode)
                }
                showOverlayCameraCredentials -> item {
                    SoftApCameraOverlayWifiSettingsSection()
                }
                showDevKitSoftApCredentials -> item {
                    DevKitWifiSoftApSettingsSection(
                        applicationId = applicationId,
                        connectionMode = connectionMode,
                    )
                }
            }
            item {
                ApplicationSettingsSelectionGuideSection(
                    applicationId = applicationId,
                    selectedBoard = selectedBoard,
                    selectedMode = connectionMode,
                    useSoftApCamera = useSoftApCamera,
                )
            }
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
fun SoftApCameraOverlayWifiSettingsSection(
    modifier: Modifier = Modifier,
) {
    val ssid = Esp32CameraDefaults.STARTER_SOFTAP_SSID
    val password = Esp32CameraDefaults.SOFTAP_PASSWORD
    SoftApCredentialsCard(
        title = stringResource(R.string.rc_vehicle_camera_overlay_wifi_section_title),
        body = stringResource(
            R.string.rc_vehicle_camera_overlay_wifi_section_body,
            ssid,
            password,
        ),
        ssid = ssid,
        password = password,
        extraHint = stringResource(R.string.rc_vehicle_camera_wifi_runtime_stream_quality_hint),
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
        extraHint = stringResource(R.string.rc_vehicle_camera_wifi_runtime_stream_quality_hint),
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
    extraHint: String? = null,
) {
    var infoExpanded by remember { mutableStateOf(false) }
    val compactSsid = stringResource(R.string.app_settings_softap_ssid_label, ssid)
    val compactPassword = stringResource(R.string.app_settings_softap_password_label, password)
    val highlightedSsid = remember(compactSsid, ssid, password) {
        highlightWifiCredentials(compactSsid, ssid, password)
    }
    val highlightedPassword = remember(compactPassword, ssid, password) {
        highlightWifiCredentials(compactPassword, ssid, password)
    }
    val highlightedBody = remember(body, ssid, password) {
        highlightWifiCredentials(
            text = body,
            ssid = ssid,
            password = password,
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeoSectionTitle(
                text = title,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { infoExpanded = !infoExpanded },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(
                        R.string.app_settings_option_info_content_description,
                    ),
                    tint = if (infoExpanded) Neo.Accent else Neo.TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        NeoCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = highlightedSsid,
                style = MaterialTheme.typography.bodyLarge,
                color = Neo.TextSecondary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = highlightedPassword,
                style = MaterialTheme.typography.bodyLarge,
                color = Neo.TextSecondary,
            )
        }
        if (infoExpanded) {
            Spacer(modifier = Modifier.height(8.dp))
            NeoCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = highlightedBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = Neo.TextSecondary,
                )
            }
            if (!extraHint.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                NeoCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = extraHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = Neo.TextSecondary,
                    )
                }
            }
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
