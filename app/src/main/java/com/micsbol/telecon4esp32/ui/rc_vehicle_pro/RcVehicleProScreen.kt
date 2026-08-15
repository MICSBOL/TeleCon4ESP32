package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.camera.CameraLinkProfile
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState
import com.micsbol.telecon4esp32.domain.camera.autoConnectSoftApControlWhenCameraOnline
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.ButtonEvent
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.model.RcVehicleProControlSettings
import com.micsbol.telecon4esp32.ui.applications.ApplicationSettingsIconButton
import com.micsbol.telecon4esp32.ui.bluetooth.BluetoothViewModel
import com.micsbol.telecon4esp32.ui.bluetooth.LocalApplicationBluetoothSession
import com.micsbol.telecon4esp32.ui.components.safeHudPadding
import com.micsbol.telecon4esp32.ui.control_panel.LockScreenOrientation
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcCameraPanPanel
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcDriveAssistDialog
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcCameraPreview
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcCenterControls
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcControlZone
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcGlassAccentEdge
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcHudMetricsRow
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcHudTopBarStatusRow
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcVehicleHudTopBar
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcSteerTrim
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcStickMapping
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProLayout
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProLayout.CAMERA_PAN_CENTER
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun RcVehicleProScreen(
    navController: NavController,
    bluetoothViewModel: BluetoothViewModel,
    viewModel: RcVehicleProViewModel = hiltViewModel(),
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isBluetoothConnecting by bluetoothViewModel.state
        .map { it.isConnecting }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = false)
    val cameraKnobValue by bluetoothViewModel.rcRightKnobValue.collectAsStateWithLifecycle()
    // Mapped throttle Y after drive-assist pipeline (for HUD speed estimate).
    val leftStickPosition by bluetoothViewModel.rcLeftStickPosition.collectAsStateWithLifecycle()
    val controlSettings by viewModel.controlSettings.collectAsStateWithLifecycle()
    val bluetoothSession = LocalApplicationBluetoothSession.current
    val onBluetoothConnect by rememberUpdatedState(bluetoothSession?.onConnect)
    val context = LocalContext.current
    val linkProfile = uiState.cameraLinkProfile
    val isWifiSoftApMode = linkProfile == CameraLinkProfile.WIFI_SOFTAP
    val usesWifiLink = isWifiSoftApMode || bluetoothSession?.usesWifiLink == true

    // SoftAP-only (Kit A): once video works, SoftAP is reachable — open TCP control.
    // Kit B (SoftAP video + DevKit BLE): SoftAP HTTP video only; never SoftAP TCP.
    LaunchedEffect(
        linkProfile,
        uiState.isCameraOnline,
        uiState.isBluetoothConnected,
    ) {
        if (
            linkProfile.autoConnectSoftApControlWhenCameraOnline &&
            uiState.isCameraOnline &&
            !uiState.isBluetoothConnected
        ) {
            bluetoothViewModel.ensureWifiSoftApConnected(ApplicationId.RC_VEHICLE_PRO)
        }
    }

    LaunchedEffect(uiState.photoFeedback) {
        when (uiState.photoFeedback) {
            PhotoFeedback.None -> Unit
            PhotoFeedback.Saved -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.rc_vehicle_photo_saved),
                    Toast.LENGTH_SHORT,
                ).show()
                viewModel.consumePhotoFeedback()
            }
            PhotoFeedback.NoFrame -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.rc_vehicle_photo_no_frame),
                    Toast.LENGTH_SHORT,
                ).show()
                viewModel.consumePhotoFeedback()
            }
            PhotoFeedback.Failed -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.rc_vehicle_photo_failed),
                    Toast.LENGTH_SHORT,
                ).show()
                viewModel.consumePhotoFeedback()
            }
        }
    }

    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity ?: return@LaunchedEffect
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
        )
    }

    LaunchedEffect(Unit) {
        bluetoothViewModel.onRcVehicleProEntered()
        viewModel.onScreenVisible()
    }

    LaunchedEffect(uiState.isBluetoothConnected) {
        if (uiState.isBluetoothConnected) {
            bluetoothViewModel.syncRcCameraPanFront()
        }
    }

    LaunchedEffect(leftStickPosition.second) {
        viewModel.updateDriveMetricsFromThrottle(leftStickPosition.second)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onScreenHidden()
            bluetoothViewModel.stopSendingRcData()
        }
    }

    BackHandler {
        navController.navigateUp()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Isolated collector: new SoftAP frames recompose only this layer.
        RcVehicleProCameraLayer(
            cameraPreviewState = viewModel.cameraPreviewState,
            cameraLinkProfile = viewModel.cameraLinkProfile,
        )

        RcVehicleProContent(
            uiState = uiState,
            controlSettings = controlSettings,
            isBluetoothConnecting = isBluetoothConnecting,
            isWifiSoftApMode = usesWifiLink,
            onNavigateBack = { navController.navigateUp() },
            onBluetoothDisconnectedClick = { onBluetoothConnect?.invoke() },
            settingsAction = {
                ApplicationSettingsIconButton(
                    applicationId = ApplicationId.RC_VEHICLE_PRO,
                    navController = navController,
                )
            },
            onThrottleMove = bluetoothViewModel::onLeftStickChanged,
            onSteeringMove = bluetoothViewModel::onRightStickChanged,
            onControlSettingsChange = viewModel::updateControlSettings,
            onCycleThrottleTravel = viewModel::cycleThrottleTravel,
            onCycleSteerTravel = viewModel::cycleSteerTravel,
            onPhotoClick = viewModel::onCapturePhoto,
            onRecordClick = viewModel::onToggleRecording,
            onLightsClick = {
                bluetoothViewModel.onLeftSwitchChanged(0, !uiState.lightsOn)
                viewModel.onToggleLights()
            },
            onBuzzerClick = {
                bluetoothViewModel.sendButtonEvent(ButtonEvent.CENTER_TOP_RIGHT)
            },
            onCameraFrontClick = {
                bluetoothViewModel.onRightKnobChanged(CAMERA_PAN_CENTER)
                bluetoothViewModel.sendButtonEvent(ButtonEvent.CENTER_BOTTOM_RIGHT)
            },
            cameraKnobValue = cameraKnobValue,
            onCameraKnobChange = bluetoothViewModel::onRightKnobChanged,
            onStopClick = {
                bluetoothViewModel.onLeftStickChanged(0f, 0f)
                bluetoothViewModel.onRightStickChanged(0f, 0f)
                viewModel.onEmergencyStop()
            },
            onSaveSteerCenter = bluetoothViewModel::saveSteerCenter,
        )
    }
}

/**
 * Owns camera StateFlow collection so HUD/controls skip recomposition on each JPEG.
 */
@Composable
private fun RcVehicleProCameraLayer(
    cameraPreviewState: StateFlow<CameraStreamState>,
    cameraLinkProfile: StateFlow<CameraLinkProfile>,
    modifier: Modifier = Modifier,
) {
    val cameraState by cameraPreviewState.collectAsStateWithLifecycle()
    val profile by cameraLinkProfile.collectAsStateWithLifecycle()
    RcCameraPreview(
        cameraState = cameraState,
        cameraLinkProfile = profile,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
fun RcVehicleProContent(
    uiState: RcVehicleProUiState,
    controlSettings: RcVehicleProControlSettings = RcVehicleProControlSettings.DEFAULT,
    isBluetoothConnecting: Boolean = false,
    isWifiSoftApMode: Boolean = false,
    onNavigateBack: () -> Unit,
    onBluetoothDisconnectedClick: () -> Unit = {},
    settingsAction: @Composable () -> Unit,
    onThrottleMove: (Float, Float) -> Unit,
    onSteeringMove: (Float, Float) -> Unit,
    onControlSettingsChange: ((RcVehicleProControlSettings) -> RcVehicleProControlSettings) -> Unit = {},
    onCycleThrottleTravel: () -> Unit = {},
    onCycleSteerTravel: () -> Unit = {},
    onPhotoClick: () -> Unit,
    onRecordClick: () -> Unit,
    onLightsClick: () -> Unit,
    onBuzzerClick: () -> Unit,
    onCameraFrontClick: () -> Unit,
    cameraKnobValue: Float,
    onCameraKnobChange: (Float) -> Unit,
    onStopClick: () -> Unit,
    onSaveSteerCenter: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Camera stays full-bleed; HUD/controls clear status, cutouts, and side nav
    // (including OEM landscape builds that under-report horizontal insets).
    var steerTrimMode by remember { mutableStateOf(false) }
    var showDriveAssist by remember { mutableStateOf(false) }
    var rawThrottle by remember { mutableStateOf(Pair(0f, 0f)) }
    var rawSteer by remember { mutableStateOf(Pair(0f, 0f)) }

    fun publishThrottle(x: Float = rawThrottle.first, y: Float = rawThrottle.second) {
        rawThrottle = Pair(x, y)
        val mapped = RcStickMapping.mapThrottleStick(x, y, controlSettings)
        onThrottleMove(mapped.first, mapped.second)
    }

    fun publishSteer(x: Float = rawSteer.first, y: Float = rawSteer.second) {
        rawSteer = Pair(x, y)
        val mapped = RcStickMapping.mapSteerStick(x, y, controlSettings)
        onSteeringMove(mapped.first, mapped.second)
    }

    val throttleHold = controlSettings.throttleHold
    val steeringHold = controlSettings.steeringHold
    val steerTrim = controlSettings.steerTrim
    val throttleMode = if (throttleHold) {
        JoystickMode.VerticalHold(JoystickMode.DOWN)
    } else {
        JoystickMode.VerticalSpring(JoystickMode.CENTER)
    }
    val steeringMode = if (steeringHold) {
        JoystickMode.HorizontalHold(JoystickMode.CENTER)
    } else {
        JoystickMode.HorizontalSpring(JoystickMode.CENTER)
    }

    // Re-emit mapped channels when dual-rate / expo / reverse / trim change while held.
    LaunchedEffect(controlSettings) {
        val throttle = RcStickMapping.mapThrottleStick(rawThrottle.first, rawThrottle.second, controlSettings)
        onThrottleMove(throttle.first, throttle.second)
        val steer = RcStickMapping.mapSteerStick(rawSteer.first, rawSteer.second, controlSettings)
        onSteeringMove(steer.first, steer.second)
    }

    if (showDriveAssist) {
        RcDriveAssistDialog(
            settings = controlSettings,
            onSettingsChange = { next -> onControlSettingsChange { next } },
            onDismiss = { showDriveAssist = false },
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        RcVehicleHudTopBar(
            title = stringResource(R.string.app_rc_vehicle_title),
            onNavigateBack = onNavigateBack,
            metricsContent = {
                RcHudMetricsRow(uiState = uiState)
            },
            statusContent = {
                RcHudTopBarStatusRow(
                    uiState = uiState,
                    isBluetoothConnecting = isBluetoothConnecting,
                    onBluetoothDisconnectedClick = onBluetoothDisconnectedClick,
                    isWifiSoftApMode = isWifiSoftApMode,
                )
            },
            actions = settingsAction,
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .safeHudPadding(
                    includeTop = false,
                    includeBottom = false,
                    includeHorizontal = true,
                )
                .padding(start = 4.dp, end = 4.dp, bottom = 2.dp),
        ) {
            val showControlTitles = maxWidth >= 700.dp
            // Same stick size for throttle and steering; reserve camera knob only.
            val cameraReserve = RcVehicleProLayout.ControlZoneKnobSize + 8.dp
            val availableForStick =
                maxHeight - cameraReserve - 2.dp - RcVehicleProLayout.ControlZoneChromeHeight
            val joystickSize = minOf(
                RcVehicleProLayout.JoystickSize,
                availableForStick,
                maxWidth / 2.7f,
            ).coerceAtLeast(
                minOf(RcVehicleProLayout.JoystickSizeCompact, availableForStick.coerceAtLeast(1.dp)),
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
            ) {
                Row {
                    Spacer(modifier.weight(3f))
                    RcCameraPanPanel(
                        modifier = Modifier.weight(1f),
                        value = cameraKnobValue,
                        onValueChange = onCameraKnobChange,
                        onFrontClick = onCameraFrontClick,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    RcControlZone(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.rc_vehicle_control_throttle),
                        negativeLabel = stringResource(R.string.rc_vehicle_control_reverse),
                        positiveLabel = stringResource(R.string.rc_vehicle_control_forward),
                        stickPosition = rawThrottle,
                        mode = throttleMode,
                        accentEdge = RcGlassAccentEdge.START,
                        joystickSize = joystickSize,
                        showTitle = showControlTitles,
                        onMove = { x, y -> publishThrottle(x, y) },
                        onStopClick = {
                            rawThrottle = Pair(0f, 0f)
                            rawSteer = Pair(0f, 0f)
                            steerTrimMode = false
                            onStopClick()
                        },
                        isStickHold = throttleHold,
                        onStickModeToggle = {
                            onControlSettingsChange { it.copy(throttleHold = !it.throttleHold) }
                            publishThrottle(0f, 0f)
                        },
                    )

                    RcCenterControls(
                        modifier = Modifier.weight(1f),
                        isRecording = uiState.isRecording,
                        lightsOn = uiState.lightsOn,
                        onPhotoClick = onPhotoClick,
                        onRecordClick = onRecordClick,
                        onLightsClick = onLightsClick,
                        throttleTravelPercent = RcStickMapping.travelPercentLabel(
                            controlSettings.throttleTravel,
                        ),
                        steerTravelPercent = RcStickMapping.travelPercentLabel(
                            controlSettings.steerTravel,
                        ),
                        onCycleThrottleTravel = onCycleThrottleTravel,
                        onCycleSteerTravel = onCycleSteerTravel,
                        onOpenDriveAssist = { showDriveAssist = true },
                    )

                    RcControlZone(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.rc_vehicle_control_steering),
                        negativeLabel = stringResource(R.string.rc_vehicle_control_left),
                        positiveLabel = stringResource(R.string.rc_vehicle_control_right),
                        stickPosition = if (steerTrimMode) Pair(0f, 0f) else rawSteer,
                        mode = steeringMode,
                        accentEdge = RcGlassAccentEdge.END,
                        joystickSize = joystickSize,
                        showTitle = showControlTitles,
                        onMove = { x, y ->
                            if (!steerTrimMode) {
                                publishSteer(x, y)
                            }
                        },
                        onBuzzerClick = onBuzzerClick,
                        isStickHold = steeringHold,
                        onStickModeToggle = {
                            onControlSettingsChange { it.copy(steeringHold = !it.steeringHold) }
                            publishSteer(0f, 0f)
                        },
                        steerTrimMode = steerTrimMode,
                        steerTrimChannel = RcSteerTrim.toChannelUnits(steerTrim),
                        onSteerTrimModeToggle = {
                            steerTrimMode = !steerTrimMode
                            if (steerTrimMode) {
                                publishSteer(0f, 0f)
                            }
                        },
                        onSteerTrimNudge = { steps ->
                            val next = RcSteerTrim.nudge(steerTrim, steps)
                            onControlSettingsChange { it.copy(steerTrim = next) }
                            // Publish with updated trim on next frame via controlSettings; force now:
                            rawSteer = Pair(0f, 0f)
                            val mapped = RcStickMapping.mapSteerStick(
                                0f,
                                0f,
                                controlSettings.copy(steerTrim = next),
                            )
                            onSteeringMove(mapped.first, mapped.second)
                        },
                        onSteerTrimConfirm = {
                            // Live rx already includes trim; ask ESP32 to store PWM as center.
                            // Keep persisted trim so wheels stay straight before firmware saves.
                            onSaveSteerCenter()
                            steerTrimMode = false
                            publishSteer(0f, 0f)
                        },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 844, heightDp = 390)
@Composable
private fun RcVehicleProContentPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize()) {
            RcCameraPreview(
                cameraState = CameraStreamState.Idle,
                cameraLinkProfile = CameraLinkProfile.WIFI_SOFTAP,
                modifier = Modifier.fillMaxSize(),
            )
            RcVehicleProContent(
                uiState = RcVehicleProUiState(
                    isBluetoothConnected = true,
                    isCameraOnline = true,
                    speedKmh = 18.6f,
                    batteryPercent = 76,
                    motorTempCelsius = 48,
                ),
                onNavigateBack = {},
                settingsAction = {},
                onThrottleMove = { _, _ -> },
                onSteeringMove = { _, _ -> },
                onPhotoClick = {},
                onRecordClick = {},
                onLightsClick = {},
                onBuzzerClick = {},
                onCameraFrontClick = {},
                cameraKnobValue = 0.5f,
                onCameraKnobChange = {},
                onStopClick = {},
            )
        }
    }
}
