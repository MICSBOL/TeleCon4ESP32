package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.domain.model.ChannelRouting
import com.micsbol.telecon4esp32.domain.model.PlotLineStyle
import com.micsbol.telecon4esp32.domain.model.TelemetryChannel
import com.micsbol.telecon4esp32.domain.model.TelemetrySink
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.domain.model.parseCalibrationFloat
import com.micsbol.telecon4esp32.domain.model.toCalibrationDraftText
import com.micsbol.telecon4esp32.ui.components.LocalHudGlassDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialogTitle
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.rc_settings.labelRes

@Composable
fun RcHudPlotSettingsDialog(
    series: List<PlotData>,
    traceStyles: List<RcHudPlotTraceStyle>,
    channelRouting: ChannelRouting,
    onTraceStylesChange: (List<RcHudPlotTraceStyle>) -> Unit,
    onPlotLabelChange: (Int, String) -> Unit,
    onPlotChannelChange: (Int, TelemetryChannel) -> Unit,
    onDismiss: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val horizontalMargin = if (isLandscape) 48.dp else 16.dp
    val maxDialogWidth = if (isLandscape) 560.dp else 420.dp
    val accent = brandPrimary()
    val colors = PlotHudDialogColors(accent)
    val count = UserSettings.PLOT_LABEL_COUNT
    var selectedIndex by remember { mutableIntStateOf(0) }
    val index = selectedIndex.coerceIn(0, count - 1)
    val style = traceStyles.getOrElse(index) { RcHudPlotTraceStyle.defaults()[index] }
    val sink = TelemetrySink.plotAt(index)
    val selectedChannel = channelRouting.sourceFor(sink)
    val seriesName = series.getOrElse(index) { PlotData() }.name
    var nameDraft by remember(index, seriesName) { mutableStateOf(seriesName) }
    var yMinDraft by remember(index, style.yMin) {
        mutableStateOf(style.yMin.toCalibrationDraftText())
    }
    var yMaxDraft by remember(index, style.yMax) {
        mutableStateOf(style.yMax.toCalibrationDraftText())
    }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun commitName() {
        onPlotLabelChange(index, nameDraft)
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    fun updateStyle(updated: RcHudPlotTraceStyle) {
        val next = traceStyles.toMutableList()
        while (next.size < count) {
            next.add(RcHudPlotTraceStyle.defaults().getOrElse(next.size) {
                RcHudPlotTraceStyle(colorArgb = RcHudPlotTraceStyle.defaultColorArgb(next.size))
            })
        }
        next[index] = updated
        onTraceStylesChange(next)
    }

    fun commitYRange() {
        val min = parseCalibrationFloat(yMinDraft, style.yMin)
        val max = parseCalibrationFloat(yMaxDraft, style.yMax)
        val (lo, hi) = if (max > min) min to max else min to (min + 1f)
        yMinDraft = lo.toCalibrationDraftText()
        yMaxDraft = hi.toCalibrationDraftText()
        updateStyle(style.copy(yMin = lo, yMax = hi))
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    CompositionLocalProvider(LocalHudGlassDialog provides true) {
        NeoDialog(
            onDismissRequest = {
                commitName()
                commitYRange()
                onDismiss()
            },
            horizontalMargin = horizontalMargin,
            surfaceColor = colors.dialogSurface,
            surfaceAlpha = 1f,
            scrimAlpha = 0.68f,
            wrapContentHeight = true,
            modifier = Modifier.widthIn(max = maxDialogWidth),
            title = {
                NeoDialogTitle(text = stringResource(R.string.rc_vehicle_plot_settings_title))
            },
            subtitle = {
                Text(
                    text = stringResource(R.string.rc_vehicle_plot_settings_body),
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.bodyText,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
            },
            content = {
                PlotHudSectionLabel(
                    text = stringResource(R.string.rc_vehicle_plot_settings_channel_section),
                    color = colors.sectionLabel,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(count) { channelIndex ->
                        PlotHudFilterChip(
                            label = stringResource(
                                R.string.rc_vehicle_plot_settings_channel,
                                channelIndex + 1,
                            ),
                            selected = index == channelIndex,
                            colors = colors,
                            onClick = {
                                commitName()
                                commitYRange()
                                selectedIndex = channelIndex
                            },
                        )
                    }
                }

                PlotHudSectionLabel(
                    text = stringResource(R.string.rc_vehicle_plot_settings_source),
                    color = colors.sectionLabel,
                )
                TelemetryChannel.U8_SOURCES.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        row.forEach { channel ->
                            PlotHudFilterChip(
                                label = stringResource(channel.labelRes()),
                                selected = channel == selectedChannel,
                                colors = colors,
                                onClick = { onPlotChannelChange(index, channel) },
                            )
                        }
                        repeat(4 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    label = {
                        Text(
                            text = stringResource(R.string.rc_vehicle_plot_settings_name),
                            color = colors.bodyText,
                        )
                    },
                    placeholder = {
                        Text(
                            text = stringResource(
                                R.string.rc_vehicle_plot_settings_channel,
                                index + 1,
                            ),
                            color = colors.bodyText.copy(alpha = 0.55f),
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = accent.copy(alpha = 0.45f),
                        cursorColor = accent,
                        focusedLabelColor = accent,
                        unfocusedLabelColor = colors.bodyText,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commitName() }),
                    modifier = Modifier.fillMaxWidth(),
                )

                PlotHudSectionLabel(
                    text = stringResource(R.string.rc_vehicle_plot_settings_color),
                    color = colors.sectionLabel,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RcHudPlotTraceStyle.PALETTE_ARGB.take(8).forEach { argb ->
                        val selected = style.colorArgb == argb
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) Color.White else Color.White.copy(alpha = 0.35f),
                                    shape = CircleShape,
                                )
                                .clickable { updateStyle(style.copy(colorArgb = argb)) },
                        )
                    }
                }

                PlotHudSectionLabel(
                    text = stringResource(R.string.rc_vehicle_plot_settings_style),
                    color = colors.sectionLabel,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PlotHudFilterChip(
                        label = stringResource(R.string.rc_plot_style_line),
                        selected = style.lineStyle == PlotLineStyle.LINE && !style.dashed,
                        colors = colors,
                        onClick = {
                            updateStyle(style.copy(lineStyle = PlotLineStyle.LINE, dashed = false))
                        },
                    )
                    PlotHudFilterChip(
                        label = stringResource(R.string.rc_vehicle_plot_style_dashed),
                        selected = style.lineStyle == PlotLineStyle.LINE && style.dashed,
                        colors = colors,
                        onClick = {
                            updateStyle(style.copy(lineStyle = PlotLineStyle.LINE, dashed = true))
                        },
                    )
                    PlotHudFilterChip(
                        label = stringResource(R.string.rc_plot_style_stair),
                        selected = style.lineStyle == PlotLineStyle.STAIR,
                        colors = colors,
                        onClick = {
                            updateStyle(style.copy(lineStyle = PlotLineStyle.STAIR, dashed = false))
                        },
                    )
                    PlotHudFilterChip(
                        label = stringResource(R.string.rc_plot_style_triangle),
                        selected = style.lineStyle == PlotLineStyle.TRIANGLE,
                        colors = colors,
                        onClick = {
                            updateStyle(style.copy(lineStyle = PlotLineStyle.TRIANGLE, dashed = false))
                        },
                    )
                }

                PlotHudSectionLabel(
                    text = stringResource(R.string.rc_vehicle_plot_settings_y_scale),
                    color = colors.sectionLabel,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = yMinDraft,
                        onValueChange = { yMinDraft = it },
                        label = {
                            Text(
                                text = stringResource(R.string.rc_vehicle_plot_settings_y_min),
                                color = colors.bodyText,
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = accent.copy(alpha = 0.45f),
                            cursorColor = accent,
                            focusedLabelColor = accent,
                            unfocusedLabelColor = colors.bodyText,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = yMaxDraft,
                        onValueChange = { yMaxDraft = it },
                        label = {
                            Text(
                                text = stringResource(R.string.rc_vehicle_plot_settings_y_max),
                                color = colors.bodyText,
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = accent.copy(alpha = 0.45f),
                            cursorColor = accent,
                            focusedLabelColor = accent,
                            unfocusedLabelColor = colors.bodyText,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { commitYRange() }),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = stringResource(R.string.rc_vehicle_plot_settings_y_scale_sides_hint),
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.bodyText.copy(alpha = 0.82f),
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.rc_vehicle_plot_settings_visible),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.sectionLabel,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = style.visible,
                        onCheckedChange = { updateStyle(style.copy(visible = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.chipSelectedLabel,
                            checkedTrackColor = accent,
                            uncheckedThumbColor = colors.bodyText,
                            uncheckedTrackColor = colors.chipUnselectedBg,
                            uncheckedBorderColor = colors.chipUnselectedBorder,
                        ),
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
                        TextButton(
                            onClick = {
                                commitName()
                                commitYRange()
                                onDismiss()
                            },
                        ) {
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

internal data class PlotHudDialogColors(
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
internal fun PlotHudSectionLabel(
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
internal fun RowScope.PlotHudFilterChip(
    label: String,
    selected: Boolean,
    colors: PlotHudDialogColors,
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
