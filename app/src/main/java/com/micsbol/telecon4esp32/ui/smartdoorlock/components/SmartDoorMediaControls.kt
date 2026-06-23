package com.micsbol.telecon4esp32.ui.smartdoorlock.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.theme.DarkSurfaceVariant
import com.micsbol.telecon4esp32.ui.theme.StatusConnected

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
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SmartDoorMediaTile(
            label = if (isMicEnabled) {
                stringResource(R.string.smart_door_lock_mic_on)
            } else {
                stringResource(R.string.smart_door_lock_mic_off)
            },
            icon = if (isMicEnabled) Icons.Default.Mic else Icons.Default.MicOff,
            enabled = isMicEnabled,
            onClick = onMicToggle,
            modifier = Modifier.weight(1f),
        )
        SmartDoorMediaTile(
            label = if (isSpeakerEnabled) {
                stringResource(R.string.smart_door_lock_speaker_on)
            } else {
                stringResource(R.string.smart_door_lock_speaker_off)
            },
            icon = if (isSpeakerEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
            enabled = isSpeakerEnabled,
            onClick = onSpeakerToggle,
            modifier = Modifier.weight(1f),
        )
        SmartDoorMediaTile(
            label = if (isCameraEnabled) {
                stringResource(R.string.smart_door_lock_camera_on)
            } else {
                stringResource(R.string.smart_door_lock_camera_off)
            },
            icon = if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
            enabled = isCameraEnabled,
            onClick = onCameraToggle,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SmartDoorMediaTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) StatusConnected else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (enabled) StatusConnected else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
