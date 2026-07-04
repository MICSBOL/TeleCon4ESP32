package com.micsbol.telecon4esp32.ui.greenhouse.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.greenhouse.GreenhouseGlass

@Composable
fun GreenhouseControlPanel(
    fanOn: Boolean,
    heaterOn: Boolean,
    pumpOn: Boolean,
    lightsOn: Boolean,
    isAutoMode: Boolean,
    ventOpenPercent: Int,
    targetTempC: Int,
    targetHumidityPercent: Int,
    onFanToggle: () -> Unit,
    onHeaterToggle: () -> Unit,
    onPumpToggle: () -> Unit,
    onLightsToggle: () -> Unit,
    onAutoModeToggle: () -> Unit,
    onVentChange: (Int) -> Unit,
    onVentChangeFinished: () -> Unit,
    onTargetTempChange: (Int) -> Unit,
    onTargetHumidityChange: (Int) -> Unit,
    onTargetClimateFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPortrait =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    var expanded by rememberSaveable { mutableStateOf(!isPortrait) }
    val expandLabel = stringResource(R.string.greenhouse_controls_expand)
    val collapseLabel = stringResource(R.string.greenhouse_controls_collapse)
    val toggleLabel = if (expanded) collapseLabel else expandLabel
    val collapsedSummary = buildCollapsedControlsSummary(
        fanOn = fanOn,
        heaterOn = heaterOn,
        pumpOn = pumpOn,
        lightsOn = lightsOn,
        isAutoMode = isAutoMode,
        ventOpenPercent = ventOpenPercent,
        targetTempC = targetTempC,
        targetHumidityPercent = targetHumidityPercent,
    )

    GreenhouseCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .semantics { contentDescription = toggleLabel }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.greenhouse_controls_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenhouseGlass.TextPrimary,
                )
                if (!expanded) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = collapsedSummary,
                        style = MaterialTheme.typography.labelSmall,
                        color = GreenhouseGlass.TextOnGlassMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = GreenhouseGlass.AccentGreen,
                modifier = Modifier.size(24.dp),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(12.dp))
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
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GreenhouseToggleButton(
                        icon = Icons.Default.LightMode,
                        label = stringResource(
                            if (lightsOn) R.string.greenhouse_lights_on else R.string.greenhouse_lights_off,
                        ),
                        isActive = lightsOn,
                        onClick = onLightsToggle,
                        activeColor = GreenhouseGlass.AccentLime,
                        modifier = Modifier.weight(1f),
                    )
                    GreenhouseToggleButton(
                        icon = Icons.Default.Eco,
                        label = stringResource(
                            if (isAutoMode) R.string.greenhouse_mode_auto else R.string.greenhouse_mode_manual,
                        ),
                        isActive = isAutoMode,
                        onClick = onAutoModeToggle,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                GreenhouseSliderRow(
                    label = stringResource(R.string.greenhouse_vent_slider_label, ventOpenPercent),
                    value = ventOpenPercent.toFloat(),
                    valueRange = 0f..100f,
                    steps = 19,
                    onValueChange = { onVentChange(it.toInt()) },
                    onValueChangeFinished = onVentChangeFinished,
                )
                Spacer(modifier = Modifier.height(12.dp))
                GreenhouseSliderRow(
                    label = stringResource(R.string.greenhouse_target_temp_slider, targetTempC),
                    value = targetTempC.toFloat(),
                    valueRange = 10f..40f,
                    steps = 30,
                    onValueChange = { onTargetTempChange(it.toInt()) },
                    onValueChangeFinished = onTargetClimateFinished,
                )
                Spacer(modifier = Modifier.height(8.dp))
                GreenhouseSliderRow(
                    label = stringResource(
                        R.string.greenhouse_target_humidity_slider,
                        targetHumidityPercent,
                    ),
                    value = targetHumidityPercent.toFloat(),
                    valueRange = 30f..95f,
                    steps = 65,
                    onValueChange = { onTargetHumidityChange(it.toInt()) },
                    onValueChangeFinished = onTargetClimateFinished,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.greenhouse_target_climate,
                        targetTempC,
                        targetHumidityPercent,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = GreenhouseGlass.TextOnGlassMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun buildCollapsedControlsSummary(
    fanOn: Boolean,
    heaterOn: Boolean,
    pumpOn: Boolean,
    lightsOn: Boolean,
    isAutoMode: Boolean,
    ventOpenPercent: Int,
    targetTempC: Int,
    targetHumidityPercent: Int,
): String {
    val mode = stringResource(
        if (isAutoMode) R.string.greenhouse_mode_auto else R.string.greenhouse_mode_manual,
    )
    val actives = buildList {
        if (fanOn) add(stringResource(R.string.greenhouse_fan_on))
        if (heaterOn) add(stringResource(R.string.greenhouse_heater_on))
        if (pumpOn) add(stringResource(R.string.greenhouse_pump_on))
        if (lightsOn) add(stringResource(R.string.greenhouse_lights_on))
    }
    val activesText = if (actives.isEmpty()) {
        stringResource(R.string.greenhouse_controls_all_idle)
    } else {
        actives.joinToString(" · ")
    }
    return stringResource(
        R.string.greenhouse_controls_collapsed_summary,
        mode,
        activesText,
        ventOpenPercent,
        targetTempC,
        targetHumidityPercent,
    )
}

@Composable
private fun GreenhouseSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = GreenhouseGlass.TextOnGlassSecondary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = GreenhouseGlass.AccentGreen,
                activeTrackColor = GreenhouseGlass.AccentGreen,
                inactiveTrackColor = GreenhouseGlass.ChipBackground.copy(alpha = 0.8f),
            ),
        )
    }
}
