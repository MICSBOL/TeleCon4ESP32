package com.micsbol.telecon4esp32.ui.cyber.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.cyber.theme.CyberColors
import com.micsbol.telecon4esp32.ui.cyber.theme.CyberType

data class CyberMetric(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val valueColor: Color = CyberColors.NeonPrimary,
)

/**
 * Horizontal quick-status strip: a row of [CyberMetric]s separated by thin
 * vertical neon dividers, wrapped in a [CyberPanel].
 */
@Composable
fun QuickStatusPanel(
    title: String,
    metrics: List<CyberMetric>,
    modifier: Modifier = Modifier,
) {
    CyberPanel(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp),
        chamfer = 10.dp,
        glowIntensity = 0.7f,
        contentPadding = 14.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = title.uppercase(), style = CyberType.Meta, color = CyberColors.TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                metrics.forEachIndexed { index, metric ->
                    MetricCell(metric = metric, modifier = Modifier.weight(1f))
                    if (index != metrics.lastIndex) MetricDivider()
                }
            }
        }
    }
}

@Composable
private fun MetricCell(metric: CyberMetric, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = metric.icon,
            contentDescription = null,
            tint = CyberColors.NeonSecondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = metric.label.uppercase(),
                style = CyberType.Meta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = metric.value,
                style = CyberType.Label.copy(color = metric.valueColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(34.dp)
            .drawBehind {
                drawLine(
                    color = CyberColors.NeonSecondary.copy(alpha = 0.35f),
                    start = Offset(size.width / 2f, 0f),
                    end = Offset(size.width / 2f, size.height),
                    strokeWidth = size.width,
                )
            },
    )
}
