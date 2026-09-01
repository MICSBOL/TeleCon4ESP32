package com.micsbol.telecon4esp32.ui.rc_settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ChannelRouting
import com.micsbol.telecon4esp32.domain.model.TelemetryChannel
import com.micsbol.telecon4esp32.domain.model.TelemetrySink
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.theme.AppGlass
import com.micsbol.telecon4esp32.ui.theme.Neo

@Composable
fun ChannelMapFields(
    routing: ChannelRouting,
    onBindingChanged: (TelemetrySink, TelemetryChannel) -> Unit,
    onResetDefaults: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.rc_controller_settings_channel_map_hint),
            style = MaterialTheme.typography.bodySmall,
            color = Neo.TextPrimary.copy(alpha = 0.92f),
        )
        TelemetrySink.entries.forEach { sink ->
            ChannelBindingDropdown(
                sink = sink,
                selected = routing.sourceFor(sink),
                sources = sink.compatibleSources(),
                onSelected = { channel -> onBindingChanged(sink, channel) },
            )
        }
        NeoPillButton(
            text = stringResource(R.string.rc_controller_settings_channel_map_reset),
            onClick = onResetDefaults,
            enabled = !routing.isDefault(),
            modifier = Modifier.fillMaxWidth(),
            fillMaxWidth = true,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChannelBindingDropdown(
    sink: TelemetrySink,
    selected: TelemetryChannel,
    sources: List<TelemetryChannel>,
    onSelected: (TelemetryChannel) -> Unit,
    fieldLabel: String? = null,
) {
    var expanded by remember(sink) { mutableStateOf(false) }
    val selectedLabel = stringResource(selected.labelRes())
    val dropdownFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Neo.TextPrimary,
        unfocusedTextColor = Neo.TextPrimary,
        focusedBorderColor = Neo.Accent,
        unfocusedBorderColor = Neo.TextSecondary.copy(alpha = 0.55f),
        focusedTrailingIconColor = Neo.Accent,
        unfocusedTrailingIconColor = Neo.TextSecondary,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedLabelColor = Neo.Accent,
        unfocusedLabelColor = Neo.TextPrimary,
        cursorColor = Neo.Accent,
    )
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = {
                Text(
                    text = fieldLabel ?: stringResource(sink.labelRes()),
                    color = if (expanded) Neo.Accent else Neo.TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Neo.TextPrimary),
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = dropdownFieldColors,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(AppGlass.DialogSurface),
            containerColor = AppGlass.DialogSurface,
        ) {
            CompositionLocalProvider(LocalContentColor provides Neo.TextPrimary) {
                sources.forEach { channel ->
                    val isSelected = channel == selected
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(channel.labelRes()),
                                color = if (isSelected) Neo.Accent else Neo.TextPrimary,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            expanded = false
                            if (channel != selected) onSelected(channel)
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = Neo.TextPrimary,
                            leadingIconColor = Neo.TextPrimary,
                            trailingIconColor = Neo.TextPrimary,
                        ),
                    )
                }
            }
        }
    }
}

@StringRes
fun TelemetryChannel.labelRes(): Int = when (this) {
    TelemetryChannel.CH_1 -> R.string.telemetry_channel_ch_1
    TelemetryChannel.CH_2 -> R.string.telemetry_channel_ch_2
    TelemetryChannel.CH_3 -> R.string.telemetry_channel_ch_3
    TelemetryChannel.CH_4 -> R.string.telemetry_channel_ch_4
    TelemetryChannel.CH_5 -> R.string.telemetry_channel_ch_5
    TelemetryChannel.CH_6 -> R.string.telemetry_channel_ch_6
    TelemetryChannel.CH_7 -> R.string.telemetry_channel_ch_7
    TelemetryChannel.CH_8 -> R.string.telemetry_channel_ch_8
    TelemetryChannel.ANALOG -> R.string.telemetry_channel_analog
    TelemetryChannel.BATT -> R.string.telemetry_channel_batt
    TelemetryChannel.PANEL_LEFT -> R.string.telemetry_channel_panel_left
    TelemetryChannel.PANEL_RIGHT -> R.string.telemetry_channel_panel_right
    TelemetryChannel.LED_0 -> R.string.telemetry_channel_led_1
    TelemetryChannel.LED_1 -> R.string.telemetry_channel_led_2
    TelemetryChannel.LED_2 -> R.string.telemetry_channel_led_3
    TelemetryChannel.LED_3 -> R.string.telemetry_channel_led_4
    TelemetryChannel.LED_4 -> R.string.telemetry_channel_led_5
    TelemetryChannel.LED_5 -> R.string.telemetry_channel_led_6
    TelemetryChannel.LED_6 -> R.string.telemetry_channel_led_7
    TelemetryChannel.LED_7 -> R.string.telemetry_channel_led_8
}

@StringRes
fun TelemetrySink.labelRes(): Int = when (this) {
    TelemetrySink.PLOT_0 -> R.string.telemetry_sink_plot_1
    TelemetrySink.PLOT_1 -> R.string.telemetry_sink_plot_2
    TelemetrySink.PLOT_2 -> R.string.telemetry_sink_plot_3
    TelemetrySink.PLOT_3 -> R.string.telemetry_sink_plot_4
    TelemetrySink.RADAR_ANGLE -> R.string.telemetry_sink_radar_angle
    TelemetrySink.RADAR_RANGE -> R.string.telemetry_sink_radar_range
    TelemetrySink.ANALOG_GAUGE -> R.string.telemetry_sink_analog_gauge
    TelemetrySink.BATTERY_GAUGE -> R.string.telemetry_sink_battery_gauge
    TelemetrySink.PANEL_LEFT -> R.string.telemetry_sink_panel_left
    TelemetrySink.PANEL_RIGHT -> R.string.telemetry_sink_panel_right
    TelemetrySink.LED_0 -> R.string.telemetry_sink_led_1
    TelemetrySink.LED_1 -> R.string.telemetry_sink_led_2
    TelemetrySink.LED_2 -> R.string.telemetry_sink_led_3
    TelemetrySink.LED_3 -> R.string.telemetry_sink_led_4
    TelemetrySink.LED_4 -> R.string.telemetry_sink_led_5
    TelemetrySink.LED_5 -> R.string.telemetry_sink_led_6
    TelemetrySink.LED_6 -> R.string.telemetry_sink_led_7
    TelemetrySink.LED_7 -> R.string.telemetry_sink_led_8
}
