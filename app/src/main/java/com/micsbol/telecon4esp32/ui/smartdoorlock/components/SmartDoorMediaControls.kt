package com.micsbol.telecon4esp32.ui.smartdoorlock.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.smartdoorlock.SmartDoorLockGlass

@Composable
fun SmartDoorMediaControls(
    isMicEnabled: Boolean,
    isSpeakerEnabled: Boolean,
    isCameraEnabled: Boolean,
    onMicToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onCameraToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SmartDoorLockGlass.PillShape)
            .background(SmartDoorLockGlass.CardSurface.copy(alpha = SmartDoorLockGlass.SurfaceAlphaStrong))
            .border(
                1.dp,
                SmartDoorLockGlass.BorderColor.copy(alpha = SmartDoorLockGlass.BorderAlpha),
                SmartDoorLockGlass.PillShape,
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmartDoorMediaIconButton(
            enabled = isMicEnabled,
            onClick = onMicToggle,
            enabledIcon = Icons.Default.Mic,
            disabledIcon = Icons.Default.MicOff,
            contentDescription = if (isMicEnabled) {
                stringResource(R.string.smart_door_lock_mic_on)
            } else {
                stringResource(R.string.smart_door_lock_mic_off)
            },
        )
        SmartDoorMediaIconButton(
            enabled = isSpeakerEnabled,
            onClick = onSpeakerToggle,
            enabledIcon = Icons.Default.VolumeUp,
            disabledIcon = Icons.Default.VolumeOff,
            contentDescription = if (isSpeakerEnabled) {
                stringResource(R.string.smart_door_lock_speaker_on)
            } else {
                stringResource(R.string.smart_door_lock_speaker_off)
            },
        )
        SmartDoorMediaIconButton(
            enabled = isCameraEnabled,
            onClick = onCameraToggle,
            enabledIcon = Icons.Default.Videocam,
            disabledIcon = Icons.Default.VideocamOff,
            contentDescription = if (isCameraEnabled) {
                stringResource(R.string.smart_door_lock_camera_on)
            } else {
                stringResource(R.string.smart_door_lock_camera_off)
            },
        )
    }
}

@Composable
private fun SmartDoorMediaIconButton(
    enabled: Boolean,
    onClick: () -> Unit,
    enabledIcon: androidx.compose.ui.graphics.vector.ImageVector,
    disabledIcon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (enabled) {
                    SmartDoorLockGlass.TextPrimary
                } else {
                    Color.Transparent
                },
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (enabled) enabledIcon else disabledIcon,
            contentDescription = contentDescription,
            tint = if (enabled) {
                SmartDoorLockGlass.AccentGreen
            } else {
                SmartDoorLockGlass.TextMuted
            },
            modifier = Modifier.size(24.dp),
        )
    }
}
