package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import android.content.pm.ActivityInfo
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.ButtonEvent
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.ui.applications.ApplicationSettingsIconButton
import com.micsbol.telecon4esp32.ui.bluetooth.BluetoothViewModel
import com.micsbol.telecon4esp32.ui.bluetooth.LocalApplicationBluetoothSession
import com.micsbol.telecon4esp32.ui.control_panel.LockScreenOrientation
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcCameraPanPanel
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcCameraPreview
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcCenterControls
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcControlZone
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcGlassAccentEdge
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcHudMetricsRow
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcHudTopBarStatusRow
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcVehicleHudTopBar
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProLayout
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProLayout.CAMERA_PAN_CENTER
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme

@Composable
fun RcVehicleProScreen(
    navController: NavController,
    bluetoothViewModel: BluetoothViewModel,
    viewModel: RcVehicleProViewModel = hiltViewModel(),
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

    val uiState by viewModel.uiState.collectAsState()
    val rcControlState by bluetoothViewModel.rcControlState.collectAsState()
    val bluetoothConnectionState by bluetoothViewModel.state.collectAsState()
    val bluetoothSession = LocalApplicationBluetoothSession.current
    val onBluetoothConnect by rememberUpdatedState(bluetoothSession?.onConnect)

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity ?: return@LaunchedEffect
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
        )
    }

    LaunchedEffect(Unit) {
        bluetoothViewModel.onControlPanelEntered()
        viewModel.onScreenVisible()
    }

    LaunchedEffect(rcControlState) {
        viewModel.updateDriveMetrics(rcControlState)
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

    RcVehicleProContent(
        uiState = uiState,
        isBluetoothConnecting = bluetoothConnectionState.isConnecting,
        leftStickPosition = rcControlState.leftStickPosition,
        rightStickPosition = rcControlState.rightStickPosition,
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
        cameraKnobValue = rcControlState.rightKnobValue,
        onCameraKnobChange = bluetoothViewModel::onRightKnobChanged,
        onStopClick = {
            bluetoothViewModel.onLeftStickChanged(0f, 0f)
            bluetoothViewModel.onRightStickChanged(0f, 0f)
            viewModel.onEmergencyStop()
        },
    )
}

@Composable
fun RcVehicleProContent(
    uiState: RcVehicleProUiState,
    isBluetoothConnecting: Boolean = false,
    leftStickPosition: Pair<Float, Float>,
    rightStickPosition: Pair<Float, Float>,
    onNavigateBack: () -> Unit,
    onBluetoothDisconnectedClick: () -> Unit = {},
    settingsAction: @Composable () -> Unit,
    onThrottleMove: (Float, Float) -> Unit,
    onSteeringMove: (Float, Float) -> Unit,
    onPhotoClick: () -> Unit,
    onRecordClick: () -> Unit,
    onLightsClick: () -> Unit,
    onBuzzerClick: () -> Unit,
    onCameraFrontClick: () -> Unit,
    cameraKnobValue: Float,
    onCameraKnobChange: (Float) -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        RcCameraPreview(
            cameraState = uiState.cameraState,
            modifier = Modifier.fillMaxSize(),
        )

        Column(modifier = Modifier.fillMaxSize()) {
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
                    )
                },
                actions = settingsAction,
            )



            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
            ) {
                Row {
                    Spacer(Modifier.weight(3f))
                    RcCameraPanPanel(
                        modifier = Modifier.weight(1f),
                        value = cameraKnobValue,
                        onValueChange = onCameraKnobChange,
                        onFrontClick = onCameraFrontClick,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RcControlZone(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.rc_vehicle_control_throttle),
                        negativeLabel = stringResource(R.string.rc_vehicle_control_reverse),
                        positiveLabel = stringResource(R.string.rc_vehicle_control_forward),
                        stickPosition = leftStickPosition,
                        mode = JoystickMode.VerticalHold(JoystickMode.DOWN),
                        accentEdge = RcGlassAccentEdge.START,
                        joystickSize = RcVehicleProLayout.JoystickSize,
                        onMove = onThrottleMove,
                        onStopClick = onStopClick,
                    )

                    RcCenterControls(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp),
                        isRecording = uiState.isRecording,
                        lightsOn = uiState.lightsOn,
                        onPhotoClick = onPhotoClick,
                        onRecordClick = onRecordClick,
                        onLightsClick = onLightsClick,
                    )

                    RcControlZone(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.rc_vehicle_control_steering),
                        negativeLabel = stringResource(R.string.rc_vehicle_control_left),
                        positiveLabel = stringResource(R.string.rc_vehicle_control_right),
                        stickPosition = rightStickPosition,
                        mode = JoystickMode.HorizontalSpring(JoystickMode.CENTER),
                        accentEdge = RcGlassAccentEdge.END,
                        joystickSize = RcVehicleProLayout.JoystickSize,
                        onMove = onSteeringMove,
                        onBuzzerClick = onBuzzerClick,
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
        RcVehicleProContent(
            uiState = RcVehicleProUiState(
                isBluetoothConnected = true,
                isCameraOnline = true,
                speedKmh = 18.6f,
                batteryPercent = 76,
                motorTempCelsius = 48,
            ),
            leftStickPosition = Pair(0f, 0.4f),
            rightStickPosition = Pair(-0.2f, 0f),
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
