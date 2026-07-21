package com.micsbol.telecon4esp32.ui.watertank.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.watertank.WaterTankGlass
import com.micsbol.telecon4esp32.ui.watertank.WaterTankGlassIconButton

@Composable
fun WaterTankTopBar(
    isConnected: Boolean,
    deviceId: String,
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
                        0f to Color.White.copy(alpha = 0.28f),
                        0.8f to Color.White.copy(alpha = 0.06f),
                        1f to Color.Transparent,
                    ),
                ),
            )
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WaterTankGlassIconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.water_tank_menu_content_description),
                    tint = WaterTankGlass.AccentCyan,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            WaterTankGlassIconButton(onClick = onRefreshClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.water_tank_refresh_content_description),
                    tint = WaterTankGlass.TextMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            actions()
        }

        Text(
            text = stringResource(R.string.water_tank_screen_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = WaterTankGlass.TextPrimary,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = deviceId,
                style = MaterialTheme.typography.labelMedium,
                color = WaterTankGlass.TextSecondary,
            )
            if (isConnected) {
                WaterTankConnectionBadge(
                    text = stringResource(R.string.water_tank_esp32_connected),
                    connected = true,
                )
            } else {
                WaterTankConnectionBadge(
                    text = stringResource(R.string.water_tank_esp32_disconnected),
                    connected = false,
                )
            }
        }
    }
}

@Composable
private fun WaterTankConnectionBadge(
    text: String,
    connected: Boolean,
) {
    val dotColor = if (connected) WaterTankGlass.Positive else WaterTankGlass.Danger
    Row(
        modifier = Modifier
            .clip(WaterTankGlass.PillShape)
            .background(WaterTankGlass.CardSurface.copy(alpha = WaterTankGlass.BadgeSurfaceAlpha))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = WaterTankGlass.TextSecondary,
        )
    }
}
