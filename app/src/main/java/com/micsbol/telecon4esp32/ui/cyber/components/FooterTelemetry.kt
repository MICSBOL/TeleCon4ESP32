package com.micsbol.telecon4esp32.ui.cyber.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.cyber.theme.CyberColors
import com.micsbol.telecon4esp32.ui.cyber.theme.CyberType

/**
 * Compact bottom telemetry strip with small icon + value cells separated by
 * thin dividers, framed by a low-glow [CyberPanel].
 */
@Composable
fun FooterTelemetry(
    metrics: List<CyberMetric>,
    modifier: Modifier = Modifier,
) {
    CyberPanel(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        chamfer = 8.dp,
        glowIntensity = 0.5f,
        cornerTicks = false,
        contentPadding = 10.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            metrics.forEachIndexed { index, metric ->
                FooterCell(metric = metric, modifier = Modifier.weight(1f))
                if (index != metrics.lastIndex) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .drawBehind {
                                drawLine(
                                    color = CyberColors.NeonSecondary.copy(alpha = 0.3f),
                                    start = Offset(size.width / 2f, 0f),
                                    end = Offset(size.width / 2f, size.height),
                                    strokeWidth = size.width,
                                )
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun FooterCell(metric: CyberMetric, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = metric.icon,
                contentDescription = null,
                tint = CyberColors.NeonSecondary,
                modifier = Modifier.size(13.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = metric.label.uppercase(),
                style = CyberType.Meta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = metric.value,
            style = CyberType.Meta.copy(color = metric.valueColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
