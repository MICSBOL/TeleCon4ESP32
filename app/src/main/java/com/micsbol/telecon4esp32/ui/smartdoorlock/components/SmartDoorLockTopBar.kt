package com.micsbol.telecon4esp32.ui.smartdoorlock.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.ConnectionStatusDot
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
    Column(
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
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SmartDoorLockGlassIconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.smart_door_lock_back_content_description),
                    tint = SmartDoorLockGlass.TextPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.smart_door_lock_screen_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmartDoorLockGlass.TextPrimary,
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
                    )
                }
            }

            Text(
                text = formatCallDuration(callDurationSeconds),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = SmartDoorLockGlass.TextSecondary,
                modifier = Modifier.padding(end = 4.dp),
            )
            actions()
        }
    }
}

private fun formatCallDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
