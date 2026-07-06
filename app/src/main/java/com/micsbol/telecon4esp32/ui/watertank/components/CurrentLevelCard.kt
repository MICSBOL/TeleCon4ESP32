package com.micsbol.telecon4esp32.ui.watertank.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.watertank.WaterTankGlass

@Composable
fun CurrentLevelCard(
    levelPercent: Int,
    volumeLiters: Int,
    capacityLiters: Int,
    modifier: Modifier = Modifier,
) {
    WaterTankCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tank3DVisualization(
                levelPercent = levelPercent,
                showScale = true,
                showGlow = true,
            )
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(WaterTankGlass.AccentCyan.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = WaterTankGlass.AccentCyan,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.water_tank_current_level),
                        style = MaterialTheme.typography.labelMedium,
                        color = WaterTankGlass.TextSecondary,
                    )
                }
                Text(
                    text = stringResource(R.string.water_tank_percent_value, levelPercent),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = WaterTankGlass.AccentCyanBright,
                )
                Text(
                    text = stringResource(
                        R.string.water_tank_volume_capacity,
                        volumeLiters,
                        capacityLiters,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = WaterTankGlass.TextPrimary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                LevelProgressBar(
                    levelPercent = levelPercent,
                    modifier = Modifier.width(128.dp),
                )
            }
        }
    }
}

@Composable
private fun LevelProgressBar(
    levelPercent: Int,
    modifier: Modifier = Modifier,
) {
    val fraction by animateFloatAsState(
        targetValue = levelPercent.coerceIn(0, 100) / 100f,
        animationSpec = tween(durationMillis = 600),
        label = "levelBar",
    )
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(WaterTankGlass.AccentCyanMuted.copy(alpha = 0.22f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .clip(RoundedCornerShape(50))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(WaterTankGlass.AccentCyan, WaterTankGlass.AccentCyanBright),
                    ),
                ),
        )
    }
}
