package com.micsbol.telecon4esp32.ui.smartlighting.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.greenhouse.components.GreenhouseStatusBadge
import com.micsbol.telecon4esp32.ui.smartlighting.ConnectedLightingDeviceUiModel
import com.micsbol.telecon4esp32.ui.smartlighting.SmartLightingGlass

@Composable
fun BedroomLightingDeviceCard(
    device: ConnectedLightingDeviceUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val thumbnailRes = if (device.isOn) device.thumbnailOnRes else device.thumbnailOffRes
    val deviceName = stringResource(device.nameRes)

    SmartLightingCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
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
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SmartLightingGlass.TextPrimary,
                )
                Text(
                    text = stringResource(device.typeRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = SmartLightingGlass.TextMuted,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BedroomLightStatusChip(isOn = device.isOn)
                    if (device.isOnline) {
                        GreenhouseStatusBadge(
                            text = stringResource(R.string.smart_lighting_status_online),
                            backgroundColor = SmartLightingGlass.AccentGreen.copy(alpha = 0.16f),
                            contentColor = SmartLightingGlass.AccentGreen,
                            showStatusDot = true,
                            connected = true,
                        )
                    }
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
                        tint = SmartLightingGlass.AccentGreen,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(
                            R.string.smart_lighting_signal_percent,
                            device.signalPercent,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = SmartLightingGlass.TextSecondary,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = SmartLightingGlass.TextMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun BedroomLightStatusChip(isOn: Boolean) {
    val backgroundColor = if (isOn) {
        SmartLightingGlass.AccentGreen.copy(alpha = 0.16f)
    } else {
        SmartLightingGlass.CardSurface.copy(alpha = SmartLightingGlass.ChipSurfaceAlpha)
    }
    val contentColor = if (isOn) {
        SmartLightingGlass.AccentGreen
    } else {
        SmartLightingGlass.TextMuted
    }
    val iconTint = if (isOn) SmartLightingGlass.AccentCyan else SmartLightingGlass.AccentWarm

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Lightbulb,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = stringResource(
                if (isOn) R.string.smart_lighting_status_on else R.string.smart_lighting_status_off,
            ),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
        )
    }
}
