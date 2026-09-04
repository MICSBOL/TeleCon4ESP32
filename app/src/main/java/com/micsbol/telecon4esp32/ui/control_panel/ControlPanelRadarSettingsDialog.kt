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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.TelemetryChannel
import com.micsbol.telecon4esp32.ui.components.LocalHudGlassDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialogTitle
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.rc_settings.labelRes
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
    val accent = brandPrimary()
    val colors = RadarHudDialogColors(accent)

    CompositionLocalProvider(LocalHudGlassDialog provides true) {
        NeoDialog(
            onDismissRequest = onDismiss,
            horizontalMargin = horizontalMargin,
            surfaceColor = colors.dialogSurface,
            surfaceAlpha = 1f,
            scrimAlpha = 0.68f,
            wrapContentHeight = true,
            modifier = Modifier.widthIn(max = maxDialogWidth),
            title = {
                NeoDialogTitle(text = stringResource(R.string.control_panel_radar_settings_title))
            },
            subtitle = {
                Text(
                    text = stringResource(R.string.control_panel_radar_settings_body),
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.bodyText,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
            },
            content = {
                RadarSettingsSectionLabel(
                    text = stringResource(R.string.control_panel_radar_scan_range_label),
                    color = colors.sectionLabel,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RadarScanSpan.entries.forEach { span ->
                        RadarSettingsFilterChip(
                            label = stringResource(
                                R.string.control_panel_radar_scan_range_degrees,
                                span.degrees.roundToInt(),
                            ),
                            selected = settings.scanSpan == span,
                            colors = colors,
                            onClick = {
                                if (settings.scanSpan != span) {
                                    onSettingsChange(settings.copy(scanSpan = span))
                                }
                            },
                        )
                    }
                }

                RadarSettingsSectionLabel(
                    text = stringResource(R.string.control_panel_radar_beam_width_label),
                    color = colors.sectionLabel,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RadarBeamWidth.entries.forEach { width ->
                        RadarSettingsFilterChip(
                            label = stringResource(width.labelRes()),
                            selected = settings.beamWidth == width,
                            colors = colors,
                            onClick = {
                                if (settings.beamWidth != width) {
                                    onSettingsChange(settings.copy(beamWidth = width))
                                }
                            },
                        )
                    }
                }

                RadarSettingsSectionLabel(
                    text = stringResource(R.string.control_panel_radar_servo_channel_label),
                    color = colors.sectionLabel,
                )
                RadarSeriesChipRow(
                    selectedIndex = radarSeriesIndex(settings.angleSeriesIndex),
                    colors = colors,
                    onSelect = { index ->
                        if (settings.angleSeriesIndex != index) {
                            onSettingsChange(settings.copy(angleSeriesIndex = index))
                        }
                    },
                )

                RadarSettingsSectionLabel(
                    text = stringResource(R.string.control_panel_radar_range_channel_label),
                    color = colors.sectionLabel,
                )
                RadarSeriesChipRow(
                    selectedIndex = radarSeriesIndex(settings.rangeSeriesIndex),
                    colors = colors,
                    onSelect = { index ->
                        if (settings.rangeSeriesIndex != index) {
                            onSettingsChange(settings.copy(rangeSeriesIndex = index))
                        }
                    },
                )

                RadarSettingsSwitchRow(
                    label = stringResource(R.string.control_panel_radar_show_grid),
                    checked = settings.showGrid,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(showGrid = it)) },
                )
                RadarSettingsSwitchRow(
                    label = stringResource(R.string.control_panel_radar_show_sweep_trail),
                    checked = settings.showSweepTrail,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(showSweepTrail = it)) },
                )
                RadarSettingsSwitchRow(
                    label = stringResource(R.string.control_panel_radar_show_history),
                    checked = settings.showHistory,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(showHistory = it)) },
                )

                RadarSettingsSectionLabel(
                    text = stringResource(R.string.control_panel_radar_points_label),
                    color = colors.sectionLabel,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RadarSettingsFilterChip(
                        label = stringResource(R.string.control_panel_radar_points_live),
                        selected = !settings.keepLastPoints,
                        colors = colors,
                        onClick = {
                            if (settings.keepLastPoints) {
                                onSettingsChange(settings.copy(keepLastPoints = false))
                            }
                        },
                    )
                    RadarSettingsFilterChip(
                        label = stringResource(R.string.control_panel_radar_points_keep_last),
                        selected = settings.keepLastPoints,
                        colors = colors,
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
                        color = colors.bodyText,
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
                        colors = SliderDefaults.colors(
                            thumbColor = accent,
                            activeTrackColor = accent,
                            inactiveTrackColor = accent.copy(alpha = 0.28f),
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                    )
                }
            },
            actions = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = accent.copy(alpha = 0.28f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = stringResource(R.string.control_panel_radar_settings_done),
                                color = accent,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            },
        )
    }
}

private data class RadarHudDialogColors(
    val accent: Color,
    val dialogSurface: Color = Color(0xFF121A26),
    val bodyText: Color = Color(0xFFD8E2EC),
    val sectionLabel: Color = Color(0xFFF2F7FC),
    val chipUnselectedBg: Color = Color(0xFF1E2A3A),
    val chipUnselectedLabel: Color = Color(0xFFF0F5FA),
    val chipUnselectedBorder: Color = accent.copy(alpha = 0.72f),
    val chipSelectedBg: Color = accent.copy(alpha = 0.95f),
    val chipSelectedLabel: Color = Color(0xFF061018),
    val chipSelectedBorder: Color = accent,
)

@Composable
private fun RadarSeriesChipRow(
    selectedIndex: Int,
    colors: RadarHudDialogColors,
    onSelect: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TelemetryChannel.U8_SOURCES.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { channel ->
                    val index = channel.u8Index()
                    RadarSettingsFilterChip(
                        label = stringResource(channel.labelRes()),
                        selected = selectedIndex == index,
                        colors = colors,
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
private fun RadarSettingsSectionLabel(
    text: String,
    color: Color,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}

@Composable
private fun RadarSettingsSwitchRow(
    label: String,
    checked: Boolean,
    colors: RadarHudDialogColors,
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
            color = colors.bodyText,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF061018),
                checkedTrackColor = colors.accent,
                checkedBorderColor = colors.accent,
                uncheckedThumbColor = colors.chipUnselectedLabel,
                uncheckedTrackColor = colors.chipUnselectedBg,
                uncheckedBorderColor = colors.chipUnselectedBorder,
            ),
        )
    }
}

@Composable
private fun RowScope.RadarSettingsFilterChip(
    label: String,
    selected: Boolean,
    colors: RadarHudDialogColors,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.weight(1f),
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) colors.chipSelectedLabel else colors.chipUnselectedLabel,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.chipSelectedBg,
            selectedLabelColor = colors.chipSelectedLabel,
            containerColor = colors.chipUnselectedBg,
            labelColor = colors.chipUnselectedLabel,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colors.chipUnselectedBorder,
            selectedBorderColor = colors.chipSelectedBorder,
            borderWidth = if (selected) 1.5.dp else 1.dp,
            selectedBorderWidth = 1.5.dp,
        ),
    )
}

private fun RadarBeamWidth.labelRes(): Int = when (this) {
    RadarBeamWidth.NARROW -> R.string.control_panel_radar_beam_width_narrow
    RadarBeamWidth.MEDIUM -> R.string.control_panel_radar_beam_width_medium
    RadarBeamWidth.WIDE -> R.string.control_panel_radar_beam_width_wide
}
