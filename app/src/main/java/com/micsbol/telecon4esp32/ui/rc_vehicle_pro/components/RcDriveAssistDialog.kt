package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.RcVehicleProControlSettings
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcStickMapping
import kotlin.math.roundToInt

@Composable
fun RcDriveAssistDialog(
    settings: RcVehicleProControlSettings,
    onSettingsChange: (RcVehicleProControlSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = scheme.surfaceContainerHigh,
        titleContentColor = scheme.onSurface,
        textContentColor = scheme.onSurfaceVariant,
        title = {
            Text(
                text = stringResource(R.string.rc_vehicle_drive_assist_title),
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.rc_vehicle_drive_assist_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                AssistSwitchRow(
                    label = stringResource(R.string.rc_vehicle_drive_assist_reverse_throttle),
                    checked = settings.reverseThrottle,
                    onCheckedChange = { onSettingsChange(settings.copy(reverseThrottle = it)) },
                )
                AssistSwitchRow(
                    label = stringResource(R.string.rc_vehicle_drive_assist_reverse_steer),
                    checked = settings.reverseSteer,
                    onCheckedChange = { onSettingsChange(settings.copy(reverseSteer = it)) },
                )
                AssistSliderRow(
                    label = stringResource(
                        R.string.rc_vehicle_drive_assist_throttle_travel,
                        RcStickMapping.travelPercentLabel(settings.throttleTravel),
                    ),
                    value = settings.throttleTravel,
                    onValueChange = {
                        onSettingsChange(settings.copy(throttleTravel = it.coerceIn(0.3f, 1f)))
                    },
                )
                AssistSliderRow(
                    label = stringResource(
                        R.string.rc_vehicle_drive_assist_steer_travel,
                        RcStickMapping.travelPercentLabel(settings.steerTravel),
                    ),
                    value = settings.steerTravel,
                    onValueChange = {
                        onSettingsChange(settings.copy(steerTravel = it.coerceIn(0.3f, 1f)))
                    },
                )
                AssistSliderRow(
                    label = stringResource(
                        R.string.rc_vehicle_drive_assist_steer_expo,
                        (settings.steerExpo * 100f).roundToInt(),
                    ),
                    value = settings.steerExpo,
                    onValueChange = {
                        onSettingsChange(settings.copy(steerExpo = it.coerceIn(0f, 1f)))
                    },
                )
                AssistSliderRow(
                    label = stringResource(
                        R.string.rc_vehicle_drive_assist_throttle_expo,
                        (settings.throttleExpo * 100f).roundToInt(),
                    ),
                    value = settings.throttleExpo,
                    onValueChange = {
                        onSettingsChange(settings.copy(throttleExpo = it.coerceIn(0f, 1f)))
                    },
                )
                AssistSliderRow(
                    label = stringResource(
                        R.string.rc_vehicle_drive_assist_deadzone,
                        (settings.deadzone * 100f).roundToInt(),
                    ),
                    value = settings.deadzone,
                    valueRange = 0f..0.25f,
                    onValueChange = {
                        onSettingsChange(settings.copy(deadzone = it.coerceIn(0f, 0.25f)))
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.rc_vehicle_drive_assist_done),
                    color = brandPrimary(),
                )
            }
        },
    )
}

@Composable
private fun AssistSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AssistSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
        )
    }
}
