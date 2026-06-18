package com.micsbol.telecon4esp32.ui.solarsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.smarthome.components.SmartHomeSectionHeader
import com.micsbol.telecon4esp32.ui.solarsystem.PowerFlowUiModel
import com.micsbol.telecon4esp32.ui.solarsystem.SolarIconSize
import com.micsbol.telecon4esp32.ui.solarsystem.SolarSystemIconType
import com.micsbol.telecon4esp32.ui.theme.PlotOrange
import com.micsbol.telecon4esp32.ui.theme.StatusConnected
import com.micsbol.telecon4esp32.ui.theme.TechBlueBright

@Composable
fun LivePowerFlow(
    powerFlow: PowerFlowUiModel,
    modifier: Modifier = Modifier,
) {
    SolarSystemCard(modifier = modifier) {
        SmartHomeSectionHeader(title = stringResource(R.string.solar_system_section_live_power_flow))
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PowerFlowNode(
                iconType = SolarSystemIconType.SOLAR,
                tint = PlotOrange,
                label = stringResource(R.string.solar_system_metric_solar),
                value = stringResource(R.string.solar_system_power_watts, powerFlow.solarPowerW),
            )
            PowerFlowArrow(color = PlotOrange)
            PowerFlowNode(
                iconType = SolarSystemIconType.BATTERY,
                tint = StatusConnected,
                label = stringResource(R.string.solar_system_metric_battery),
                value = stringResource(R.string.solar_system_power_watts, powerFlow.batteryPowerW),
            )
            PowerFlowArrow(color = StatusConnected)
            PowerFlowNode(
                iconType = SolarSystemIconType.LOAD,
                tint = TechBlueBright,
                label = stringResource(R.string.solar_system_metric_load),
                value = stringResource(R.string.solar_system_power_watts, powerFlow.loadPowerW),
            )
        }
    }
}

@Composable
private fun PowerFlowNode(
    iconType: SolarSystemIconType,
    tint: Color,
    label: String,
    value: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SolarSystemIcon(
            type = iconType,
            tint = tint,
            size = SolarIconSize.PowerFlow,
            showBadge = false,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = tint,
        )
    }
}

@Composable
private fun PowerFlowArrow(
    color: Color,
) {
    Canvas(
        modifier = Modifier
            .width(60.dp)
            .height(36.dp),
    ) {
        val stroke = 3.dp.toPx()
        val y = size.height * 0.55f
        val arrowHead = 10.dp.toPx()

        drawLine(
            color = color.copy(alpha = 0.35f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width - arrowHead, y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )

        val head = Path().apply {
            moveTo(size.width, y)
            lineTo(size.width - arrowHead, y - arrowHead * 0.55f)
            lineTo(size.width - arrowHead, y + arrowHead * 0.55f)
            close()
        }
        drawPath(path = head, color = color)

        drawCircle(
            color = color.copy(alpha = 0.45f),
            radius = 3.dp.toPx(),
            center = Offset(size.width * 0.35f, y),
        )
    }
}
