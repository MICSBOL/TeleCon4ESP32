package com.micsbol.telecon4esp32.ui.smartlighting.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.smartlighting.SmartLightingGlass

@Composable
fun SmartLightingHeroBanner(
    lightsOnCount: Int,
    totalLightsCount: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(SmartLightingGlass.CardShape)
            .border(
                width = 1.dp,
                color = SmartLightingGlass.BorderColor.copy(alpha = SmartLightingGlass.BorderAlpha),
                shape = SmartLightingGlass.CardShape,
            ),
    ) {
        Image(
            painter = painterResource(R.drawable.smart_lighting_background),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.15f),
                            0.55f to Color.Black.copy(alpha = 0.45f),
                            1f to SmartLightingGlass.BackgroundTop.copy(alpha = 0.75f),
                        ),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.smart_lighting_hero_active_lights),
                    style = MaterialTheme.typography.labelMedium,
                    color = SmartLightingGlass.TextSecondary,
                )
                Text(
                    text = stringResource(
                        R.string.smart_lighting_hero_lights_count,
                        lightsOnCount,
                        totalLightsCount,
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = SmartLightingGlass.TextPrimary,
                )
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(SmartLightingGlass.HeroGradient)
                    .border(
                        1.dp,
                        SmartLightingGlass.BorderColor.copy(alpha = 0.35f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = SmartLightingGlass.TextPrimary,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        LightHotspot(modifier = Modifier.align(Alignment.TopCenter).padding(top = 28.dp))
        LightHotspot(modifier = Modifier.align(Alignment.CenterStart).padding(start = 36.dp))
        LightHotspot(modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 52.dp))
    }
}

@Composable
private fun LightHotspot(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.22f))
            .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f)),
        )
    }
}
