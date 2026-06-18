package com.micsbol.telecon4esp32.ui.solarsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.micsbol.telecon4esp32.ui.solarsystem.SolarIconSize
import com.micsbol.telecon4esp32.ui.solarsystem.SolarSystemIconType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.mutedTextColor
import com.micsbol.telecon4esp32.ui.smarthome.components.SmartHomeSectionHeader
import com.micsbol.telecon4esp32.ui.theme.StatusConnected
import java.util.Locale

@Composable
fun DailyEnergyCard(
    dailyEnergyKwh: Float,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    SolarSystemCard(
        modifier = modifier,
        fillHeight = true,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SmartHomeSectionHeader(title = stringResource(R.string.solar_system_section_daily_energy))
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = mutedTextColor(),
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp),
            contentAlignment = Alignment.Center,
        ) {
            DailyEnergyRing(progress = progress)
            SolarSystemIcon(
                type = SolarSystemIconType.BATTERY,
                tint = StatusConnected,
                size = SolarIconSize.Hero,
                showBadge = false,
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(
                    R.string.solar_system_daily_energy_value,
                    String.format(Locale.US, "%.1f", dailyEnergyKwh),
                ),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.solar_system_period_today),
                style = MaterialTheme.typography.labelMedium,
                color = mutedTextColor(),
            )
        }
    }
}

@Composable
private fun DailyEnergyRing(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val trackColor = StatusConnected.copy(alpha = 0.15f)
    val progressColor = StatusConnected

    Canvas(modifier = modifier.size(140.dp)) {
        val strokeWidth = 12.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(
            (size.width - diameter) / 2f,
            (size.height - diameter) / 2f,
        )
        val arcSize = Size(diameter, diameter)

        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}
