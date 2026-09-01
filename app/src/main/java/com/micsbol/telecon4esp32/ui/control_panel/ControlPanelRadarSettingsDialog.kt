package com.micsbol.telecon4esp32.ui.control_panel

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.TelemetryChannel
import com.micsbol.telecon4esp32.ui.components.NeoDialog
import com.micsbol.telecon4esp32.ui.rc_settings.labelRes
import com.micsbol.telecon4esp32.ui.components.NeoDialogBody
import com.micsbol.telecon4esp32.ui.components.NeoDialogTitle
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.theme.Neo
import kotlin.math.roundToInt

@Composable
fun ControlPanelRadarSettingsDialog(
    settings: RadarDisplaySettings,
    onSettingsChange: (RadarDisplaySettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val horizontalMargin = if (isLandscape) 48.dp else 16.dp
    val maxDialogWidth = if (isLandscape) 520.dp else 420.dp

    NeoDialog(
        onDismissRequest = onDismiss,
        horizontalMargin = horizontalMargin,
        wrapContentHeight = true,
        modifier = Modifier.widthIn(max = maxDialogWidth),
        title = {
            NeoDialogTitle(text = stringResource(R.string.control_panel_radar_settings_title))
        },
        subtitle = {
            NeoDialogBody(text = stringResource(R.string.control_panel_radar_settings_body))
        },
        content = {
            RadarSettingsSectionLabel(text = stringResource(R.string.control_panel_radar_scan_range_label))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RadarScanSpan.entries.forEach { span ->
                    RadarSettingsFilterChip(
                        label = stringResource(
                            R.string.control_panel_radar_scan_range_degrees,
                            span.degrees.roundToInt(),
                        ),
                        selected = settings.scanSpan == span,
                        onClick = {
                            if (settings.scanSpan != span) {
                                onSettingsChange(settings.copy(scanSpan = span))
                            }
                        },
                    )
                }
            }

            RadarSettingsSectionLabel(text = stringResource(R.string.control_panel_radar_beam_width_label))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RadarBeamWidth.entries.forEach { width ->
                    RadarSettingsFilterChip(
                        label = stringResource(width.labelRes()),
                        selected = settings.beamWidth == width,
                        onClick = {
                            if (settings.beamWidth != width) {
                                onSettingsChange(settings.copy(beamWidth = width))
                            }
                        },
                    )
                }
            }

            RadarSettingsSectionLabel(text = stringResource(R.string.control_panel_radar_servo_channel_label))
            RadarSeriesChipRow(
                selectedIndex = radarSeriesIndex(settings.angleSeriesIndex),
                onSelect = { index ->
                    if (settings.angleSeriesIndex != index) {
                        onSettingsChange(settings.copy(angleSeriesIndex = index))
                    }
                },
            )

            RadarSettingsSectionLabel(text = stringResource(R.string.control_panel_radar_range_channel_label))
            RadarSeriesChipRow(
                selectedIndex = radarSeriesIndex(settings.rangeSeriesIndex),
                onSelect = { index ->
                    if (settings.rangeSeriesIndex != index) {
                        onSettingsChange(settings.copy(rangeSeriesIndex = index))
                    }
                },
            )

            RadarSettingsSwitchRow(
                label = stringResource(R.string.control_panel_radar_show_grid),
                checked = settings.showGrid,
                onCheckedChange = { onSettingsChange(settings.copy(showGrid = it)) },
            )
            RadarSettingsSwitchRow(
                label = stringResource(R.string.control_panel_radar_show_sweep_trail),
                checked = settings.showSweepTrail,
                onCheckedChange = { onSettingsChange(settings.copy(showSweepTrail = it)) },
            )
            RadarSettingsSwitchRow(
                label = stringResource(R.string.control_panel_radar_show_history),
                checked = settings.showHistory,
                onCheckedChange = { onSettingsChange(settings.copy(showHistory = it)) },
            )

            RadarSettingsSectionLabel(text = stringResource(R.string.control_panel_radar_points_label))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RadarSettingsFilterChip(
                    label = stringResource(R.string.control_panel_radar_points_live),
                    selected = !settings.keepLastPoints,
                    onClick = {
                        if (settings.keepLastPoints) {
                            onSettingsChange(settings.copy(keepLastPoints = false))
                        }
                    },
                )
                RadarSettingsFilterChip(
                    label = stringResource(R.string.control_panel_radar_points_keep_last),
                    selected = settings.keepLastPoints,
                    onClick = {
                        if (!settings.keepLastPoints) {
                            onSettingsChange(settings.copy(keepLastPoints = true))
                        }
                    },
                )
            }

            val ringCount = settings.rangeRingCount.coerceIn(
                RadarDisplaySettings.MIN_RANGE_RINGS,
                RadarDisplaySettings.MAX_RANGE_RINGS,
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.control_panel_radar_range_rings, ringCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Neo.TextSecondary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Slider(
                    value = ringCount.toFloat(),
                    onValueChange = { value ->
                        val snapped = value.roundToInt().coerceIn(
                            RadarDisplaySettings.MIN_RANGE_RINGS,
                            RadarDisplaySettings.MAX_RANGE_RINGS,
                        )
                        if (snapped != settings.rangeRingCount) {
                            onSettingsChange(settings.copy(rangeRingCount = snapped))
                        }
                    },
                    valueRange = RadarDisplaySettings.MIN_RANGE_RINGS.toFloat()..
                        RadarDisplaySettings.MAX_RANGE_RINGS.toFloat(),
                    steps = RadarDisplaySettings.MAX_RANGE_RINGS -
                        RadarDisplaySettings.MIN_RANGE_RINGS - 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                )
            }
        },
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NeoPillButton(
                    text = stringResource(R.string.control_panel_radar_settings_done),
                    onClick = onDismiss,
                    compact = true,
                )
            }
        },
    )
}

@Composable
private fun RadarSeriesChipRow(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TelemetryChannel.U8_SOURCES.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { channel ->
                    val index = channel.u8Index()
                    RadarSettingsFilterChip(
                        label = stringResource(channel.labelRes()),
                        selected = selectedIndex == index,
                        onClick = { onSelect(index) },
                    )
                }
                repeat(4 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RadarSettingsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = Neo.TextPrimary,
    )
}

@Composable
private fun RadarSettingsSwitchRow(
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
            color = Neo.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun RowScope.RadarSettingsFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = brandPrimary()
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.weight(1f),
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accent.copy(alpha = 0.95f),
            selectedLabelColor = Color(0xFF061018),
            containerColor = Neo.SurfaceLow,
            labelColor = Neo.TextPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = accent.copy(alpha = 0.55f),
            selectedBorderColor = accent,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.5.dp,
        ),
    )
}

private fun RadarBeamWidth.labelRes(): Int = when (this) {
    RadarBeamWidth.NARROW -> R.string.control_panel_radar_beam_width_narrow
    RadarBeamWidth.MEDIUM -> R.string.control_panel_radar_beam_width_medium
    RadarBeamWidth.WIDE -> R.string.control_panel_radar_beam_width_wide
}
