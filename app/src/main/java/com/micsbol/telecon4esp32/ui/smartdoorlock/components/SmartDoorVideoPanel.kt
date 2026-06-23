package com.micsbol.telecon4esp32.ui.smartdoorlock.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.greenhouse.components.GreenhouseStatusBadge
import com.micsbol.telecon4esp32.ui.smartdoorlock.DoorLockState
import com.micsbol.telecon4esp32.ui.theme.AccentOrange
import com.micsbol.telecon4esp32.ui.theme.DarkBackground
import com.micsbol.telecon4esp32.ui.theme.StatusConnected
import com.micsbol.telecon4esp32.ui.theme.StatusDisconnected

@Composable
fun SmartDoorVideoPanel(
    cameraState: CameraStreamState,
    doorLockState: DoorLockState,
    isEsp32Online: Boolean,
    wifiSignalDbm: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkBackground),
    ) {
        when (cameraState) {
            is CameraStreamState.Frame -> {
                Image(
                    bitmap = cameraState.bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.smart_door_lock_video_content_description),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            CameraStreamState.Connecting -> {
                SmartDoorConnectionLostBackground()
                SmartDoorConnectionOverlay(
                    title = stringResource(R.string.smart_door_lock_connecting),
                    showSpinner = true,
                )
            }
            is CameraStreamState.Error -> {
                SmartDoorConnectionLostBackground()
                SmartDoorConnectionOverlay(
                    title = stringResource(R.string.smart_door_lock_connection_lost_title),
                    subtitle = cameraState.message,
                )
            }
            CameraStreamState.Idle -> {
                SmartDoorConnectionLostBackground()
                SmartDoorConnectionOverlay(
                    title = stringResource(R.string.smart_door_lock_connection_lost_title),
                    subtitle = stringResource(R.string.smart_door_lock_connection_lost_body),
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val isLocked = doorLockState == DoorLockState.LOCKED
            GreenhouseStatusBadge(
                text = if (isLocked) {
                    stringResource(R.string.smart_door_lock_door_locked)
                } else {
                    stringResource(R.string.smart_door_lock_door_unlocked)
                },
                backgroundColor = if (isLocked) {
                    StatusConnected.copy(alpha = 0.2f)
                } else {
                    AccentOrange.copy(alpha = 0.2f)
                },
                contentColor = if (isLocked) StatusConnected else AccentOrange,
                leadingIcon = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                iconTint = if (isLocked) StatusConnected else AccentOrange,
            )
            GreenhouseStatusBadge(
                text = if (isEsp32Online) {
                    stringResource(R.string.smart_door_lock_esp32_online)
                } else {
                    stringResource(R.string.smart_door_lock_esp32_offline)
                },
                backgroundColor = Color.Black.copy(alpha = 0.45f),
                contentColor = if (isEsp32Online) StatusConnected else StatusDisconnected,
                leadingIcon = Icons.Default.Memory,
                iconTint = if (isEsp32Online) StatusConnected else StatusDisconnected,
            )
            GreenhouseStatusBadge(
                text = if (isEsp32Online) {
                    stringResource(R.string.smart_door_lock_wifi_signal, wifiSignalDbm)
                } else {
                    stringResource(R.string.smart_door_lock_disconnected)
                },
                backgroundColor = Color.Black.copy(alpha = 0.45f),
                contentColor = Color.White,
                leadingIcon = if (isEsp32Online) Icons.Default.Wifi else Icons.Default.WifiOff,
                iconTint = if (isEsp32Online) StatusConnected else StatusDisconnected,
            )
        }
    }
}

@Composable
private fun SmartDoorConnectionLostBackground(
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.smart_door_lock_connection_lost),
        contentDescription = stringResource(R.string.smart_door_lock_video_fallback_content_description),
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun SmartDoorConnectionOverlay(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showSpinner: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.padding(horizontal = 24.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showSpinner) {
                    CircularProgressIndicator(
                        color = brandPrimary(),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = StatusDisconnected,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
