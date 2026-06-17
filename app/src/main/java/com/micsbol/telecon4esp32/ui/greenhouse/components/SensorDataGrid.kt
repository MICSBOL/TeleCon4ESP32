package com.micsbol.telecon4esp32.ui.greenhouse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.theme.PlotOrange
import com.micsbol.telecon4esp32.ui.theme.PlotYellow
import com.micsbol.telecon4esp32.ui.theme.TechBlueBright
import java.util.Locale

@Composable
fun SensorDataGrid(
    soilPercent: Int,
    soilTargetMin: Int,
    soilTargetMax: Int,
    lightLux: Float,
    lightsOn: Boolean,
    deltaTempC: Float,
    ventOpenPercent: Int,
    tankPercent: Int,
    lastIrrigationAgo: String,
    modifier: Modifier = Modifier,
    fillAvailableHeight: Boolean = false,
) {
    val lightValue = if (lightLux >= 1000f) {
        stringResource(
            R.string.greenhouse_light_kilolux,
            String.format(Locale.US, "%.1f", lightLux / 1000f),
        )
    } else {
        stringResource(
            R.string.greenhouse_light_lux,
            lightLux.toInt(),
        )
    }
    val deltaSign = if (deltaTempC >= 0f) "+" else ""

    Column(
        modifier = if (fillAvailableHeight) {
            modifier.fillMaxHeight()
        } else {
            modifier.fillMaxWidth()
        },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = if (fillAvailableHeight) {
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            } else {
                Modifier.fillMaxWidth()
            },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SensorMetricCard(
                icon = Icons.Default.WaterDrop,
                iconTint = TechBlueBright,
                title = stringResource(R.string.greenhouse_soil),
                value = stringResource(R.string.greenhouse_percent_value, soilPercent),
                subtext = stringResource(
                    R.string.greenhouse_soil_target,
                    soilTargetMin,
                    soilTargetMax,
                ),
                valueColor = TechBlueBright,
                modifier = Modifier.weight(1f),
                fillHeight = fillAvailableHeight,
            )
            SensorMetricCard(
                icon = Icons.Default.LightMode,
                iconTint = PlotYellow,
                title = stringResource(R.string.greenhouse_light),
                value = lightValue,
                subtext = stringResource(
                    if (lightsOn) R.string.greenhouse_lights_on else R.string.greenhouse_lights_off,
                ),
                valueColor = PlotYellow,
                modifier = Modifier.weight(1f),
                fillHeight = fillAvailableHeight,
            )
        }
        Row(
            modifier = if (fillAvailableHeight) {
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            } else {
                Modifier.fillMaxWidth()
            },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SensorMetricCard(
                icon = Icons.Default.Thermostat,
                iconTint = PlotOrange,
                title = stringResource(R.string.greenhouse_delta_temp),
                value = stringResource(
                    R.string.greenhouse_delta_temp_value,
                    "$deltaSign${String.format(Locale.US, "%.1f", deltaTempC)}",
                ),
                subtext = stringResource(R.string.greenhouse_vent_open, ventOpenPercent),
                valueColor = PlotOrange,
                modifier = Modifier.weight(1f),
                fillHeight = fillAvailableHeight,
            )
            SensorMetricCard(
                icon = Icons.Default.Opacity,
                iconTint = TechBlueBright,
                title = stringResource(R.string.greenhouse_tank),
                value = stringResource(R.string.greenhouse_percent_value, tankPercent),
                subtext = stringResource(R.string.greenhouse_last_irrigation, lastIrrigationAgo),
                valueColor = TechBlueBright,
                modifier = Modifier.weight(1f),
                fillHeight = fillAvailableHeight,
            )
        }
    }
}
