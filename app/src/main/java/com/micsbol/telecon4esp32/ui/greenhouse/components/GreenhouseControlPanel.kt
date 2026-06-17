package com.micsbol.telecon4esp32.ui.greenhouse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.mutedTextColor

@Composable
fun GreenhouseControlPanel(
    fanOn: Boolean,
    heaterOn: Boolean,
    pumpOn: Boolean,
    targetTempC: Int,
    targetHumidityPercent: Int,
    onFanToggle: () -> Unit,
    onHeaterToggle: () -> Unit,
    onPumpToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GreenhouseCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GreenhouseToggleButton(
                icon = Icons.Default.Air,
                label = stringResource(
                    if (fanOn) R.string.greenhouse_fan_on else R.string.greenhouse_fan_off,
                ),
                isActive = fanOn,
                onClick = onFanToggle,
                modifier = Modifier.weight(1f),
            )
            GreenhouseToggleButton(
                icon = Icons.Default.LocalFireDepartment,
                label = stringResource(
                    if (heaterOn) R.string.greenhouse_heater_on else R.string.greenhouse_heater_off,
                ),
                isActive = heaterOn,
                onClick = onHeaterToggle,
                modifier = Modifier.weight(1f),
            )
            GreenhouseToggleButton(
                icon = Icons.Default.WaterDrop,
                label = stringResource(
                    if (pumpOn) R.string.greenhouse_pump_on else R.string.greenhouse_pump_idle,
                ),
                isActive = pumpOn,
                onClick = onPumpToggle,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(
                R.string.greenhouse_target_climate,
                targetTempC,
                targetHumidityPercent,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = mutedTextColor(),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
        )
    }
}
