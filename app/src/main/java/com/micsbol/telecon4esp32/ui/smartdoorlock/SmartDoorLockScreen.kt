package com.micsbol.telecon4esp32.ui.smartdoorlock

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.applications.ApplicationSettingsIconButton
import com.micsbol.telecon4esp32.ui.smartdoorlock.components.SmartDoorLockTopBar
import com.micsbol.telecon4esp32.ui.smartdoorlock.components.SmartDoorMediaControls
import com.micsbol.telecon4esp32.ui.smartdoorlock.components.SmartDoorRelayPanel
import com.micsbol.telecon4esp32.ui.smartdoorlock.components.SmartDoorSignalButtons
import com.micsbol.telecon4esp32.ui.smartdoorlock.components.SmartDoorVideoPanel
import com.micsbol.telecon4esp32.ui.theme.StatusDisconnected
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme

@Composable
fun SmartDoorLockScreen(
    navController: NavController,
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
            ApplicationSettingsIconButton(
                applicationId = ApplicationId.SMART_DOOR_LOCK,
                navController = navController,
            )
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SmartDoorLockTopBar(
            callDurationSeconds = uiState.callDurationSeconds,
            isConnected = uiState.isEsp32Online,
            onBackClick = onBackClick,
            actions = topBarActions,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SmartDoorVideoPanel(
                cameraState = uiState.cameraState,
                doorLockState = uiState.doorLockState,
                isEsp32Online = uiState.isEsp32Online,
                wifiSignalDbm = uiState.wifiSignalDbm,
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

            Button(
                onClick = onEndCall,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusDisconnected,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = null,
                )
                Text(
                    text = stringResource(R.string.smart_door_lock_end_call),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
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
