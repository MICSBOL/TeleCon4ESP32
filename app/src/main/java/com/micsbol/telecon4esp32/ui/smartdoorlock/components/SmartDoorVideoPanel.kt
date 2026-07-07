package com.micsbol.telecon4esp32.ui.smartdoorlock.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.micsbol.telecon4esp32.ui.smartdoorlock.DoorLockState
import com.micsbol.telecon4esp32.ui.smartdoorlock.SmartDoorLockGlass

@Composable
fun SmartDoorVideoPanel(
    cameraState: CameraStreamState,
    doorLockState: DoorLockState,
    isEsp32Online: Boolean,
    wifiSignalDbm: Int,
    onSwipeUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SmartDoorLockCard(
        modifier = modifier.fillMaxWidth(),
        style = SmartDoorLockCardStyle.Green,
        fillHeight = true,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmartDoorStatusChip(
                    icon = Icons.Default.Wifi,
                    text = if (isEsp32Online) {
                        stringResource(R.string.smart_door_lock_wifi_signal, wifiSignalDbm)
                    } else {
                        stringResource(R.string.smart_door_lock_disconnected)
                    },
                    iconTint = if (isEsp32Online) {
                        SmartDoorLockGlass.AccentGreenBright
                    } else {
                        SmartDoorLockGlass.TextMuted
                    },
                )
                SmartDoorStatusChip(
                    icon = Icons.Default.Bluetooth,
                    text = if (isEsp32Online) {
                        stringResource(R.string.smart_door_lock_esp32_online_short)
                    } else {
                        stringResource(R.string.smart_door_lock_esp32_offline_short)
                    },
                    iconTint = if (isEsp32Online) {
                        SmartDoorLockGlass.AccentGreenBright
                    } else {
                        SmartDoorLockGlass.TextMuted
                    },
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.35f)),
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
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val isLocked = doorLockState == DoorLockState.LOCKED
                    SmartDoorStatusChip(
                        icon = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        text = if (isLocked) {
                            stringResource(R.string.smart_door_lock_door_locked)
                        } else {
                            stringResource(R.string.smart_door_lock_door_unlocked)
                        },
                        iconTint = if (isLocked) {
                            SmartDoorLockGlass.AccentGreenBright
                        } else {
                            Color(0xFFF97316)
                        },
                    )
                    SmartDoorStatusChip(
                        icon = Icons.Default.Memory,
                        text = if (isEsp32Online) {
                            stringResource(R.string.smart_door_lock_esp32_online)
                        } else {
                            stringResource(R.string.smart_door_lock_esp32_offline)
                        },
                        iconTint = if (isEsp32Online) {
                            SmartDoorLockGlass.AccentGreenBright
                        } else {
                            SmartDoorLockGlass.TextMuted
                        },
                    )
                }
            }

            SmartDoorSwipeToUnlock(
                onUnlock = onSwipeUnlock,
                enabled = isEsp32Online,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun SmartDoorStatusChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(SmartDoorLockGlass.PillShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = SmartDoorLockGlass.TextPrimary,
        )
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
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SmartDoorLockGlass.CardSurface.copy(alpha = SmartDoorLockGlass.SurfaceAlphaStrong))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showSpinner) {
                CircularProgressIndicator(
                    color = SmartDoorLockGlass.AccentGreenBright,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = SmartDoorLockGlass.TextMuted,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = SmartDoorLockGlass.TextPrimary,
                textAlign = TextAlign.Center,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SmartDoorLockGlass.TextMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
