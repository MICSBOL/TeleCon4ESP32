package com.micsbol.telecon4esp32.ui.smarthome.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.mutedTextColor
import com.micsbol.telecon4esp32.ui.smarthome.RoomStatusBadge
import com.micsbol.telecon4esp32.ui.smarthome.RoomUiModel
import com.micsbol.telecon4esp32.ui.theme.PlotOrange
import com.micsbol.telecon4esp32.ui.theme.TechBlueBright

@Composable
fun RoomCard(
    room: RoomUiModel,
    modifier: Modifier = Modifier,
) {
    SmartHomeCard(
        modifier = modifier.width(160.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(room.nameRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            RoomStatusBadgeChip(room = room)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            room.accentColor.copy(alpha = 0.35f),
                            room.accentColor.copy(alpha = 0.08f),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = room.icon,
                contentDescription = null,
                tint = room.accentColor,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Thermostat,
                contentDescription = null,
                tint = mutedTextColor(),
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(R.string.smart_home_temperature_short, room.temperatureC),
                style = MaterialTheme.typography.labelSmall,
                color = mutedTextColor(),
            )
        }
        if (room.alertTextRes != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = PlotOrange,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(room.alertTextRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = PlotOrange,
                )
            }
        }
    }
}

@Composable
private fun RoomStatusBadgeChip(room: RoomUiModel) {
    val (text, backgroundColor, contentColor) = when (room.statusBadge) {
        RoomStatusBadge.ON_COUNT -> Triple(
            stringResource(R.string.smart_home_room_status_on_count, room.onCount),
            TechBlueBright.copy(alpha = 0.2f),
            TechBlueBright,
        )
        RoomStatusBadge.OFF -> Triple(
            stringResource(R.string.smart_home_room_status_off),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RoomStatusBadge.OPEN -> Triple(
            stringResource(R.string.smart_home_room_status_open),
            PlotOrange.copy(alpha = 0.2f),
            PlotOrange,
        )
    }

    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = contentColor,
    )
}
