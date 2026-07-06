package com.micsbol.telecon4esp32.ui.watertank.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.watertank.TankStatus
import com.micsbol.telecon4esp32.ui.watertank.WaterTankGlass

@Composable
fun WaterTankMetricRow(
    volumeLiters: Int,
    capacityLiters: Int,
    updatedAgo: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WaterTankMetricChip(
            icon = Icons.Default.WaterDrop,
            iconTint = WaterTankGlass.AccentCyan,
            label = stringResource(R.string.water_tank_volume),
            value = stringResource(R.string.water_tank_liters_value, volumeLiters),
            modifier = Modifier.weight(1f),
        )
        WaterTankMetricChip(
            icon = Icons.Default.Opacity,
            iconTint = Color(0xFF818CF8),
            label = stringResource(R.string.water_tank_capacity),
            value = stringResource(R.string.water_tank_liters_value, capacityLiters),
            modifier = Modifier.weight(1f),
        )
        WaterTankMetricChip(
            icon = Icons.Default.AccessTime,
            iconTint = WaterTankGlass.AccentCyanBright,
            label = stringResource(R.string.water_tank_last_updated),
            value = stringResource(R.string.water_tank_updated_ago, updatedAgo),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WaterTankMetricChip(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    WaterTankCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(WaterTankGlass.AccentCyanMuted.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = WaterTankGlass.TextSecondary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                ),
                color = WaterTankGlass.TextPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun WaterTankStatusRow(
    tankStatus: TankStatus,
    pumpOn: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val statusText = when (tankStatus) {
            TankStatus.NORMAL -> stringResource(R.string.water_tank_status_normal)
            TankStatus.LOW -> stringResource(R.string.water_tank_status_low)
            TankStatus.CRITICAL -> stringResource(R.string.water_tank_status_critical)
        }
        val statusColor = when (tankStatus) {
            TankStatus.NORMAL -> WaterTankGlass.Positive
            TankStatus.LOW -> WaterTankGlass.Warning
            TankStatus.CRITICAL -> WaterTankGlass.Danger
        }

        WaterTankStatusBadge(
            text = statusText,
            modifier = Modifier.weight(1f),
            backgroundColor = statusColor.copy(alpha = 0.12f),
            contentColor = statusColor,
            leadingIcon = Icons.Default.Check,
            iconTint = statusColor,
        )
        WaterTankStatusBadge(
            text = stringResource(
                if (pumpOn) R.string.water_tank_pump_on else R.string.water_tank_pump_off,
            ),
            modifier = Modifier.weight(1f),
            backgroundColor = WaterTankGlass.AccentCyanMuted.copy(alpha = 0.18f),
            contentColor = WaterTankGlass.TextSecondary,
            leadingIcon = Icons.Default.Settings,
            iconTint = WaterTankGlass.TextMuted,
        )
    }
}

@Composable
private fun WaterTankStatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    contentColor: Color,
    leadingIcon: ImageVector,
    iconTint: Color,
) {
    Row(
        modifier = modifier
            .clip(WaterTankGlass.PillShape)
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor,
        )
    }
}
