package com.micsbol.telecon4esp32.ui.control_panel

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.ControlPanelCenterMode
import com.micsbol.telecon4esp32.ui.applications.ApplicationSettingsSection
import com.micsbol.telecon4esp32.ui.applications.navigateToApplicationSettings
import com.micsbol.telecon4esp32.ui.components.LiveControlBluetoothDisconnectedBannerOverlay
import com.micsbol.telecon4esp32.ui.components.NeoDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialogBody
import com.micsbol.telecon4esp32.ui.components.NeoDialogTextAction
import com.micsbol.telecon4esp32.ui.components.NeoDialogTitle
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.components.NeoSecondaryButton
import com.micsbol.telecon4esp32.ui.components.safeHudPadding
import com.micsbol.telecon4esp32.ui.control_panel.components.ControlPanelOverlayControls
import com.micsbol.telecon4esp32.ui.home.LocalFirstConnectionTutorial
import com.micsbol.telecon4esp32.ui.home.TutorialAnchor
import com.micsbol.telecon4esp32.ui.home.reportTutorialAnchor
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.micsbol.telecon4esp32.BuildConfig
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.AnalogChannelHistory
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.domain.bluetooth.TelemetryChannelRouter
import com.micsbol.telecon4esp32.domain.bluetooth.TelemetryState
import com.micsbol.telecon4esp32.domain.bluetooth.IndicatorState
import com.micsbol.telecon4esp32.domain.bluetooth.PanelState
import com.micsbol.telecon4esp32.domain.model.ChannelRouting
import com.micsbol.telecon4esp32.domain.model.PremiumFeature
import com.micsbol.telecon4esp32.domain.model.PlotCalibration
import com.micsbol.telecon4esp32.domain.model.TelemetryChannel
import com.micsbol.telecon4esp32.domain.model.TelemetrySink
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.domain.model.canUseControlPanelCenterExtras
import com.micsbol.telecon4esp32.domain.model.canUseControlPanelRadar
import com.micsbol.telecon4esp32.domain.model.canUseControlPanelSessionCsv
import com.micsbol.telecon4esp32.domain.model.canUseControlPanelStick
import com.micsbol.telecon4esp32.domain.model.isUnlocked
import com.micsbol.telecon4esp32.domain.model.usesCoinEconomy
import com.micsbol.telecon4esp32.ui.bluetooth.ApplicationBluetoothSessionUi
import com.micsbol.telecon4esp32.ui.bluetooth.BluetoothConnectionErrorDialog
import com.micsbol.telecon4esp32.ui.bluetooth.BluetoothViewModel
import com.micsbol.telecon4esp32.ui.bluetooth.LocalApplicationBluetoothSession
import com.micsbol.telecon4esp32.ui.bluetooth.SessionRecordingUiState
import com.micsbol.telecon4esp32.ui.bluetooth.rememberSoftApConnectAction
import com.micsbol.telecon4esp32.domain.model.ButtonEvent
import com.micsbol.telecon4esp32.ui.bluetooth.RcControlState
import com.micsbol.telecon4esp32.ui.control_panel.components.AnalogIndicator
import com.micsbol.telecon4esp32.ui.control_panel.components.BatteryStatus
import com.micsbol.telecon4esp32.ui.control_panel.components.ButtonSide
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.model.JoystickRangeShape
import com.micsbol.telecon4esp32.domain.model.KnobChannelLink
import com.micsbol.telecon4esp32.domain.model.StickChannelLink
import com.micsbol.telecon4esp32.ui.ads.InterstitialTrigger
import com.micsbol.telecon4esp32.ui.ads.rememberNavigateWithInterstitial
import com.micsbol.telecon4esp32.ui.entitlement.LocalEntitlement
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.navigation.modulesHubRoute
import com.micsbol.telecon4esp32.ui.bluetooth.BluetoothUiState
import com.micsbol.telecon4esp32.ui.rc_settings.SettingsUiState
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme
import com.micsbol.telecon4esp32.ui.wallet.LocalWallet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@SuppressLint("RestrictedApi", "UnusedBoxWithConstraintsScope")
@Composable
fun ControlPanelScreen(
    bluetoothViewModel: BluetoothViewModel? = null,
    telemetryState: StateFlow<TelemetryState>? = null,
    userSettings: StateFlow<SettingsUiState>? = null,
    rcControlState: StateFlow<RcControlState>? = null,
    navController: NavHostController? = null,
    cameraStreamViewModel: ControlPanelCameraStreamViewModel? = null,
) {
    LockHudLandscape()
    val actualViewModel = bluetoothViewModel
    val actualTelemetryState = telemetryState ?: bluetoothViewModel?.telemetryState ?: MutableStateFlow(TelemetryState())
    val actualUserSettings = userSettings ?: bluetoothViewModel?.userSettings ?: MutableStateFlow(SettingsUiState.Loading)
    val actualRcControlState = rcControlState ?: bluetoothViewModel?.rcControlState ?: MutableStateFlow(RcControlState())

    if (actualViewModel == null && telemetryState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {}
        return
    }

    val exitRcToHome = rememberNavigateWithInterstitial(
        trigger = InterstitialTrigger.RC_EXIT,
        onNavigate = {
            actualViewModel?.stopSendingRcData()
            navController?.popBackStack(Screen.Home.route, inclusive = false)
        },
    )

    BackHandler(enabled = navController != null) {
        exitRcToHome()
    }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity ?: return@LaunchedEffect
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb())
        )
    }

    val collectedUserSettings by actualUserSettings.collectAsState()
    val settingsSyncGeneration by (bluetoothViewModel?.rcSettingsSyncGeneration
        ?: MutableStateFlow(0)).collectAsState()
    val bluetoothConnectionState by (bluetoothViewModel?.state
        ?: MutableStateFlow(BluetoothUiState())).collectAsState()
    val controlPanelConnectionMode by (bluetoothViewModel?.controlPanelConnectionMode
        ?: MutableStateFlow(BluetoothConnectionMode.CLASSIC_SIMPLE)).collectAsState()
    val controlPanelTransportType = controlPanelConnectionMode.transport
    val isConnectedForControlPanel =
        bluetoothConnectionState.isConnected &&
            bluetoothConnectionState.activeSession?.applicationId == ApplicationId.CONTROL_PANEL

    val softApConnect = rememberSoftApConnectAction(
        onConnect = {
            val wifiMode = BluetoothConnectionMode.WIFI_BINARY
            actualViewModel?.dismissError()
            actualViewModel?.requestApplicationConnection(
                applicationId = ApplicationId.CONTROL_PANEL,
                protocolMode = wifiMode.protocolMode,
                transport = BluetoothTransportType.WIFI,
                connectionMode = wifiMode,
            )
            actualViewModel?.connectToWifiSoftAp()
        },
    )

    // Shows RC:CONNECT handshake failures (proto/app mismatch or timeout) after Classic/BLE connect.
    // Telemetry UI is identical for Classic Simple, Classic Binary, and BLE — bench/simulate
    // echo (sticks→panels/plots) is firmware-side; see CONTROL_PANEL_DEBUG_PARITY_ESP32_PROMPT.md.
    BluetoothConnectionErrorDialog(
        handshakeFailure = bluetoothConnectionState.handshakeFailure,
        connectFailure = bluetoothConnectionState.connectFailure,
        errorMessage = bluetoothConnectionState.errorMessage,
        onDismiss = { bluetoothViewModel?.dismissError() },
    )

    when (val state = collectedUserSettings) {
        is SettingsUiState.Loading -> {
            CircularProgressIndicator()
        }

        is SettingsUiState.Error -> {
            Text("Error: ${state.message}")
        }

        is SettingsUiState.Success -> {
            val plotLabels by (bluetoothViewModel?.rcPlotDisplayLabels
                ?: MutableStateFlow(state.settings.plotLabels)).collectAsState()
            val plotCalibrations by (bluetoothViewModel?.rcPlotCalibrations
                ?: MutableStateFlow(PlotCalibration.padded(state.settings.plotCalibrations)))
                .collectAsState()
            val channelRouting by (bluetoothViewModel?.rcChannelRouting
                ?: MutableStateFlow(state.settings.channelRouting)).collectAsState()
            val sessionRecording by (bluetoothViewModel?.sessionRecording
                ?: MutableStateFlow(SessionRecordingUiState())).collectAsState()
            val isVideoRecording by (cameraStreamViewModel?.isVideoRecording
                ?: remember { MutableStateFlow(false) }).collectAsState()
            val videoExport by (
                cameraStreamViewModel?.videoExport
                    ?: remember {
                        MutableStateFlow<ControlPanelCameraStreamViewModel.CameraVideoExport?>(null)
                    }
                ).collectAsState()
            val stillResult by (
                cameraStreamViewModel?.stillResult
                    ?: remember {
                        MutableStateFlow<ControlPanelCameraStreamViewModel.CameraStillResult?>(null)
                    }
                ).collectAsState()
            var csvShareFailed by remember { mutableStateOf(false) }
            var csvSaveFailed by remember { mutableStateOf(false) }
            val entitlement = LocalEntitlement.current
            val wallet = LocalWallet.current
            val localCenterMode = remember { MutableStateFlow(ControlPanelCenterMode.PLOTS) }
            val centerMode by (cameraStreamViewModel?.centerMode ?: localCenterMode).collectAsState()
            val onCenterModeSelected: (ControlPanelCenterMode) -> Unit = { mode ->
                cameraStreamViewModel?.onCenterModeSelected(mode)
                    ?: run { localCenterMode.value = mode }
            }
            var unlockFeature by remember { mutableStateOf<PremiumFeature?>(null) }
            // Match Home/Catalog: debug Premium does not auto-enable Pro center extras.
            val requiresCoinEntry = entitlement.usesCoinEconomy() || BuildConfig.DEBUG
            val cameraUnlocked = remember(entitlement, wallet, requiresCoinEntry) {
                entitlement.canUseControlPanelCenterExtras(
                    wallet = wallet,
                    requiresCoinEntry = requiresCoinEntry,
                )
            }
            val radarUnlocked = remember(entitlement, wallet, requiresCoinEntry) {
                entitlement.canUseControlPanelRadar(
                    wallet = wallet,
                    requiresCoinEntry = requiresCoinEntry,
                )
            }
            val stickUnlocked = remember(entitlement, wallet, requiresCoinEntry) {
                entitlement.canUseControlPanelStick(
                    wallet = wallet,
                    requiresCoinEntry = requiresCoinEntry,
                )
            }
            val sessionCsvUnlocked = remember(entitlement, wallet, requiresCoinEntry) {
                entitlement.canUseControlPanelSessionCsv(
                    wallet = wallet,
                    requiresCoinEntry = requiresCoinEntry,
                )
            }
            val isCenterModeUnlocked: (ControlPanelCenterMode) -> Boolean =
                remember(cameraUnlocked, radarUnlocked, stickUnlocked) {
                    { mode ->
                        mode.isUnlocked(
                            cameraUnlocked = cameraUnlocked,
                            radarUnlocked = radarUnlocked,
                            stickUnlocked = stickUnlocked,
                        )
                    }
                }
            LaunchedEffect(centerMode, cameraUnlocked) {
                if (centerMode == ControlPanelCenterMode.CAMERA && !cameraUnlocked) {
                    cameraStreamViewModel?.onCenterModeSelected(
                        mode = centerMode,
                        engageCameraHardware = false,
                        toggleCamera = false,
                    )
                }
                val plotsShown = centerMode == ControlPanelCenterMode.PLOTS
                val cameraShown = centerMode == ControlPanelCenterMode.CAMERA && cameraUnlocked
                if (!plotsShown && sessionRecording.isRecording) {
                    actualViewModel?.toggleSessionRecording()
                }
                if (!cameraShown && isVideoRecording) {
                    cameraStreamViewModel?.stopVideoRecording()
                }
            }

            LaunchedEffect(actualViewModel) {
                actualViewModel?.onControlPanelEntered()
                actualViewModel?.markRecentApplication(ApplicationId.CONTROL_PANEL)
            }

            DisposableEffect(actualViewModel) {
                onDispose {
                    actualViewModel?.stopSendingRcData()
                }
            }

            if (navController != null) {
                val unlockTitle = unlockFeature?.let { feature ->
                    stringResource(
                        when (feature) {
                            PremiumFeature.CONTROL_PANEL_CENTER_EXTRAS ->
                                R.string.control_panel_center_camera_title
                            PremiumFeature.CONTROL_PANEL_RADAR ->
                                R.string.control_panel_center_radar_title
                            PremiumFeature.CONTROL_PANEL_STICK ->
                                R.string.control_panel_center_stick_title
                            PremiumFeature.CONTROL_PANEL_SESSION_CSV ->
                                R.string.control_panel_session_csv_unlock_title
                            else -> R.string.app_control_panel_title
                        },
                    )
                }.orEmpty()
                ControlPanelCenterFeatureUnlockDialogs(
                    unlockFeature = unlockFeature,
                    featureTitle = unlockTitle,
                    navController = navController,
                    onDismiss = { unlockFeature = null },
                    onUnlocked = {
                        if (unlockFeature == PremiumFeature.CONTROL_PANEL_SESSION_CSV) {
                            if (sessionRecording.isRecording.not()) {
                                actualViewModel?.toggleSessionRecording()
                            }
                            return@ControlPanelCenterFeatureUnlockDialogs
                        }
                        val unlockedMode = ControlPanelCenterMode.entries.firstOrNull { mode ->
                            mode.premiumFeature == unlockFeature
                        }
                        if (unlockedMode != null) {
                            cameraStreamViewModel?.onCenterModeSelected(
                                mode = unlockedMode,
                                toggleCamera = false,
                            ) ?: onCenterModeSelected(unlockedMode)
                        }
                    },
                )
            }

            when (val export = videoExport) {
                is ControlPanelCameraStreamViewModel.CameraVideoExport.Saved -> NeoDialog(
                    onDismissRequest = { cameraStreamViewModel?.dismissVideoExport() },
                    title = {
                        NeoDialogTitle(text = stringResource(R.string.control_panel_video_saved))
                    },
                    subtitle = {
                        NeoDialogBody(
                            text = stringResource(
                                R.string.control_panel_video_saved_message,
                                export.fileName,
                                export.location,
                            ),
                        )
                    },
                    actions = {
                        NeoPillButton(
                            text = stringResource(R.string.codes_dialog_ok),
                            onClick = { cameraStreamViewModel?.dismissVideoExport() },
                        )
                    },
                )
                ControlPanelCameraStreamViewModel.CameraVideoExport.Failed -> NeoDialog(
                    onDismissRequest = { cameraStreamViewModel?.dismissVideoExport() },
                    title = {
                        NeoDialogTitle(text = stringResource(R.string.control_panel_video_save_failed_title))
                    },
                    subtitle = {
                        NeoDialogBody(
                            text = stringResource(R.string.control_panel_video_save_failed_message),
                        )
                    },
                    actions = {
                        NeoPillButton(
                            text = stringResource(R.string.codes_dialog_ok),
                            onClick = { cameraStreamViewModel?.dismissVideoExport() },
                        )
                    },
                )
                null -> Unit
            }

            when (val still = stillResult) {
                is ControlPanelCameraStreamViewModel.CameraStillResult.Saved -> NeoDialog(
                    onDismissRequest = { cameraStreamViewModel?.dismissStillResult() },
                    title = {
                        NeoDialogTitle(text = stringResource(R.string.control_panel_photo_saved))
                    },
                    subtitle = {
                        NeoDialogBody(
                            text = stringResource(
                                R.string.control_panel_photo_saved_message,
                                still.fileName,
                                still.location,
                            ),
                        )
                    },
                    actions = {
                        NeoPillButton(
                            text = stringResource(R.string.codes_dialog_ok),
                            onClick = { cameraStreamViewModel?.dismissStillResult() },
                        )
                    },
                )
                ControlPanelCameraStreamViewModel.CameraStillResult.NoFrame -> NeoDialog(
                    onDismissRequest = { cameraStreamViewModel?.dismissStillResult() },
                    title = {
                        NeoDialogTitle(text = stringResource(R.string.control_panel_photo_no_frame))
                    },
                    actions = {
                        NeoPillButton(
                            text = stringResource(R.string.codes_dialog_ok),
                            onClick = { cameraStreamViewModel?.dismissStillResult() },
                        )
                    },
                )
                ControlPanelCameraStreamViewModel.CameraStillResult.Failed -> NeoDialog(
                    onDismissRequest = { cameraStreamViewModel?.dismissStillResult() },
                    title = {
                        NeoDialogTitle(text = stringResource(R.string.control_panel_photo_failed))
                    },
                    actions = {
                        NeoPillButton(
                            text = stringResource(R.string.codes_dialog_ok),
                            onClick = { cameraStreamViewModel?.dismissStillResult() },
                        )
                    },
                )
                null -> Unit
            }

            if (sessionRecording.savedLocation.isNotBlank()) {
                NeoDialog(
                    onDismissRequest = { actualViewModel?.dismissFinishedSessionCsv() },
                    title = {
                        NeoDialogTitle(text = stringResource(R.string.control_panel_session_csv_saved))
                    },
                    subtitle = {
                        NeoDialogBody(
                            text = stringResource(
                                R.string.control_panel_session_csv_saved_message,
                                sessionRecording.finishedFileName,
                                sessionRecording.savedLocation,
                            ),
                        )
                    },
                    actions = {
                        NeoPillButton(
                            text = stringResource(R.string.codes_dialog_ok),
                            onClick = { actualViewModel?.dismissFinishedSessionCsv() },
                        )
                    },
                )
            }

            sessionRecording.finishedFile?.let { csvFile ->
                NeoDialog(
                    onDismissRequest = { actualViewModel?.dismissFinishedSessionCsv() },
                    title = {
                        NeoDialogTitle(text = stringResource(R.string.control_panel_session_csv_title))
                    },
                    subtitle = {
                        NeoDialogBody(text = stringResource(R.string.control_panel_session_csv_message))
                    },
                    actions = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            NeoPillButton(
                                text = stringResource(R.string.control_panel_session_csv_share),
                                onClick = {
                                    val shared = shareSessionCsv(context, csvFile)
                                    if (shared) {
                                        actualViewModel?.dismissFinishedSessionCsv()
                                    } else {
                                        csvShareFailed = true
                                    }
                                },
                                fillMaxWidth = true,
                            )
                            NeoSecondaryButton(
                                text = stringResource(R.string.control_panel_session_csv_save),
                                onClick = {
                                    actualViewModel?.saveFinishedSessionCsvToDownloads { ok ->
                                        if (!ok) csvSaveFailed = true
                                    }
                                },
                                fillMaxWidth = true,
                            )
                            NeoDialogTextAction(
                                text = stringResource(R.string.codes_pdf_cancel),
                                onClick = { actualViewModel?.dismissFinishedSessionCsv() },
                            )
                        }
                    },
                )
            }

            if (csvSaveFailed) {
                NeoDialog(
                    onDismissRequest = { csvSaveFailed = false },
                    title = {
                        NeoDialogTitle(
                            text = stringResource(R.string.control_panel_session_csv_save_failed_title),
                        )
                    },
                    subtitle = {
                        NeoDialogBody(
                            text = stringResource(R.string.control_panel_session_csv_save_failed_message),
                        )
                    },
                    actions = {
                        NeoPillButton(
                            text = stringResource(R.string.codes_dialog_ok),
                            onClick = { csvSaveFailed = false },
                        )
                    },
                )
            }

            if (csvShareFailed) {
                NeoDialog(
                    onDismissRequest = { csvShareFailed = false },
                    title = {
                        NeoDialogTitle(
                            text = stringResource(R.string.control_panel_session_csv_share_no_app_title),
                        )
                    },
                    subtitle = {
                        NeoDialogBody(
                            text = stringResource(R.string.control_panel_session_csv_share_no_app_message),
                        )
                    },
                    actions = {
                        NeoPillButton(
                            text = stringResource(R.string.codes_dialog_ok),
                            onClick = { csvShareFailed = false },
                        )
                    },
                )
            }

            val onOpenBluetooth: () -> Unit = remember(
                actualViewModel,
                navController,
                controlPanelConnectionMode,
                softApConnect,
            ) {
                {
                    if (controlPanelConnectionMode.isWifiLink) {
                        // SoftAP: in-app local-only join (API 29+) or system Wi‑Fi settings.
                        softApConnect()
                    } else {
                        // Passes Classic Simple so RC:CONNECT,proto,simple matches ESP32-TC-RC-BT-Simple.
                        actualViewModel?.dismissError()
                        actualViewModel?.requestApplicationConnection(
                            applicationId = ApplicationId.CONTROL_PANEL,
                            protocolMode = controlPanelConnectionMode.protocolMode,
                            transport = controlPanelConnectionMode.transport,
                            connectionMode = controlPanelConnectionMode,
                        )
                        actualViewModel?.preparePostConnectPopBack()
                        if (navController != null) {
                            navController.navigate(Screen.Bluetooth.route)
                        }
                    }
                }
            }

            CompositionLocalProvider(
                LocalApplicationBluetoothSession provides ApplicationBluetoothSessionUi(
                    isConnected = isConnectedForControlPanel,
                    isConnecting = bluetoothConnectionState.isConnecting,
                    transport = controlPanelConnectionMode.transport,
                    onConnect = onOpenBluetooth,
                ),
            ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.plastic_background),
                    contentDescription = "Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (navController != null) {
                    LiveControlBluetoothDisconnectedBannerOverlay(
                        visible = !isConnectedForControlPanel &&
                            !bluetoothConnectionState.isConnecting,
                        onClick = onOpenBluetooth,
                    )
                }

                // Insets applied first so layout metrics use the real usable HUD area.
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeHudPadding(
                            includeTop = true,
                            includeBottom = true,
                            includeHorizontal = true,
                        ),
                ) {
                    val contentWidth = maxWidth
                    LaunchedEffect(contentWidth, maxHeight) {
                        Log.d(
                            "DeviceMetrics",
                            "ControlPanel content: ${contentWidth}x${maxHeight}",
                        )
                    }

                    // ── Stable callbacks ───────────────────────────────────────────────────
                    // remember(bluetoothViewModel) means the same lambda object is reused on
                    // every recomposition, allowing Compose to skip ControllerSide entirely
                    // when only the joystick position (which is NOT a ControllerSide param)
                    // changes.
                    val onLeftMove: (Float, Float) -> Unit = remember(bluetoothViewModel) {
                        { x, y -> bluetoothViewModel?.onLeftStickChanged(x, y) }
                    }
                    val onRightMove: (Float, Float) -> Unit = remember(bluetoothViewModel) {
                        { x, y -> bluetoothViewModel?.onRightStickChanged(x, y) }
                    }
                    val onLeftSwitchChange: (Int, Boolean) -> Unit = remember(bluetoothViewModel) {
                        { idx, v -> bluetoothViewModel?.onLeftSwitchChanged(idx, v) }
                    }
                    val onRightSwitchChange: (Int, Boolean) -> Unit = remember(bluetoothViewModel) {
                        { idx, v -> bluetoothViewModel?.onRightSwitchChanged(idx, v) }
                    }
                    val onLeftKnobChange: (Float) -> Unit = remember(bluetoothViewModel) {
                        { v -> bluetoothViewModel?.onLeftKnobChanged(v) }
                    }
                    val onRightKnobChange: (Float) -> Unit = remember(bluetoothViewModel) {
                        { v -> bluetoothViewModel?.onRightKnobChanged(v) }
                    }
                    val onTopLeftPress: () -> Unit = remember(bluetoothViewModel) {
                        { bluetoothViewModel?.sendButtonEvent(ButtonEvent.CENTER_TOP_LEFT) }
                    }
                    val onBottomLeftPress: () -> Unit = remember(bluetoothViewModel) {
                        { bluetoothViewModel?.sendButtonEvent(ButtonEvent.CENTER_BOTTOM_LEFT) }
                    }
                    val onTopRightPress: () -> Unit = remember(bluetoothViewModel) {
                        { bluetoothViewModel?.sendButtonEvent(ButtonEvent.CENTER_TOP_RIGHT) }
                    }
                    val onBottomRightPress: () -> Unit = remember(bluetoothViewModel) {
                        { bluetoothViewModel?.sendButtonEvent(ButtonEvent.CENTER_BOTTOM_RIGHT) }
                    }

                    // Row keeps plot/center recompositions isolated from the side controller trees.
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            ControlPanelLeftControllerHost(
                                bluetoothViewModel = bluetoothViewModel,
                                telemetryState = actualTelemetryState,
                                rcControlState = actualRcControlState,
                                settings = state.settings,
                                contentWidth = contentWidth,
                                settingsSyncGeneration = settingsSyncGeneration,
                                onMove = onLeftMove,
                                onSwitchStateChange = onLeftSwitchChange,
                                onKnobValueChange = onLeftKnobChange,
                                onTopPress = onTopLeftPress,
                                onBottomPress = onBottomLeftPress,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        ) {
                            val centerOverlay: @Composable () -> Unit = {
                                if (navController != null) {
                                    ControlPanelOverlayControls(
                                        isBluetoothConnected = isConnectedForControlPanel,
                                        isBluetoothConnecting = bluetoothConnectionState.isConnecting,
                                        usesWifiLink = controlPanelTransportType ==
                                            com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType.WIFI,
                                        onBackToModulesClick = {
                                            actualViewModel?.stopSendingRcData()
                                            navController.navigate(modulesHubRoute()) {
                                                popUpTo(Screen.Home.route) {
                                                    inclusive = false
                                                }
                                                launchSingleTop = true
                                            }
                                        },
                                        onConnectionSettingsClick = {
                                            navController.navigateToApplicationSettings(
                                                ApplicationId.CONTROL_PANEL,
                                                ApplicationSettingsSection.CONNECTION,
                                            )
                                        },
                                        onBluetoothDisconnectedClick = onOpenBluetooth,
                                        centerMode = centerMode,
                                        isModeUnlocked = isCenterModeUnlocked,
                                        onCenterModeClick = { mode ->
                                            if (isCenterModeUnlocked(mode)) {
                                                onCenterModeSelected(mode)
                                            } else {
                                                unlockFeature = mode.premiumFeature
                                            }
                                        },
                                        onCenterModeScroll = { mode ->
                                            cameraStreamViewModel?.onCenterModeSelected(
                                                mode = mode,
                                                engageCameraHardware = isCenterModeUnlocked(mode),
                                                toggleCamera = false,
                                            ) ?: run { onCenterModeSelected(mode) }
                                        },
                                        isSessionRecording = sessionRecording.isRecording,
                                        isSessionRecordingUnlocked = sessionCsvUnlocked,
                                        onToggleSessionRecording = {
                                            if (sessionRecording.isRecording || sessionCsvUnlocked) {
                                                actualViewModel?.toggleSessionRecording()
                                            } else {
                                                unlockFeature = PremiumFeature.CONTROL_PANEL_SESSION_CSV
                                            }
                                        },
                                        isVideoRecording = isVideoRecording,
                                        onToggleVideoRecording = {
                                            cameraStreamViewModel?.toggleVideoRecording()
                                        },
                                        onTakePicture = {
                                            cameraStreamViewModel?.capturePhoto()
                                        },
                                    )
                                }
                            }
                            ControlPanelCenterPlotHost(
                                bluetoothViewModel = bluetoothViewModel,
                                telemetryState = actualTelemetryState,
                                rcControlState = actualRcControlState,
                                plotLabels = plotLabels,
                                plotCalibrations = plotCalibrations,
                                channelRouting = channelRouting,
                                settingsSyncGeneration = settingsSyncGeneration,
                                centerMode = centerMode,
                                centerModeUnlocked = isCenterModeUnlocked(centerMode),
                                onUnlockCenterMode = {
                                    unlockFeature = centerMode.premiumFeature
                                },
                                leftStickMode = state.settings.leftStickMode,
                                rightStickMode = state.settings.rightStickMode,
                                modifier = Modifier.fillMaxSize(),
                                topStartOverlay = centerOverlay,
                            )
                        }
                        Box(
                            modifier = Modifier.fillMaxHeight(),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            ControlPanelRightControllerHost(
                                bluetoothViewModel = bluetoothViewModel,
                                telemetryState = actualTelemetryState,
                                rcControlState = actualRcControlState,
                                settings = state.settings,
                                contentWidth = contentWidth,
                                settingsSyncGeneration = settingsSyncGeneration,
                                onMove = onRightMove,
                                onSwitchStateChange = onRightSwitchChange,
                                onKnobValueChange = onRightKnobChange,
                                onTopPress = onTopRightPress,
                                onBottomPress = onBottomRightPress,
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun ControlPanelCenterPlotHost(
    bluetoothViewModel: BluetoothViewModel?,
    telemetryState: StateFlow<TelemetryState>,
    rcControlState: StateFlow<RcControlState>,
    plotLabels: List<String>,
    plotCalibrations: List<PlotCalibration>,
    channelRouting: ChannelRouting,
    settingsSyncGeneration: Int = 0,
    centerMode: ControlPanelCenterMode = ControlPanelCenterMode.PLOTS,
    centerModeUnlocked: Boolean = true,
    onUnlockCenterMode: () -> Unit = {},
    leftStickMode: JoystickMode = JoystickMode.Spring(),
    rightStickMode: JoystickMode = JoystickMode.Spring(),
    modifier: Modifier = Modifier,
    topStartOverlay: @Composable () -> Unit = {},
) {
    val rcState by rcControlState.collectAsState()
    // Center display is two panes × two traces = four plot widgets.
    // Analog bus on the wire is CH1…CH8 (`v0`…`v7` / CC 33 count up to 8).
    if (bluetoothViewModel != null) {
        val plotUi by bluetoothViewModel.rcPlotUiState.collectAsState()
        val displaySeries = remember(plotUi.series, plotLabels, settingsSyncGeneration) {
            fourPlotSeriesForDisplay(plotUi.series, plotLabels)
        }
        PersistedControlPanelCenterPlot(
            series = displaySeries,
            radarSeries = plotUi.radarSeries,
            plotRevision = plotUi.revision,
            plotCalibrations = plotCalibrations,
            channelRouting = channelRouting,
            onRadarSourceChange = bluetoothViewModel::saveChannelBinding,
            onPlotLabelChange = bluetoothViewModel::savePlotLabel,
            onPlotChannelChange = { index, channel ->
                bluetoothViewModel.saveChannelBinding(TelemetrySink.plotAt(index), channel)
            },
            onPlotCalibrationChange = bluetoothViewModel::savePlotCalibration,
            centerMode = centerMode,
            centerModeUnlocked = centerModeUnlocked,
            onUnlockCenterMode = onUnlockCenterMode,
            leftStickXy = rcState.leftStickPosition,
            rightStickXy = rcState.rightStickPosition,
            leftStickMode = leftStickMode,
            rightStickMode = rightStickMode,
            leftKnobValue = rcState.leftKnobValue,
            rightKnobValue = rcState.rightKnobValue,
            modifier = modifier,
            topStartOverlay = topStartOverlay,
        )
    } else {
        val telemetry by telemetryState.collectAsState()
        val analogHistory = remember { AnalogChannelHistory() }
        val routed = remember(telemetry, channelRouting, plotLabels, settingsSyncGeneration) {
            analogHistory.ingest(telemetry)
            RoutedPreviewPlots(
                series = fourPlotSeriesForDisplay(
                    TelemetryChannelRouter.plotSeries(
                        telemetry = telemetry,
                        routing = channelRouting,
                        analogHistory = analogHistory,
                        plotLabels = plotLabels,
                    ),
                    plotLabels,
                ),
                radarSeries = TelemetryChannelRouter.radarSeries(telemetry, analogHistory),
                revision = telemetry.plotState.revision,
            )
        }
        ControlPanelCenterPlot(
            series = routed.series,
            radarSeries = routed.radarSeries,
            plotRevision = routed.revision,
            plotCalibrations = plotCalibrations,
            channelRouting = channelRouting,
            centerMode = centerMode,
            centerModeUnlocked = centerModeUnlocked,
            onUnlockCenterMode = onUnlockCenterMode,
            leftStickXy = rcState.leftStickPosition,
            rightStickXy = rcState.rightStickPosition,
            leftStickMode = leftStickMode,
            rightStickMode = rightStickMode,
            leftKnobValue = rcState.leftKnobValue,
            rightKnobValue = rcState.rightKnobValue,
            modifier = modifier,
            topStartOverlay = topStartOverlay,
        )
    }
}

private data class RoutedPreviewPlots(
    val series: List<PlotData>,
    val radarSeries: List<PlotData>,
    val revision: Long,
)

/** Exactly [UserSettings.PLOT_LABEL_COUNT] series for the dual-pane Cartesian plot. */
private fun fourPlotSeriesForDisplay(
    telemetrySeries: List<PlotData>,
    plotLabels: List<String>,
): List<PlotData> = plotSeriesForDisplay(telemetrySeries, plotLabels)
    .take(UserSettings.PLOT_LABEL_COUNT)

@Composable
private fun ControlPanelCenterPlot(
    series: List<PlotData>,
    radarSeries: List<PlotData> = emptyList(),
    plotRevision: Long,
    plotCalibrations: List<PlotCalibration> = PlotCalibration.defaults(),
    channelRouting: ChannelRouting = ChannelRouting.defaults(),
    onRadarSourceChange: (TelemetrySink, TelemetryChannel) -> Unit = { _, _ -> },
    onPlotLabelChange: (Int, String) -> Unit = { _, _ -> },
    onPlotChannelChange: (Int, TelemetryChannel) -> Unit = { _, _ -> },
    onPlotCalibrationChange: (Int, PlotCalibration) -> Unit = { _, _ -> },
    centerMode: ControlPanelCenterMode = ControlPanelCenterMode.PLOTS,
    centerModeUnlocked: Boolean = true,
    onUnlockCenterMode: () -> Unit = {},
    leftStickXy: Pair<Float, Float> = Pair(0f, 0f),
    rightStickXy: Pair<Float, Float> = Pair(0f, 0f),
    leftStickMode: JoystickMode = JoystickMode.Spring(),
    rightStickMode: JoystickMode = JoystickMode.Spring(),
    leftKnobValue: Float = 0.5f,
    rightKnobValue: Float = 0.5f,
    modifier: Modifier = Modifier,
    topStartOverlay: @Composable () -> Unit = {},
    displaySettings: ControlPanelPlotDisplaySettings? = null,
    onDisplaySettingsChange: (ControlPanelPlotDisplaySettings) -> Unit = {},
) {
    CenterDisplay(
        modifier = modifier,
        // Indices 0–1 → top pane; 2–3 → bottom pane (see CartesianPlot).
        series = series,
        radarSeries = radarSeries,
        plotRevision = plotRevision,
        plotCalibrations = plotCalibrations,
        channelRouting = channelRouting,
        onRadarSourceChange = onRadarSourceChange,
        onPlotLabelChange = onPlotLabelChange,
        onPlotChannelChange = onPlotChannelChange,
        onPlotCalibrationChange = onPlotCalibrationChange,
        centerMode = centerMode,
        centerModeUnlocked = centerModeUnlocked,
        onUnlockCenterMode = onUnlockCenterMode,
        leftStickXy = leftStickXy,
        rightStickXy = rightStickXy,
        leftStickMode = leftStickMode,
        rightStickMode = rightStickMode,
        leftKnobValue = leftKnobValue,
        rightKnobValue = rightKnobValue,
        topStartOverlay = topStartOverlay,
        displaySettings = displaySettings,
        onDisplaySettingsChange = onDisplaySettingsChange,
    )
}

@Composable
private fun PersistedControlPanelCenterPlot(
    series: List<PlotData>,
    radarSeries: List<PlotData> = emptyList(),
    plotRevision: Long,
    plotCalibrations: List<PlotCalibration> = PlotCalibration.defaults(),
    channelRouting: ChannelRouting = ChannelRouting.defaults(),
    onRadarSourceChange: (TelemetrySink, TelemetryChannel) -> Unit = { _, _ -> },
    onPlotLabelChange: (Int, String) -> Unit = { _, _ -> },
    onPlotChannelChange: (Int, TelemetryChannel) -> Unit = { _, _ -> },
    onPlotCalibrationChange: (Int, PlotCalibration) -> Unit = { _, _ -> },
    centerMode: ControlPanelCenterMode = ControlPanelCenterMode.PLOTS,
    centerModeUnlocked: Boolean = true,
    onUnlockCenterMode: () -> Unit = {},
    leftStickXy: Pair<Float, Float> = Pair(0f, 0f),
    rightStickXy: Pair<Float, Float> = Pair(0f, 0f),
    leftStickMode: JoystickMode = JoystickMode.Spring(),
    rightStickMode: JoystickMode = JoystickMode.Spring(),
    leftKnobValue: Float = 0.5f,
    rightKnobValue: Float = 0.5f,
    modifier: Modifier = Modifier,
    topStartOverlay: @Composable () -> Unit = {},
) {
    val displayVm: ControlPanelPlotDisplayViewModel = hiltViewModel()
    val displaySettings by displayVm.settings.collectAsState()
    ControlPanelCenterPlot(
        series = series,
        radarSeries = radarSeries,
        plotRevision = plotRevision,
        plotCalibrations = plotCalibrations,
        channelRouting = channelRouting,
        onRadarSourceChange = onRadarSourceChange,
        onPlotLabelChange = onPlotLabelChange,
        onPlotChannelChange = onPlotChannelChange,
        onPlotCalibrationChange = onPlotCalibrationChange,
        centerMode = centerMode,
        centerModeUnlocked = centerModeUnlocked,
        onUnlockCenterMode = onUnlockCenterMode,
        leftStickXy = leftStickXy,
        rightStickXy = rightStickXy,
        leftStickMode = leftStickMode,
        rightStickMode = rightStickMode,
        leftKnobValue = leftKnobValue,
        rightKnobValue = rightKnobValue,
        modifier = modifier,
        topStartOverlay = topStartOverlay,
        displaySettings = displaySettings,
        onDisplaySettingsChange = displayVm::save,
    )
}

@Composable
private fun ControlPanelLeftControllerHost(
    bluetoothViewModel: BluetoothViewModel?,
    telemetryState: StateFlow<TelemetryState>,
    rcControlState: StateFlow<RcControlState>,
    settings: UserSettings,
    contentWidth: Dp,
    settingsSyncGeneration: Int,
    onMove: (Float, Float) -> Unit,
    onSwitchStateChange: (Int, Boolean) -> Unit,
    onKnobValueChange: (Float) -> Unit,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
) {
    if (bluetoothViewModel != null) {
        ControlPanelLeftControllerConnected(
            bluetoothViewModel = bluetoothViewModel,
            settings = settings,
            contentWidth = contentWidth,
            settingsSyncGeneration = settingsSyncGeneration,
            onMove = onMove,
            onSwitchStateChange = onSwitchStateChange,
            onKnobValueChange = onKnobValueChange,
            onTopPress = onTopPress,
            onBottomPress = onBottomPress,
        )
    } else {
        val rcState by rcControlState.collectAsState()
        val telemetry by telemetryState.collectAsState()
        val sideTelemetry = telemetry.toLeftSideTelemetry().copy(
            panelNumber = TelemetryChannelRouter.panelLeft(telemetry, settings.channelRouting),
            panelOn = settings.leftPanelOn,
            panelColorArgb = UserSettings.panelColorArgb(settings.leftPanelColorGreen),
            ledValues = TelemetryChannelRouter.ledByte(telemetry, settings.channelRouting),
        )
        val indicator by remember {
            derivedStateOf {
                SideIndicatorUi(
                    value = TelemetryChannelRouter.analogGaugeU8(
                        telemetry,
                        settings.channelRouting,
                    ),
                    title = telemetry.indicatorState.analogTitle,
                )
            }
        }
        ControlPanelLeftController(
            settings = settings,
            contentWidth = contentWidth,
            settingsSyncGeneration = settingsSyncGeneration,
            rcState = rcState,
            sideTelemetry = sideTelemetry,
            indicator = indicator,
            onMove = onMove,
            onSwitchStateChange = onSwitchStateChange,
            onKnobValueChange = onKnobValueChange,
            onTopPress = onTopPress,
            onBottomPress = onBottomPress,
        )
    }
}

@Composable
private fun ControlPanelLeftControllerConnected(
    bluetoothViewModel: BluetoothViewModel,
    settings: UserSettings,
    contentWidth: Dp,
    settingsSyncGeneration: Int,
    onMove: (Float, Float) -> Unit,
    onSwitchStateChange: (Int, Boolean) -> Unit,
    onKnobValueChange: (Float) -> Unit,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
) {
    ControllerSideLayout(
        side = ButtonSide.LEFT,
        contentWidth = contentWidth,
        modifier = Modifier.fillMaxHeight(),
    ) { metrics ->
        Column(modifier = Modifier.fillMaxSize()) {
            ControlPanelLeftTelemetryLayer(
                metrics = metrics,
                bluetoothViewModel = bluetoothViewModel,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                ControlPanelLeftControlsLayer(
                    metrics = metrics,
                    bluetoothViewModel = bluetoothViewModel,
                    mode = settings.leftStickMode,
                    settingsSyncGeneration = settingsSyncGeneration,
                    onMove = onMove,
                    onSwitchStateChange = onSwitchStateChange,
                    onKnobValueChange = onKnobValueChange,
                    onTopPress = onTopPress,
                    onBottomPress = onBottomPress,
                    onStickModeChange = { mode ->
                        bluetoothViewModel.saveStickMode(isRightStick = false, mode = mode)
                    },
                    rangeShape = settings.leftStickRangeShape,
                    onRangeShapeChange = { shape ->
                        bluetoothViewModel.saveStickRangeShape(
                            isRightStick = false,
                            shape = shape,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ControlPanelRightControllerHost(
    bluetoothViewModel: BluetoothViewModel?,
    telemetryState: StateFlow<TelemetryState>,
    rcControlState: StateFlow<RcControlState>,
    settings: UserSettings,
    contentWidth: Dp,
    settingsSyncGeneration: Int,
    onMove: (Float, Float) -> Unit,
    onSwitchStateChange: (Int, Boolean) -> Unit,
    onKnobValueChange: (Float) -> Unit,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
) {
    if (bluetoothViewModel != null) {
        ControlPanelRightControllerConnected(
            bluetoothViewModel = bluetoothViewModel,
            settings = settings,
            contentWidth = contentWidth,
            settingsSyncGeneration = settingsSyncGeneration,
            onMove = onMove,
            onSwitchStateChange = onSwitchStateChange,
            onKnobValueChange = onKnobValueChange,
            onTopPress = onTopPress,
            onBottomPress = onBottomPress,
        )
    } else {
        val rcState by rcControlState.collectAsState()
        val telemetry by telemetryState.collectAsState()
        val sideTelemetry = telemetry.toRightSideTelemetry().copy(
            panelNumber = TelemetryChannelRouter.panelRight(telemetry, settings.channelRouting),
            panelOn = settings.rightPanelOn,
            panelColorArgb = UserSettings.panelColorArgb(settings.rightPanelColorGreen),
            ledValues = TelemetryChannelRouter.ledByte(telemetry, settings.channelRouting),
        )
        val indicator by remember {
            derivedStateOf {
                SideIndicatorUi(
                    value = TelemetryChannelRouter.batteryGaugeU8(
                        telemetry,
                        settings.channelRouting,
                    ),
                    title = telemetry.indicatorState.batteryTitle,
                )
            }
        }
        ControlPanelRightController(
            settings = settings,
            contentWidth = contentWidth,
            settingsSyncGeneration = settingsSyncGeneration,
            rcState = rcState,
            sideTelemetry = sideTelemetry,
            indicator = indicator,
            onMove = onMove,
            onSwitchStateChange = onSwitchStateChange,
            onKnobValueChange = onKnobValueChange,
            onTopPress = onTopPress,
            onBottomPress = onBottomPress,
        )
    }
}

@Composable
private fun ControlPanelRightControllerConnected(
    bluetoothViewModel: BluetoothViewModel,
    settings: UserSettings,
    contentWidth: Dp,
    settingsSyncGeneration: Int,
    onMove: (Float, Float) -> Unit,
    onSwitchStateChange: (Int, Boolean) -> Unit,
    onKnobValueChange: (Float) -> Unit,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
) {
    ControllerSideLayout(
        side = ButtonSide.RIGHT,
        contentWidth = contentWidth,
        modifier = Modifier.fillMaxHeight(),
    ) { metrics ->
        Column(modifier = Modifier.fillMaxSize()) {
            ControlPanelRightTelemetryLayer(
                metrics = metrics,
                bluetoothViewModel = bluetoothViewModel,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                ControlPanelRightControlsLayer(
                    metrics = metrics,
                    bluetoothViewModel = bluetoothViewModel,
                    mode = settings.rightStickMode,
                    settingsSyncGeneration = settingsSyncGeneration,
                    onMove = onMove,
                    onSwitchStateChange = onSwitchStateChange,
                    onKnobValueChange = onKnobValueChange,
                    onTopPress = onTopPress,
                    onBottomPress = onBottomPress,
                    onStickModeChange = { mode ->
                        bluetoothViewModel.saveStickMode(isRightStick = true, mode = mode)
                    },
                    rangeShape = settings.rightStickRangeShape,
                    onRangeShapeChange = { shape ->
                        bluetoothViewModel.saveStickRangeShape(
                            isRightStick = true,
                            shape = shape,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ControlPanelLeftTelemetryLayer(
    metrics: ControllerSideLayoutMetrics,
    bluetoothViewModel: BluetoothViewModel,
) {
    val sideTelemetry by bluetoothViewModel.rcLeftSideTelemetry.collectAsState()
    val indicator by bluetoothViewModel.rcLeftIndicator.collectAsState()
    val routing by bluetoothViewModel.rcChannelRouting.collectAsState()
    var panelMenu by remember { mutableStateOf(false) }
    var analogMenu by remember { mutableStateOf(false) }
    ControllerSideTelemetryRow(
        modifier = Modifier.fillMaxWidth(),
        side = ButtonSide.LEFT,
        telemetry = sideTelemetry,
        panelWidth = metrics.panelWidth,
        extraContentSize = metrics.extraContentSize,
        ledSize = metrics.ledSize,
        ledSpacing = metrics.ledSpacing,
        onPanelDoubleTap = { panelMenu = true },
        panelMenu = {
            TelemetryWidgetOptionsMenu(
                expanded = panelMenu,
                sink = TelemetrySink.PANEL_LEFT,
                selectedChannel = routing.sourceFor(TelemetrySink.PANEL_LEFT),
                widgetLabel = sideTelemetry.panelTitle,
                onChannelSelected = { channel ->
                    bluetoothViewModel.saveChannelBinding(TelemetrySink.PANEL_LEFT, channel)
                },
                onLabelChange = { label ->
                    bluetoothViewModel.saveWidgetLabel(TelemetrySink.PANEL_LEFT, label)
                },
                onDismiss = { panelMenu = false },
                panelOn = sideTelemetry.panelOn,
                onPanelOnChange = { isOn ->
                    bluetoothViewModel.saveNumericPanelOn(TelemetrySink.PANEL_LEFT, isOn)
                },
                panelColorGreen = sideTelemetry.panelColorArgb == UserSettings.PANEL_COLOR_GREEN_ARGB,
                onPanelColorGreenChange = { isGreen ->
                    bluetoothViewModel.saveNumericPanelColorGreen(TelemetrySink.PANEL_LEFT, isGreen)
                },
            )
        },
        topExtraContent = { mod ->
            Box {
                AnalogIndicator(
                    modifier = mod,
                    value = indicator.value,
                    title = indicator.title,
                    onDoubleTap = { analogMenu = true },
                )
                TelemetryWidgetOptionsMenu(
                    expanded = analogMenu,
                    sink = TelemetrySink.ANALOG_GAUGE,
                    selectedChannel = routing.sourceFor(TelemetrySink.ANALOG_GAUGE),
                    widgetLabel = indicator.title,
                    onChannelSelected = { channel ->
                        bluetoothViewModel.saveChannelBinding(TelemetrySink.ANALOG_GAUGE, channel)
                    },
                    onLabelChange = { label ->
                        bluetoothViewModel.saveWidgetLabel(TelemetrySink.ANALOG_GAUGE, label)
                    },
                    onDismiss = { analogMenu = false },
                )
            }
        },
    )
}

@Composable
private fun ControlPanelLeftControlsLayer(
    metrics: ControllerSideLayoutMetrics,
    bluetoothViewModel: BluetoothViewModel,
    mode: JoystickMode,
    settingsSyncGeneration: Int,
    onMove: (Float, Float) -> Unit,
    onSwitchStateChange: (Int, Boolean) -> Unit,
    onKnobValueChange: (Float) -> Unit,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
    onStickModeChange: (JoystickMode) -> Unit,
    rangeShape: JoystickRangeShape,
    onRangeShapeChange: (JoystickRangeShape) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .reportTutorialAnchor(TutorialAnchor.PANEL_LEFT_CONTROLS),
    ) {
        ControlPanelKnobSlot(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = metrics.joystickSize + 6.dp, end = 2.dp),
            side = ButtonSide.LEFT,
            knobValue = bluetoothViewModel.rcLeftKnobValue,
            onKnobValueChange = onKnobValueChange,
            metrics = metrics,
        )
        Box(
            modifier = Modifier
                .size(metrics.joystickSize)
                .align(Alignment.BottomCenter)
                .reportTutorialAnchor(TutorialAnchor.PANEL_LEFT_STICK),
            contentAlignment = Alignment.BottomEnd,
        ) {
            ControllerSideButtons(
                side = ButtonSide.LEFT,
                joystickSize = metrics.joystickSize,
                onTopPress = onTopPress,
                onBottomPress = onBottomPress,
            )
            ControlPanelStickSlot(
                side = ButtonSide.LEFT,
                metrics = metrics,
                mode = mode,
                settingsSyncGeneration = settingsSyncGeneration,
                stickPosition = bluetoothViewModel.rcLeftStickPosition,
                onMove = onMove,
                onStickModeChange = onStickModeChange,
                rangeShape = rangeShape,
                onRangeShapeChange = onRangeShapeChange,
            )
            ControlPanelSwitchesSlot(
                switchStates = bluetoothViewModel.rcLeftSwitchStates,
                onSwitchStateChange = onSwitchStateChange,
                metrics = metrics,
            )
        }
    }
}

@Composable
private fun ControlPanelRightTelemetryLayer(
    metrics: ControllerSideLayoutMetrics,
    bluetoothViewModel: BluetoothViewModel,
) {
    val sideTelemetry by bluetoothViewModel.rcRightSideTelemetry.collectAsState()
    val indicator by bluetoothViewModel.rcRightIndicator.collectAsState()
    val routing by bluetoothViewModel.rcChannelRouting.collectAsState()
    var panelMenu by remember { mutableStateOf(false) }
    var batteryMenu by remember { mutableStateOf(false) }
    ControllerSideTelemetryRow(
        modifier = Modifier.fillMaxWidth(),
        side = ButtonSide.RIGHT,
        telemetry = sideTelemetry,
        panelWidth = metrics.panelWidth,
        extraContentSize = metrics.extraContentSize,
        ledSize = metrics.ledSize,
        ledSpacing = metrics.ledSpacing,
        onPanelDoubleTap = { panelMenu = true },
        panelMenu = {
            TelemetryWidgetOptionsMenu(
                expanded = panelMenu,
                sink = TelemetrySink.PANEL_RIGHT,
                selectedChannel = routing.sourceFor(TelemetrySink.PANEL_RIGHT),
                widgetLabel = sideTelemetry.panelTitle,
                onChannelSelected = { channel ->
                    bluetoothViewModel.saveChannelBinding(TelemetrySink.PANEL_RIGHT, channel)
                },
                onLabelChange = { label ->
                    bluetoothViewModel.saveWidgetLabel(TelemetrySink.PANEL_RIGHT, label)
                },
                onDismiss = { panelMenu = false },
                panelOn = sideTelemetry.panelOn,
                onPanelOnChange = { isOn ->
                    bluetoothViewModel.saveNumericPanelOn(TelemetrySink.PANEL_RIGHT, isOn)
                },
                panelColorGreen = sideTelemetry.panelColorArgb == UserSettings.PANEL_COLOR_GREEN_ARGB,
                onPanelColorGreenChange = { isGreen ->
                    bluetoothViewModel.saveNumericPanelColorGreen(TelemetrySink.PANEL_RIGHT, isGreen)
                },
            )
        },
        topExtraContent = { mod ->
            Box {
                BatteryStatus(
                    level = indicator.value,
                    modifier = mod,
                    title = indicator.title,
                    onDoubleTap = { batteryMenu = true },
                )
                TelemetryWidgetOptionsMenu(
                    expanded = batteryMenu,
                    sink = TelemetrySink.BATTERY_GAUGE,
                    selectedChannel = routing.sourceFor(TelemetrySink.BATTERY_GAUGE),
                    widgetLabel = indicator.title,
                    onChannelSelected = { channel ->
                        bluetoothViewModel.saveChannelBinding(TelemetrySink.BATTERY_GAUGE, channel)
                    },
                    onLabelChange = { label ->
                        bluetoothViewModel.saveWidgetLabel(TelemetrySink.BATTERY_GAUGE, label)
                    },
                    onDismiss = { batteryMenu = false },
                )
            }
        },
    )
}

@Composable
private fun ControlPanelRightControlsLayer(
    metrics: ControllerSideLayoutMetrics,
    bluetoothViewModel: BluetoothViewModel,
    mode: JoystickMode,
    settingsSyncGeneration: Int,
    onMove: (Float, Float) -> Unit,
    onSwitchStateChange: (Int, Boolean) -> Unit,
    onKnobValueChange: (Float) -> Unit,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
    onStickModeChange: (JoystickMode) -> Unit,
    rangeShape: JoystickRangeShape,
    onRangeShapeChange: (JoystickRangeShape) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        ControlPanelKnobSlot(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = metrics.joystickSize + 6.dp, start = 2.dp),
            side = ButtonSide.RIGHT,
            knobValue = bluetoothViewModel.rcRightKnobValue,
            onKnobValueChange = onKnobValueChange,
            metrics = metrics,
        )
        Box(
            modifier = Modifier
                .size(metrics.joystickSize)
                .align(Alignment.BottomCenter)
                .reportTutorialAnchor(TutorialAnchor.PANEL_RIGHT_STICK),
            contentAlignment = Alignment.BottomEnd,
        ) {
            ControllerSideButtons(
                side = ButtonSide.RIGHT,
                joystickSize = metrics.joystickSize,
                onTopPress = onTopPress,
                onBottomPress = onBottomPress,
            )
            ControlPanelStickSlot(
                side = ButtonSide.RIGHT,
                metrics = metrics,
                mode = mode,
                settingsSyncGeneration = settingsSyncGeneration,
                stickPosition = bluetoothViewModel.rcRightStickPosition,
                onMove = onMove,
                onStickModeChange = onStickModeChange,
                rangeShape = rangeShape,
                onRangeShapeChange = onRangeShapeChange,
            )
            ControlPanelSwitchesSlot(
                switchStates = bluetoothViewModel.rcRightSwitchStates,
                onSwitchStateChange = onSwitchStateChange,
                metrics = metrics,
            )
        }
    }
}

@Composable
private fun BoxScope.ControlPanelStickSlot(
    side: ButtonSide,
    metrics: ControllerSideLayoutMetrics,
    mode: JoystickMode,
    settingsSyncGeneration: Int,
    stickPosition: StateFlow<Pair<Float, Float>>,
    onMove: (Float, Float) -> Unit,
    onStickModeChange: (JoystickMode) -> Unit,
    rangeShape: JoystickRangeShape,
    onRangeShapeChange: (JoystickRangeShape) -> Unit,
) {
    val position by stickPosition.collectAsState()
    val joystickSize = metrics.joystickSize
    var menuExpanded by remember { mutableStateOf(false) }
    val tutorial = LocalFirstConnectionTutorial.current
    LaunchedEffect(menuExpanded) {
        if (menuExpanded) tutorial?.onStickOptionsOpened()
    }
    val stickName = stringResource(
        if (side == ButtonSide.RIGHT) {
            R.string.rc_controller_settings_right_stick
        } else {
            R.string.rc_controller_settings_left_stick
        },
    )
    Box(modifier = Modifier.fillMaxSize()) {
            ControllerSideJoystick(
                modifier = Modifier
                    .offset(
                        x = if (side == ButtonSide.RIGHT) (-joystickSize * -0.03f) else (joystickSize * -0.03f),
                        y = (-joystickSize * 0.1f),
                    )
                    .fillMaxSize(),
                mode = mode,
                stickPosition = position,
                settingsSyncGeneration = settingsSyncGeneration,
                rangeShape = rangeShape,
                onMove = onMove,
                onDoubleTap = { menuExpanded = true },
                contentDescription = stringResource(
                    R.string.control_panel_widget_config_content_description,
                    stickName,
                ),
            )
        ControlPanelStickOptionsMenuHost(
            expanded = menuExpanded,
            side = side,
            selectedMode = mode,
            onModeSelected = onStickModeChange,
            selectedRangeShape = rangeShape,
            onRangeShapeSelected = onRangeShapeChange,
            onDismiss = { menuExpanded = false },
        )
    }
}

@Composable
private fun ControlPanelStickOptionsMenuHost(
    expanded: Boolean,
    side: ButtonSide,
    selectedMode: JoystickMode,
    onModeSelected: (JoystickMode) -> Unit,
    selectedRangeShape: JoystickRangeShape,
    onRangeShapeSelected: (JoystickRangeShape) -> Unit,
    onDismiss: () -> Unit,
) {
    if (LocalInspectionMode.current) {
        StickOptionsMenu(
            expanded = expanded,
            selectedMode = selectedMode,
            onModeSelected = onModeSelected,
            selectedRangeShape = selectedRangeShape,
            onRangeShapeSelected = onRangeShapeSelected,
            channelLink = StickChannelLink.DEFAULT,
            onDismiss = onDismiss,
        )
        return
    }
    val displayVm: ControlPanelPlotDisplayViewModel = hiltViewModel()
    val displaySettings by displayVm.settings.collectAsState()
    val channelLink = if (side == ButtonSide.RIGHT) {
        displaySettings.rightStickLink
    } else {
        displaySettings.leftStickLink
    }
    StickOptionsMenu(
        expanded = expanded,
        selectedMode = selectedMode,
        onModeSelected = onModeSelected,
        selectedRangeShape = selectedRangeShape,
        onRangeShapeSelected = onRangeShapeSelected,
        channelLink = channelLink,
        occupiedChannels = displaySettings.occupiedChannels(
            exceptLeftStick = side != ButtonSide.RIGHT,
            exceptRightStick = side == ButtonSide.RIGHT,
        ),
        onChannelLinkChange = { next ->
            displayVm.update { settings ->
                if (side == ButtonSide.RIGHT) {
                    settings.copy(rightStickLink = next)
                } else {
                    settings.copy(leftStickLink = next)
                }
            }
        },
        onDismiss = onDismiss,
    )
}

@Composable
private fun BoxScope.ControlPanelSwitchesSlot(
    switchStates: StateFlow<SwitchStates>,
    onSwitchStateChange: (Int, Boolean) -> Unit,
    metrics: ControllerSideLayoutMetrics,
) {
    val states by switchStates.collectAsState()
    ControllerSideSwitches(
        switchStates = states,
        onSwitchStateChange = onSwitchStateChange,
        switchPositions = metrics.switchPositions,
        switchSize = metrics.switchSize,
    )
}

@Composable
private fun ControlPanelKnobSlot(
    side: ButtonSide,
    knobValue: StateFlow<Float>,
    onKnobValueChange: (Float) -> Unit,
    metrics: ControllerSideLayoutMetrics,
    modifier: Modifier = Modifier,
) {
    val value by knobValue.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }
    val knobName = stringResource(
        if (side == ButtonSide.RIGHT) {
            R.string.rc_controller_settings_right_knob
        } else {
            R.string.rc_controller_settings_left_knob
        },
    )
    ControllerSideKnob(
        modifier = modifier,
        knobSize = metrics.knobSize,
        knobValue = value,
        onKnobValueChange = onKnobValueChange,
        onDoubleTap = { menuExpanded = true },
        contentDescription = stringResource(
            R.string.control_panel_widget_config_content_description,
            knobName,
        ),
        menu = {
            ControlPanelKnobOptionsMenuHost(
                expanded = menuExpanded,
                side = side,
                onDismiss = { menuExpanded = false },
            )
        },
    )
}

@Composable
internal fun ControlPanelKnobOptionsMenuHost(
    expanded: Boolean,
    side: ButtonSide,
    onDismiss: () -> Unit,
) {
    if (LocalInspectionMode.current) {
        KnobOptionsMenu(
            expanded = expanded,
            channelLink = KnobChannelLink.DEFAULT,
            onDismiss = onDismiss,
        )
        return
    }
    val displayVm: ControlPanelPlotDisplayViewModel = hiltViewModel()
    val displaySettings by displayVm.settings.collectAsState()
    val channelLink = if (side == ButtonSide.RIGHT) {
        displaySettings.rightKnobLink
    } else {
        displaySettings.leftKnobLink
    }
    KnobOptionsMenu(
        expanded = expanded,
        channelLink = channelLink,
        occupiedChannels = displaySettings.occupiedChannels(
            exceptLeftKnob = side != ButtonSide.RIGHT,
            exceptRightKnob = side == ButtonSide.RIGHT,
        ),
        onChannelLinkChange = { next ->
            displayVm.update { settings ->
                if (side == ButtonSide.RIGHT) {
                    settings.copy(rightKnobLink = next)
                } else {
                    settings.copy(leftKnobLink = next)
                }
            }
        },
        onDismiss = onDismiss,
    )
}

@Composable
private fun ControlPanelLeftController(
    settings: UserSettings,
    contentWidth: Dp,
    settingsSyncGeneration: Int,
    rcState: RcControlState,
    sideTelemetry: SideTelemetry,
    indicator: SideIndicatorUi,
    onMove: (Float, Float) -> Unit,
    onSwitchStateChange: (Int, Boolean) -> Unit,
    onKnobValueChange: (Float) -> Unit,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
) {
    val switchStates = SwitchStates.of(rcState.leftSwitches)
    val topExtraContent: @Composable (Modifier) -> Unit = { mod ->
        AnalogIndicator(modifier = mod, value = indicator.value, title = indicator.title)
    }

    ControllerSide(
        modifier = Modifier.fillMaxHeight(),
        side = ButtonSide.LEFT,
        contentWidth = contentWidth,
        mode = settings.leftStickMode,
        stickPosition = rcState.leftStickPosition,
        settingsSyncGeneration = settingsSyncGeneration,
        onMove = onMove,
        switchStates = switchStates,
        onSwitchStateChange = onSwitchStateChange,
        knobValue = rcState.leftKnobValue,
        onKnobValueChange = onKnobValueChange,
        telemetry = sideTelemetry,
        topExtraContent = topExtraContent,
        onTopPress = onTopPress,
        onBottomPress = onBottomPress,
    )
}

@Composable
private fun ControlPanelRightController(
    settings: UserSettings,
    contentWidth: Dp,
    settingsSyncGeneration: Int,
    rcState: RcControlState,
    sideTelemetry: SideTelemetry,
    indicator: SideIndicatorUi,
    onMove: (Float, Float) -> Unit,
    onSwitchStateChange: (Int, Boolean) -> Unit,
    onKnobValueChange: (Float) -> Unit,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
) {
    val switchStates = SwitchStates.of(rcState.rightSwitches)
    val topExtraContent: @Composable (Modifier) -> Unit = { mod ->
        BatteryStatus(level = indicator.value, modifier = mod, title = indicator.title)
    }

    ControllerSide(
        modifier = Modifier.fillMaxHeight(),
        side = ButtonSide.RIGHT,
        contentWidth = contentWidth,
        mode = settings.rightStickMode,
        stickPosition = rcState.rightStickPosition,
        settingsSyncGeneration = settingsSyncGeneration,
        onMove = onMove,
        switchStates = switchStates,
        onSwitchStateChange = onSwitchStateChange,
        knobValue = rcState.rightKnobValue,
        onKnobValueChange = onKnobValueChange,
        telemetry = sideTelemetry,
        topExtraContent = topExtraContent,
        onTopPress = onTopPress,
        onBottomPress = onBottomPress,
    )
}

private fun shareSessionCsv(context: Context, file: java.io.File): Boolean {
    return try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        fun sendIntent(mimeType: String): Intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val csvIntent = sendIntent("text/csv")
        val plainIntent = sendIntent("text/plain")
        val hasCsvTargets = context.packageManager
            .queryIntentActivities(csvIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .isNotEmpty()
        val sendIntent = if (hasCsvTargets) csvIntent else plainIntent
        context.packageManager
            .queryIntentActivities(sendIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .forEach { resolve ->
                context.grantUriPermission(
                    resolve.activityInfo.packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        val chooser = Intent.createChooser(
            sendIntent,
            context.getString(R.string.control_panel_session_csv_share_chooser_title),
        ).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }
}

class FakeBluetoothViewModel {
    val telemetryState: StateFlow<TelemetryState> = MutableStateFlow(
        TelemetryState(
            panelState = PanelState(1234, 5678, true, true, 0xFFFF0000.toInt(), 0xFF00FF00.toInt(), "RPM", "RPM"),
            indicatorState = IndicatorState(75, 98, 0x00.toByte(), "Analog", "Battery")
        )
    )
    val userSettings: StateFlow<SettingsUiState> = MutableStateFlow(
        SettingsUiState.Success(
            UserSettings(
                leftStickMode = JoystickMode.HorizontalHold(JoystickMode.LEFT),
                rightStickMode = JoystickMode.Spring(JoystickMode.CENTER),
                leftKnobInitialValue = 0.25f,
                rightKnobInitialValue = 0.75f,
                switchInitialStates = mapOf(0 to false, 1 to true, 2 to false, 3 to true, 4 to false, 5 to true)
            )
        )
    )
    val rcControlState: StateFlow<RcControlState> = MutableStateFlow(
        RcControlState(
            leftStickPosition = Pair(0f, 0f),
            rightStickPosition = Pair(0f, 0f),
            leftSwitches = listOf(false, false, false),
            rightSwitches = listOf(false, false, false),
            leftKnobValue = 0.25f,
            rightKnobValue = 0.75f
        )
    )

//    fun startSendingRcData() {}
//    fun stopSendingRcData() {}
//    fun onLeftStickChanged(x: Float, y: Float) {}
//    fun onRightStickChanged(x: Float, y: Float) {}
//    fun onLeftSwitchChanged(index: Int, newState: Boolean) {}
//    fun onRightSwitchChanged(index: Int, newState: Boolean) {}
//    fun onLeftKnobChanged(newValue: Float) {}
//    fun onRightKnobChanged(newValue: Float) {}
//    fun sendButtonEvent(event: ButtonEvent) {}
}

@Preview(showBackground = true, device = "spec:width=914dp,height=411dp,dpi=420", name = "ControlPanel Landscape Phone")
@Composable
fun ControlPanelScreenPreviewDirect() {
    TeleCon4Esp32Theme {
        val fakeViewModel = FakeBluetoothViewModel()
        ControlPanelScreen(
            telemetryState = fakeViewModel.telemetryState,
            userSettings = fakeViewModel.userSettings,
            rcControlState = fakeViewModel.rcControlState
        )
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=160", name = "ControlPanel Tablet Landscape")
@Composable
fun ControlPanelScreenPreviewTablet() {
    TeleCon4Esp32Theme {
        val fakeViewModel = FakeBluetoothViewModel()
        ControlPanelScreen(
            telemetryState = fakeViewModel.telemetryState,
            userSettings = fakeViewModel.userSettings,
            rcControlState = fakeViewModel.rcControlState
        )
    }
}

@Preview(showBackground = true, device = "spec:width=1024dp,height=600dp,dpi=160", name = "ControlPanel Small Tablet Landscape")
@Composable
fun ControlPanelScreenPreviewSmallTablet() {
    TeleCon4Esp32Theme {
        val fakeViewModel = FakeBluetoothViewModel()
        ControlPanelScreen(
            telemetryState = fakeViewModel.telemetryState,
            userSettings = fakeViewModel.userSettings,
            rcControlState = fakeViewModel.rcControlState
        )
    }
}

@Preview(showBackground = true, device = "spec:width=1920dp,height=1080dp,dpi=160", name = "ControlPanel Large Screen Landscape")
@Composable
fun ControlPanelScreenPreviewLargeScreen() {
    TeleCon4Esp32Theme {
        val fakeViewModel = FakeBluetoothViewModel()
        ControlPanelScreen(
            telemetryState = fakeViewModel.telemetryState,
            userSettings = fakeViewModel.userSettings,
            rcControlState = fakeViewModel.rcControlState
        )
    }
}
