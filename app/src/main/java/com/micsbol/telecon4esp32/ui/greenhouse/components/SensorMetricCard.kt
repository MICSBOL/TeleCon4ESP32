package com.micsbol.telecon4esp32.ui.greenhouse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.ui.greenhouse.GreenhouseGlass

@Composable
fun SensorMetricCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    subtext: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false,
    sparklineSeed: Int = value.hashCode(),
) {
    GreenhouseCard(
        modifier = if (fillHeight) modifier.fillMaxHeight() else modifier,
        fillHeight = fillHeight,
    ) {
        Box(
            modifier = if (fillHeight) {
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
            } else {
                Modifier.fillMaxWidth()
            },
        ) {
            MetricSparkline(
                seed = sparklineSeed,
                lineColor = iconTint,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(top = 28.dp),
            )

            Column(
                modifier = if (fillHeight) {
                    Modifier.fillMaxHeight()
                } else {
                    Modifier.fillMaxWidth()
                },
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = valueColor,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenhouseGlass.TextOnGlassPrimary,
                )
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall,
                    color = GreenhouseGlass.TextOnGlassMuted,
                )
            }
        }
    }
}
