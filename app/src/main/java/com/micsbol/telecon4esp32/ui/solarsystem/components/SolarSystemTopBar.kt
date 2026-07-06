package com.micsbol.telecon4esp32.ui.solarsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.solarsystem.SolarGlass
import com.micsbol.telecon4esp32.ui.solarsystem.SolarGlassIconButton
import com.micsbol.telecon4esp32.ui.solarsystem.SolarGridMode

@Composable
fun SolarSystemTopBar(
    isConnected: Boolean,
    isOnline: Boolean,
    deviceId: String,
    panelName: String,
    updatedAgo: String,
    gridMode: SolarGridMode,
    onMenuClick: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Black.copy(alpha = 0.42f),
                        0.75f to Color.Black.copy(alpha = 0.14f),
                        1f to Color.Transparent,
                    ),
                ),
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SolarGlassIconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.solar_system_menu_content_description),
                    tint = SolarGlass.AccentAmber,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = SolarGlass.SunBright,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.solar_system_screen_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SolarGlass.TextOnBackgroundPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            SolarGlassIconButton(onClick = onRefreshClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.solar_system_refresh_content_description),
                    tint = SolarGlass.AccentAmber,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            actions()
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SolarStatusBadge(
                text = when {
                    isOnline -> stringResource(R.string.solar_system_status_online)
                    isConnected -> stringResource(R.string.solar_system_status_waiting)
                    else -> stringResource(R.string.solar_system_status_offline)
                },
                connected = isOnline,
            )
            SolarStatusBadge(
                text = gridModeLabel(gridMode),
                connected = gridMode != SolarGridMode.IDLE,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$deviceId · $panelName",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = SolarGlass.TextOnBackgroundSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (updatedAgo.isNotEmpty() && isOnline) {
                Text(
                    text = stringResource(R.string.solar_system_updated_ago, updatedAgo),
                    style = MaterialTheme.typography.labelSmall,
                    color = SolarGlass.TextOnBackgroundSecondary,
                )
            }
        }
    }
}

@Composable
private fun SolarStatusBadge(
    text: String,
    connected: Boolean,
) {
    Row(
        modifier = Modifier
            .clip(SolarGlass.PillShape)
            .background(SolarGlass.BadgeBackground.copy(alpha = SolarGlass.BadgeSurfaceAlpha))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (connected) SolarGlass.Positive else SolarGlass.TextOnGlassMuted,
                ),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = SolarGlass.TextOnGlassPrimary,
        )
    }
}

@Composable
private fun gridModeLabel(mode: SolarGridMode): String = when (mode) {
    SolarGridMode.EXPORT -> stringResource(R.string.solar_system_grid_export)
    SolarGridMode.IMPORT -> stringResource(R.string.solar_system_grid_import)
    SolarGridMode.IDLE -> stringResource(R.string.solar_system_grid_idle)
}
