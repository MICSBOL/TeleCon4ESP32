package com.micsbol.telecon4esp32.ui.smartdoorlock.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.ConnectionStatusDot
import com.micsbol.telecon4esp32.ui.components.safeHudPadding
import com.micsbol.telecon4esp32.ui.smartdoorlock.SmartDoorLockGlass
import com.micsbol.telecon4esp32.ui.smartdoorlock.SmartDoorLockGlassIconButton

@Composable
fun SmartDoorLockTopBar(
    callDurationSeconds: Int,
    isConnected: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.75f to Color.Transparent,
                        1f to Color.Transparent,
                    ),
                ),
            )
            .safeHudPadding(
                includeTop = true,
                includeBottom = false,
                includeHorizontal = true,
            ),
    ) {
        val compact = maxWidth < 400.dp
        val edgePad = if (compact) 12.dp else 16.dp
        val btnSize = if (compact) 40.dp else 44.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = edgePad, vertical = if (compact) 8.dp else 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmartDoorLockGlassIconButton(onClick = onBackClick, size = btnSize) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.smart_door_lock_back_content_description),
                        tint = SmartDoorLockGlass.TextPrimary,
                        modifier = Modifier.size(if (compact) 20.dp else 22.dp),
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 0.dp)
                        .padding(horizontal = if (compact) 8.dp else 12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.smart_door_lock_screen_title),
                        style = if (compact) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.titleLarge
                        },
                        fontWeight = FontWeight.Bold,
                        color = SmartDoorLockGlass.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ConnectionStatusDot(
                            connected = isConnected,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                        Text(
                            text = if (isConnected) {
                                stringResource(R.string.smart_door_lock_connected)
                            } else {
                                stringResource(R.string.smart_door_lock_disconnected)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isConnected) {
                                SmartDoorLockGlass.AccentGreenBright
                            } else {
                                SmartDoorLockGlass.TextMuted
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (!compact) {
                    Text(
                        text = formatCallDuration(callDurationSeconds),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SmartDoorLockGlass.TextSecondary,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
                actions()
            }
            if (compact) {
                Text(
                    text = formatCallDuration(callDurationSeconds),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = SmartDoorLockGlass.TextSecondary,
                    modifier = Modifier.padding(start = btnSize + 8.dp, top = 4.dp),
                )
            }
        }
    }
}

private fun formatCallDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
