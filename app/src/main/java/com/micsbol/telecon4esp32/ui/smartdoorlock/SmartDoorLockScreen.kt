package com.micsbol.telecon4esp32.ui.smartdoorlock

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.ui.components.safeHudPadding
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.applications.applicationSettingsTitleRes
import com.micsbol.telecon4esp32.ui.applications.navigateToApplicationSettings
import com.micsbol.telecon4esp32.ui.bluetooth.ApplicationBluetoothTopBarButton
import com.micsbol.telecon4esp32.ui.bluetooth.BluetoothViewModel
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.smartdoorlock.components.SmartDoorLockTopBar
import com.micsbol.telecon4esp32.ui.smartdoorlock.components.SmartDoorMediaControls
import com.micsbol.telecon4esp32.ui.smartdoorlock.components.SmartDoorRelayPanel
import com.micsbol.telecon4esp32.ui.smartdoorlock.components.SmartDoorSignalButtons
import com.micsbol.telecon4esp32.ui.smartdoorlock.components.SmartDoorVideoPanel
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme

@Composable
fun SmartDoorLockScreen(
    navController: NavController,
    bluetoothViewModel: BluetoothViewModel,
    viewModel: SmartDoorLockViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onScreenVisible()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onScreenHidden()
        }
    }

    LaunchedEffect(
        uiState.cameraLinkProfile,
        uiState.isCameraOnline,
        uiState.isEsp32Online,
    ) {
        if (viewModel.shouldAutoConnectSoftApControl()) {
            bluetoothViewModel.ensureWifiSoftApConnected(ApplicationId.SMART_DOOR_LOCK)
        }
    }

    SmartDoorLockScreenContent(
        uiState = uiState,
        onBackClick = {
            viewModel.onEndCall()
            navController.navigateUp()
        },
        onUnlockClick = viewModel::onSendUnlockSignal,
        onLockClick = viewModel::onSendLockSignal,
        onMicToggle = viewModel::onToggleMic,
        onSpeakerToggle = viewModel::onToggleSpeaker,
        onCameraToggle = viewModel::onToggleCamera,
        onTriggerPulse = viewModel::onTriggerRelayPulse,
        onEndCall = {
            viewModel.onEndCall()
            navController.navigateUp()
        },
        topBarActions = {
            Row(
                modifier = Modifier.padding(start = 4.dp, end = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ApplicationBluetoothTopBarButton(accent = SmartDoorLockGlass.AccentGreenBright) { onClick, enabled, content ->
                    SmartDoorLockGlassIconButton(onClick = onClick, enabled = enabled, content = content)
                }
                SmartDoorLockGlassIconButton(
                    onClick = { navController.navigate(Screen.SmartDoorLockHelp.route) },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = stringResource(R.string.smart_door_lock_help_content_description),
                        tint = SmartDoorLockGlass.AccentGreenBright,
                        modifier = Modifier.size(22.dp),
                    )
                }
                SmartDoorLockGlassIconButton(
                    onClick = { navController.navigateToApplicationSettings(ApplicationId.SMART_DOOR_LOCK) },
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(
                            R.string.applications_settings_content_description,
                            stringResource(applicationSettingsTitleRes(ApplicationId.SMART_DOOR_LOCK)),
                        ),
                        tint = SmartDoorLockGlass.TextPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        },
    )
}

@Composable
fun SmartDoorLockScreenContent(
    uiState: SmartDoorLockUiState,
    onBackClick: () -> Unit,
    onUnlockClick: () -> Unit,
    onLockClick: () -> Unit,
    onMicToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onCameraToggle: () -> Unit,
    onTriggerPulse: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier,
    topBarActions: @Composable () -> Unit = {},
) {
    SmartDoorLockBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            SmartDoorLockTopBar(
                callDurationSeconds = uiState.callDurationSeconds,
                isConnected = uiState.isEsp32Online,
                onBackClick = onBackClick,
                actions = topBarActions,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeHudPadding(
                        includeTop = false,
                        includeBottom = true,
                        includeHorizontal = true,
                    )
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SmartDoorVideoPanel(
                    cameraState = uiState.cameraState,
                    doorLockState = uiState.doorLockState,
                    isEsp32Online = uiState.isEsp32Online,
                    wifiSignalDbm = uiState.wifiSignalDbm,
                    onSwipeUnlock = onUnlockClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )

                SmartDoorSignalButtons(
                    onUnlockClick = onUnlockClick,
                    onLockClick = onLockClick,
                )

                SmartDoorMediaControls(
                    isMicEnabled = uiState.isMicEnabled,
                    isSpeakerEnabled = uiState.isSpeakerEnabled,
                    isCameraEnabled = uiState.isCameraEnabled,
                    onMicToggle = onMicToggle,
                    onSpeakerToggle = onSpeakerToggle,
                    onCameraToggle = onCameraToggle,
                )

                SmartDoorRelayPanel(
                    relayPinState = uiState.relayPinState,
                    isPulseActive = uiState.isRelayPulseActive,
                    onTriggerPulse = onTriggerPulse,
                )

                SmartDoorEndCallButton(
                    onClick = onEndCall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SmartDoorEndCallButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(SmartDoorLockGlass.PillShape)
            .background(SmartDoorLockGlass.CardSurfaceTint.copy(alpha = 0.75f))
            .border(
                1.dp,
                SmartDoorLockGlass.BorderColor.copy(alpha = SmartDoorLockGlass.BorderAlpha),
                SmartDoorLockGlass.PillShape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.CallEnd,
            contentDescription = null,
            tint = SmartDoorLockGlass.TextPrimary,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = stringResource(R.string.smart_door_lock_end_call),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = SmartDoorLockGlass.TextPrimary,
        )
    }
}

@Preview(
    showBackground = true,
    name = "Smart Door Lock Connected",
    uiMode = UI_MODE_NIGHT_YES,
    device = "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=portrait"
)
@Composable
private fun SmartDoorLockScreenConnectedPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        SmartDoorLockScreenContent(
            uiState = SmartDoorLockUiState(
                cameraState = CameraStreamState.Idle,
                isEsp32Online = true,
                isCallActive = true,
                callDurationSeconds = 65,
            ),
            onBackClick = {},
            onUnlockClick = {},
            onLockClick = {},
            onMicToggle = {},
            onSpeakerToggle = {},
            onCameraToggle = {},
            onTriggerPulse = {},
            onEndCall = {},
        )
    }
}

@Preview(
    showBackground = true,
    name = "Smart Door Lock Connection Lost",
    uiMode = UI_MODE_NIGHT_YES,
    device = "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=portrait"
)
@Composable
private fun SmartDoorLockScreenLostConnectionPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        SmartDoorLockScreenContent(
            uiState = SmartDoorLockUiState(
                cameraState = CameraStreamState.Error("Camera connection failed"),
                isEsp32Online = false,
                isCallActive = true,
                callDurationSeconds = 12,
            ),
            onBackClick = {},
            onUnlockClick = {},
            onLockClick = {},
            onMicToggle = {},
            onSpeakerToggle = {},
            onCameraToggle = {},
            onTriggerPulse = {},
            onEndCall = {},
        )
    }
}
