package com.micsbol.telecon4esp32.ui.rc_settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.ConnectionLinkFamily
import com.micsbol.telecon4esp32.domain.bluetooth.linkFamily
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.CameraHardwareRole
import com.micsbol.telecon4esp32.domain.model.coerceForApplication
import com.micsbol.telecon4esp32.domain.model.ControlPanelCenterMode
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.model.PremiumFeature
import com.micsbol.telecon4esp32.domain.model.SettingsUserType
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.domain.model.canUseAdvancedProtocol
import com.micsbol.telecon4esp32.domain.model.canUseControlPanelCenterExtras
import com.micsbol.telecon4esp32.domain.model.effectiveCameraHardwareRole
import com.micsbol.telecon4esp32.domain.model.enablesCameraHardwareRoles
import com.micsbol.telecon4esp32.domain.model.isConnectionModeAvailable
import com.micsbol.telecon4esp32.domain.model.preferredConnectionMode
import com.micsbol.telecon4esp32.domain.model.resolveCameraHardwareRole
import com.micsbol.telecon4esp32.domain.model.settingsUserType
import com.micsbol.telecon4esp32.domain.model.WifiLinkChoice
import com.micsbol.telecon4esp32.domain.model.usesCoinEconomy
import com.micsbol.telecon4esp32.ui.applications.AdvancedSettingsInfoDialog
import com.micsbol.telecon4esp32.ui.applications.AdvancedSettingsTitleLockAction
import com.micsbol.telecon4esp32.ui.applications.ApplicationProtocolSettingsSection
import com.micsbol.telecon4esp32.ui.applications.ApplicationSettingsSelectionGuideSection
import com.micsbol.telecon4esp32.ui.applications.ApplicationSettingsSection
import com.micsbol.telecon4esp32.ui.applications.ApplicationSettingsViewModel
import com.micsbol.telecon4esp32.ui.applications.DevKitWifiSoftApSettingsSection
import com.micsbol.telecon4esp32.ui.applications.RcVehicleCameraWifiSettingsSection
import com.micsbol.telecon4esp32.ui.applications.SettingsUserTypeSection
import com.micsbol.telecon4esp32.ui.applications.SoftApCameraOverlayWifiSettingsSection
import com.micsbol.telecon4esp32.ui.applications.applicationSettingsTitleRes
import com.micsbol.telecon4esp32.ui.applications.controlPanelSettingsTitleRes
import com.micsbol.telecon4esp32.ui.components.NeoCard
import com.micsbol.telecon4esp32.ui.components.NeoScaffold
import com.micsbol.telecon4esp32.ui.components.NeoSectionTitle
import com.micsbol.telecon4esp32.ui.control_panel.ControlPanelCenterFeatureUnlockDialogs
import com.micsbol.telecon4esp32.ui.entitlement.LocalEntitlement
import com.micsbol.telecon4esp32.ui.theme.Neo
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme
import com.micsbol.telecon4esp32.ui.wallet.LocalWallet
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun RcSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    uiState: SettingsUiState? = null,
    applicationId: ApplicationId = ApplicationId.CONTROL_PANEL,
    settingsSection: ApplicationSettingsSection = ApplicationSettingsSection.CONNECTION,
    applicationSettingsViewModel: ApplicationSettingsViewModel? = null,
) {
    val state = uiState ?: viewModel.uiState.collectAsStateWithLifecycle().value
    val connectionMode = applicationSettingsViewModel
        ?.connectionMode
        ?.collectAsStateWithLifecycle()
        ?.value
    val selectedBoard = applicationSettingsViewModel
        ?.board
        ?.collectAsStateWithLifecycle()
        ?.value
        ?: Esp32Board.DEV_KIT
    val useSoftApCamera = applicationSettingsViewModel
        ?.useSoftApCamera
        ?.collectAsStateWithLifecycle()
        ?.value
        ?: false
    val userTypeOverride = applicationSettingsViewModel
        ?.settingsUserTypeOverride
        ?.collectAsStateWithLifecycle()
        ?.value
    val advancedSettingsRevealed = applicationSettingsViewModel
        ?.advancedSettingsRevealed
        ?.collectAsStateWithLifecycle()
        ?.value
        ?: true
    val fallbackCenterMode = remember { MutableStateFlow(ControlPanelCenterMode.TERMINAL) }
    val fallbackCenterModeLoaded = remember { MutableStateFlow(true) }
    val centerMode by (applicationSettingsViewModel?.centerMode ?: fallbackCenterMode)
        .collectAsStateWithLifecycle()
    val centerModeLoaded by (applicationSettingsViewModel?.isCenterModeLoaded
        ?: fallbackCenterModeLoaded)
        .collectAsStateWithLifecycle()
    val entitlement = LocalEntitlement.current
    val wallet = LocalWallet.current
    val requiresCoinEntry = entitlement.usesCoinEconomy()
    val centerExtrasUnlocked = entitlement.canUseControlPanelCenterExtras(
        wallet = wallet,
        requiresCoinEntry = requiresCoinEntry,
    )
    val canUseAdvanced = entitlement.canUseAdvancedProtocol(
        applicationId = applicationId,
        wallet = wallet,
        requiresCoinEntry = requiresCoinEntry,
    )
    val showAdvancedUi = canUseAdvanced && advancedSettingsRevealed
    var showAdvancedInfo by remember { mutableStateOf(false) }
    var unlockAdvanced by remember { mutableStateOf(false) }

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

    val screenTitle = if (applicationId == ApplicationId.CONTROL_PANEL) {
        stringResource(controlPanelSettingsTitleRes(settingsSection))
    } else {
        stringResource(applicationSettingsTitleRes(applicationId))
    }
    val screenSubtitle = stringResource(R.string.app_control_panel_settings_title)

    NeoScaffold(
        title = screenTitle,
        subtitle = screenSubtitle,
        onNavigateBack = { navController.navigateUp() },
    ) { paddingValues ->
        when (val state = state) {
            is SettingsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Neo.Accent)
                }
            }
            is SettingsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = Neo.Negative,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is SettingsUiState.Success -> {
                SettingsContent(
                        modifier = Modifier.padding(paddingValues),
                        applicationId = applicationId,
                        settingsSection = settingsSection,
                        connectionMode = connectionMode,
                        selectedBoard = selectedBoard,
                        useSoftApCamera = useSoftApCamera,
                        centerMode = centerMode,
                        centerModeLoaded = centerModeLoaded,
                        centerExtrasUnlocked = centerExtrasUnlocked,
                        canUseAdvanced = canUseAdvanced,
                        showAdvancedUi = showAdvancedUi,
                        onRequestAdvancedUnlock = { unlockAdvanced = true },
                        onConnectionModeChanged = applicationSettingsViewModel?.let { vm ->
                            { mode -> vm.onConnectionModeChanged(mode) }
                        },
                        onWifiChoiceSelected = applicationSettingsViewModel?.let { vm ->
                            { choice, type, advanced ->
                                vm.onWifiLinkChoiceSelected(choice, type, advanced)
                            }
                        },
                        onBluetoothFamilySelected = applicationSettingsViewModel?.let { vm ->
                            { type, advanced -> vm.onBluetoothLinkSelected(type, advanced) }
                        },
                        userTypeOverride = userTypeOverride,
                        onSettingsUserTypeSelected = applicationSettingsViewModel?.let { vm ->
                            { type -> vm.onSettingsUserTypeSelected(type) }
                        },
                    )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    modifier: Modifier = Modifier,
    applicationId: ApplicationId = ApplicationId.CONTROL_PANEL,
    settingsSection: ApplicationSettingsSection = ApplicationSettingsSection.CONNECTION,
    connectionMode: BluetoothConnectionMode? = null,
    selectedBoard: Esp32Board = Esp32Board.DEV_KIT,
    useSoftApCamera: Boolean = false,
    centerMode: ControlPanelCenterMode = ControlPanelCenterMode.TERMINAL,
    centerModeLoaded: Boolean = true,
    centerExtrasUnlocked: Boolean = false,
    canUseAdvanced: Boolean = true,
    showAdvancedUi: Boolean = true,
    onRequestAdvancedUnlock: (() -> Unit)? = null,
    onConnectionModeChanged: ((BluetoothConnectionMode) -> Unit)? = null,
    onWifiChoiceSelected: ((WifiLinkChoice, SettingsUserType, Boolean) -> Unit)? = null,
    onBluetoothFamilySelected: ((SettingsUserType, Boolean) -> Unit)? = null,
    userTypeOverride: SettingsUserType? = null,
    onSettingsUserTypeSelected: ((SettingsUserType) -> Unit)? = null,
) {
    val inferredUserType = connectionMode?.settingsUserType ?: SettingsUserType.NORMAL
    val userType = if (showAdvancedUi) {
        userTypeOverride ?: inferredUserType
    } else {
        SettingsUserType.NORMAL
    }
    LaunchedEffect(canUseAdvanced, userType) {
        if (!canUseAdvanced && userType == SettingsUserType.ADVANCED) {
            onSettingsUserTypeSelected?.invoke(SettingsUserType.NORMAL)
        }
    }
    val storedRole = resolveCameraHardwareRole(selectedBoard, useSoftApCamera)
    val centerCameraEnabled = centerModeLoaded &&
        centerMode.enablesCameraHardwareRoles(centerExtrasUnlocked)
    val cameraRole = applicationId.effectiveCameraHardwareRole(
        stored = storedRole,
        userType = userType,
        centerExtrasUnlocked = centerExtrasUnlocked,
        centerCameraEnabled = centerCameraEnabled,
    )
    val settingsConnectionMode = connectionMode?.let { mode ->
        if (applicationId.isConnectionModeAvailable(selectedBoard, mode)) {
            mode
        } else {
            BluetoothConnectionMode.CLASSIC_SIMPLE
        }
    }
    LaunchedEffect(cameraRole, connectionMode, settingsConnectionMode) {
        val stored = connectionMode ?: return@LaunchedEffect
        val next = settingsConnectionMode ?: return@LaunchedEffect
        if (cameraRole != CameraHardwareRole.TWO_DEVICES || !stored.isWifiLink) {
            return@LaunchedEffect
        }
        if (next != stored) {
            onConnectionModeChanged?.invoke(next)
        }
    }
    // Do not persist coerced CAM roles when opening settings. A load flicker
    // (Plots placeholder vs live Camera) used to write No CAM and tear down
    // the SoftAP stream under a displayed frame.

    LazyColumn(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .imePadding(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (
            settingsSection == ApplicationSettingsSection.CONNECTION &&
            settingsConnectionMode != null &&
            onConnectionModeChanged != null
        ) {
            item {
                ApplicationProtocolSettingsSection(
                    applicationId = applicationId,
                    selectedMode = settingsConnectionMode,
                    onModeSelected = onConnectionModeChanged,
                    userType = userType,
                    canUseAdvanced = canUseAdvanced,
                    selectedBoard = selectedBoard,
                    allowWifiControl = false,
                )
            }
            when {
                storedRole.coerceForApplication(applicationId) == CameraHardwareRole.TWO_DEVICES -> item {
                    SoftApCameraOverlayWifiSettingsSection()
                }
            }
            item {
                ApplicationSettingsSelectionGuideSection(
                    applicationId = applicationId,
                    selectedBoard = selectedBoard,
                    selectedMode = settingsConnectionMode,
                    useSoftApCamera = storedRole == CameraHardwareRole.TWO_DEVICES,
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        NeoSectionTitle(text = title)
        NeoCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                content()
            }
        }
    }
}



@Preview(showBackground = true, name = "RC Settings Dark")
@Preview(showBackground = true, name = "RC Settings Light", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
fun RcSettingsScreenPreview() {
    val navController = rememberNavController()
    val mockSettings = UserSettings(
        leftStickMode = JoystickMode.Spring(),
        rightStickMode = JoystickMode.Hold(),
        switchInitialStates = mapOf(0 to true, 1 to false, 2 to true, 3 to false, 4 to true, 5 to false),
        leftKnobInitialValue = 0.5f,
        rightKnobInitialValue = 0.7f
    )
    val uiState = SettingsUiState.Success(mockSettings)
    TeleCon4Esp32Theme {
        RcSettingsScreen(
            navController = navController,
            uiState = uiState,
            settingsSection = ApplicationSettingsSection.CONNECTION,
        )
    }
}
