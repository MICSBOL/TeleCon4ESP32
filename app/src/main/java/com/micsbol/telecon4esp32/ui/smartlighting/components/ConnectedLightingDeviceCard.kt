package com.micsbol.telecon4esp32.ui.smartlighting.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.mutedTextColor
import com.micsbol.telecon4esp32.ui.greenhouse.components.GreenhouseStatusBadge
import com.micsbol.telecon4esp32.ui.smartlighting.ConnectedLightingDeviceUiModel
import com.micsbol.telecon4esp32.ui.theme.StatusConnected

@Composable
fun ConnectedLightingDeviceCard(
    device: ConnectedLightingDeviceUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val thumbnailRes = if (device.isOn) device.thumbnailOnRes else device.thumbnailOffRes
    val deviceName = stringResource(device.nameRes)

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(thumbnailRes),
                contentDescription = stringResource(
                    R.string.smart_lighting_device_thumbnail_content_description,
                    deviceName,
                    stringResource(
                        if (device.isOn) {
                            R.string.smart_lighting_status_on
                        } else {
                            R.string.smart_lighting_status_off
                        },
                    ),
                ),
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(device.typeRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedTextColor(),
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (device.isOnline) {
                    GreenhouseStatusBadge(
                        text = stringResource(R.string.smart_lighting_status_online),
                        backgroundColor = StatusConnected.copy(alpha = 0.12f),
                        contentColor = StatusConnected,
                        showStatusDot = true,
                        connected = true,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = StatusConnected,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(
                            R.string.smart_lighting_signal_percent,
                            device.signalPercent,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = mutedTextColor(),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
