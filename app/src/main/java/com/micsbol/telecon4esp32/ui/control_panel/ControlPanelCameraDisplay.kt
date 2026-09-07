package com.micsbol.telecon4esp32.ui.control_panel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.camera.CameraLinkProfile
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState
import com.micsbol.telecon4esp32.domain.camera.shouldStartCameraStream
import com.micsbol.telecon4esp32.ui.bluetooth.LocalApplicationBluetoothSession
import com.micsbol.telecon4esp32.ui.bluetooth.openSystemWifiSettings
import com.micsbol.telecon4esp32.ui.bluetooth.rememberOpenWifiSettingsThenConnect
import com.micsbol.telecon4esp32.ui.bluetooth.supportsInAppSoftApJoin
import com.micsbol.telecon4esp32.ui.camera.SoftApRuntimeStreamQualityOverlay
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcCameraPreview

/**
 * Control Panel center camera pane.
 * On enter, joins the ESP32-CAM SoftAP (if previously connected, HTTP starts immediately)
 * and streams SoftAP video. Bluetooth protocol is unchanged and SoftAP TCP `:3333`
 * is not opened for DevKit overlay video.
 */
@Composable
fun ControlPanelCameraDisplay(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    viewModel: ControlPanelCameraStreamViewModel = hiltViewModel(),
) {
    DisposableEffect(isActive) {
        viewModel.onCameraPaneVisible(isActive)
        onDispose { viewModel.onCameraPaneVisible(false) }
    }

    val profile by viewModel.cameraLinkProfile.collectAsStateWithLifecycle()
    val cameraState by viewModel.cameraPreviewState.collectAsStateWithLifecycle()
    val connectUi by viewModel.cameraConnectUi.collectAsStateWithLifecycle()
    val showStreamQuality by viewModel.showStreamQualityPanel.collectAsStateWithLifecycle()
    val preset by viewModel.softApPerformancePreset.collectAsStateWithLifecycle()
    val hudRate by viewModel.softApHudProcessingRate.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val bluetoothSession = LocalApplicationBluetoothSession.current
    val retry = viewModel::retryCameraWifi
    val openWifiThenRetry = rememberOpenWifiSettingsThenConnect(retry)
    val joinCameraWifi = {
        if (supportsInAppSoftApJoin()) retry() else openWifiThenRetry()
    }
    val openWifiSettings = { openSystemWifiSettings(context) }
    val streamingProfile =
        if (profile.shouldStartCameraStream) profile else CameraLinkProfile.WIFI_CAMERA_DEVKIT_BT

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color(0xFF0A1018)),
    ) {
        when {
            !profile.shouldStartCameraStream -> {
                ControlPanelCameraPreviewShell(
                    modifier = Modifier.fillMaxSize(),
                    message = stringResource(R.string.control_panel_camera_disabled),
                    grayedOut = true,
                )
            }
            cameraState is CameraStreamState.Frame -> {
                RcCameraPreview(
                    cameraState = cameraState,
                    cameraLinkProfile = streamingProfile,
                    isControlConnected = bluetoothSession?.isConnected == true,
                    onConnectControl = bluetoothSession?.onConnect,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> {
                ControlPanelCameraConnectContent(
                    connectUi = connectUi,
                    cameraState = cameraState,
                    onJoinCameraWifi = joinCameraWifi,
                    onRetry = retry,
                    onOpenWifiSettings = openWifiSettings,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (isActive && showStreamQuality) {
            SoftApRuntimeStreamQualityOverlay(
                selectedPreset = preset,
                selectedHudRate = hudRate,
                onPresetSelected = viewModel::onSoftApPerformancePresetChanged,
                onHudRateSelected = viewModel::onSoftApHudProcessingRateChanged,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
private fun ControlPanelCameraConnectContent(
    connectUi: ControlPanelCameraConnectUi,
    cameraState: CameraStreamState,
    onJoinCameraWifi: () -> Unit,
    onRetry: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val connecting = connectUi is ControlPanelCameraConnectUi.Connecting ||
        (connectUi is ControlPanelCameraConnectUi.Ready && cameraState is CameraStreamState.Connecting) ||
        (connectUi is ControlPanelCameraConnectUi.Ready && cameraState is CameraStreamState.Idle)
    when (connectUi) {
        ControlPanelCameraConnectUi.Idle,
        ControlPanelCameraConnectUi.Connecting,
        -> ControlPanelCameraPreviewShell(
            modifier = modifier,
            message = stringResource(R.string.control_panel_camera_connecting),
            showProgress = true,
        )
        ControlPanelCameraConnectUi.Disabled -> ControlPanelCameraPreviewShell(
            modifier = modifier,
            message = stringResource(R.string.control_panel_camera_disabled),
            grayedOut = true,
        )
        is ControlPanelCameraConnectUi.Ready -> {
            if (connecting) {
                ControlPanelCameraPreviewShell(
                    modifier = modifier,
                    message = stringResource(R.string.control_panel_camera_connecting),
                    showProgress = true,
                )
            } else {
                ControlPanelCameraPreviewShell(
                    modifier = modifier,
                    message = stringResource(
                        R.string.control_panel_camera_unavailable,
                        connectUi.ssid,
                        connectUi.password,
                    ),
                    primaryActionLabel = stringResource(R.string.control_panel_camera_retry),
                    onPrimaryAction = onRetry,
                    secondaryActionLabel = stringResource(R.string.control_panel_camera_open_wifi),
                    onSecondaryAction = onOpenWifiSettings,
                )
            }
        }
        is ControlPanelCameraConnectUi.NeedsWifi -> ControlPanelCameraPreviewShell(
            modifier = modifier,
            message = stringResource(
                R.string.control_panel_camera_wifi_required,
                connectUi.ssid,
                connectUi.password,
            ),
            primaryActionLabel = stringResource(R.string.control_panel_camera_join_wifi),
            onPrimaryAction = onJoinCameraWifi,
            secondaryActionLabel = stringResource(R.string.control_panel_camera_open_wifi),
            onSecondaryAction = onOpenWifiSettings,
        )
        is ControlPanelCameraConnectUi.ControlWifiConflict -> ControlPanelCameraPreviewShell(
            modifier = modifier,
            message = stringResource(
                R.string.control_panel_camera_wifi_conflict,
                connectUi.ssid,
            ),
        )
    }
}
