package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.JoystickAxis
import com.micsbol.telecon4esp32.domain.model.StickChannelLink
import com.micsbol.telecon4esp32.domain.model.TelemetryChannel
import com.micsbol.telecon4esp32.ui.components.LocalHudGlassDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialogTitle
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.rc_settings.labelRes

data class RcHudScopeStyle(
    val colorArgb: Int,
    val dashed: Boolean = false,
    val fill: Boolean = true,
) {
    companion object {
        fun batteryDefault(): RcHudScopeStyle = RcHudScopeStyle(
            colorArgb = RcHudPlotTraceStyle.defaultColorArgb(1),
        )

        fun tempDefault(): RcHudScopeStyle = RcHudScopeStyle(
            colorArgb = RcHudPlotTraceStyle.defaultColorArgb(10),
        )
    }
}

data class RcHudStickPointStyle(
    val visible: Boolean = true,
    val colorArgb: Int,
    val showLabel: Boolean = true,
    val showTrail: Boolean = true,
    val thickTrail: Boolean = false,
    val largePoint: Boolean = false,
    val vertical: TelemetryChannel? = null,
    val horizontal: TelemetryChannel? = null,
) {
    fun channelLink(): StickChannelLink = StickChannelLink(
        enabled = vertical != null || horizontal != null,
        vertical = vertical,
        horizontal = horizontal,
    )

    fun togglingChannel(axis: JoystickAxis, channel: TelemetryChannel): RcHudStickPointStyle =
        when (axis) {
            JoystickAxis.VERTICAL -> copy(
                vertical = if (vertical == channel) null else StickChannelLink.analogOrNull(channel),
            )
            JoystickAxis.HORIZONTAL -> copy(
                horizontal = if (horizontal == channel) null else StickChannelLink.analogOrNull(channel),
            )
            JoystickAxis.COMBINED -> this
        }
}

data class RcHudStickStyle(
    val showGrid: Boolean = true,
    val showAxes: Boolean = true,
    val left: RcHudStickPointStyle,
    val right: RcHudStickPointStyle,
) {
    fun point(index: Int): RcHudStickPointStyle = if (index == 0) left else right

    fun withPoint(index: Int, point: RcHudStickPointStyle): RcHudStickStyle =
        if (index == 0) copy(left = point) else copy(right = point)

    companion object {
        fun defaults(): RcHudStickStyle = RcHudStickStyle(
            left = RcHudStickPointStyle(
                colorArgb = RcHudPlotTraceStyle.defaultColorArgb(0),
            ),
            right = RcHudStickPointStyle(
                colorArgb = RcHudPlotTraceStyle.defaultColorArgb(1),
            ),
        )
    }
}

internal fun encodeRcHudScopeStyle(style: RcHudScopeStyle): String = listOf(
    style.colorArgb.toString(),
    if (style.dashed) "1" else "0",
    if (style.fill) "1" else "0",
).joinToString(",")

internal fun decodeRcHudScopeStyle(encoded: String?, fallbackColorArgb: Int): RcHudScopeStyle {
    val parts = encoded?.split(',') ?: return RcHudScopeStyle(colorArgb = fallbackColorArgb)
    return RcHudScopeStyle(
        colorArgb = parts.getOrNull(0)?.toIntOrNull() ?: fallbackColorArgb,
        dashed = parts.getOrNull(1) == "1",
        fill = parts.getOrNull(2) != "0",
    )
}

internal fun encodeRcHudStickStyle(style: RcHudStickStyle): String = listOf(
    if (style.showGrid) "1" else "0",
    if (style.showAxes) "1" else "0",
    encodeStickPoint(style.left),
    encodeStickPoint(style.right),
).joinToString(";")

internal fun decodeRcHudStickStyle(encoded: String?): RcHudStickStyle {
    val parts = encoded?.split(';').orEmpty()
    val defaults = RcHudStickStyle.defaults()
    return RcHudStickStyle(
        showGrid = parts.getOrNull(0) != "0",
        showAxes = parts.getOrNull(1) != "0",
        left = decodeStickPoint(parts.getOrNull(2), defaults.left),
        right = decodeStickPoint(parts.getOrNull(3), defaults.right),
    )
}

internal val RcHudScopeStyleSaver = Saver<RcHudScopeStyle, String>(
    save = { encodeRcHudScopeStyle(it) },
    restore = { encoded ->
        decodeRcHudScopeStyle(encoded, RcHudPlotTraceStyle.defaultColorArgb(1))
    },
)

internal val RcHudStickStyleSaver = Saver<RcHudStickStyle, String>(
    save = { encodeRcHudStickStyle(it) },
    restore = { decodeRcHudStickStyle(it) },
)

private fun encodeStickPoint(point: RcHudStickPointStyle): String = listOf(
    if (point.visible) "1" else "0",
    point.colorArgb.toString(),
    if (point.showLabel) "1" else "0",
    if (point.showTrail) "1" else "0",
    if (point.thickTrail) "1" else "0",
    if (point.largePoint) "1" else "0",
    point.vertical?.name.orEmpty(),
    point.horizontal?.name.orEmpty(),
).joinToString(",")

private fun decodeStickPoint(
    encoded: String?,
    fallback: RcHudStickPointStyle,
): RcHudStickPointStyle {
    val parts = encoded?.split(',') ?: return fallback
    if (parts.size < 5) return fallback
    return RcHudStickPointStyle(
        visible = parts[0] != "0",
        colorArgb = parts[1].toIntOrNull() ?: fallback.colorArgb,
        showLabel = parts.getOrNull(2) != "0",
        showTrail = parts.getOrNull(3) != "0",
        thickTrail = parts.getOrNull(4) == "1",
        largePoint = parts.getOrNull(5) == "1",
        vertical = StickChannelLink.analogOrNull(TelemetryChannel.fromStored(parts.getOrNull(6))),
        horizontal = StickChannelLink.analogOrNull(TelemetryChannel.fromStored(parts.getOrNull(7))),
    )
}

@Composable
fun RcHudScopeSettingsDialog(
    title: String,
    body: String,
    selectedChannel: TelemetryChannel,
    style: RcHudScopeStyle,
    onChannelChange: (TelemetryChannel) -> Unit,
    onStyleChange: (RcHudScopeStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val accent = brandPrimary()
    val colors = PlotHudDialogColors(accent)
    CompositionLocalProvider(LocalHudGlassDialog provides true) {
        NeoDialog(
            onDismissRequest = onDismiss,
            horizontalMargin = if (isLandscape) 48.dp else 16.dp,
            surfaceColor = colors.dialogSurface,
            surfaceAlpha = 1f,
            scrimAlpha = 0.68f,
            wrapContentHeight = true,
            modifier = Modifier.widthIn(max = if (isLandscape) 520.dp else 420.dp),
            title = { NeoDialogTitle(text = title) },
            subtitle = {
                Text(
                    text = body,
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.bodyText,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
            },
            content = {
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
                                onClick = { onChannelChange(channel) },
                            )
                        }
                        repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
                PlotHudSectionLabel(
                    text = stringResource(R.string.rc_vehicle_plot_settings_color),
                    color = colors.sectionLabel,
                )
                RcHudColorRow(
                    selectedArgb = style.colorArgb,
                    onSelect = { onStyleChange(style.copy(colorArgb = it)) },
                )
                PlotHudSwitchRow(
                    label = stringResource(R.string.rc_vehicle_plot_style_dashed),
                    checked = style.dashed,
                    colors = colors,
                    accent = accent,
                    onCheckedChange = { onStyleChange(style.copy(dashed = it)) },
                )
                PlotHudSwitchRow(
                    label = stringResource(R.string.rc_vehicle_scope_fill),
                    checked = style.fill,
                    colors = colors,
                    accent = accent,
                    onCheckedChange = { onStyleChange(style.copy(fill = it)) },
                )
            },
            actions = { PlotHudDoneRow(accent = accent, onDone = onDismiss) },
        )
    }
}

@Composable
fun RcHudStickSettingsDialog(
    style: RcHudStickStyle,
    onStyleChange: (RcHudStickStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val accent = brandPrimary()
    val colors = PlotHudDialogColors(accent)
    var selectedIndex by remember { mutableIntStateOf(0) }
    val point = style.point(selectedIndex)
    fun updatePoint(updated: RcHudStickPointStyle) {
        onStyleChange(style.withPoint(selectedIndex, updated))
    }
    CompositionLocalProvider(LocalHudGlassDialog provides true) {
        NeoDialog(
            onDismissRequest = onDismiss,
            horizontalMargin = if (isLandscape) 48.dp else 16.dp,
            surfaceColor = colors.dialogSurface,
            surfaceAlpha = 1f,
            scrimAlpha = 0.68f,
            wrapContentHeight = true,
            modifier = Modifier.widthIn(max = if (isLandscape) 520.dp else 420.dp),
            title = {
                NeoDialogTitle(text = stringResource(R.string.rc_vehicle_stick_settings_title))
            },
            subtitle = {
                Text(
                    text = stringResource(R.string.rc_vehicle_stick_settings_body),
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.bodyText,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
            },
            content = {
                PlotHudSwitchRow(
                    label = stringResource(R.string.rc_vehicle_stick_show_grid),
                    checked = style.showGrid,
                    colors = colors,
                    accent = accent,
                    onCheckedChange = { onStyleChange(style.copy(showGrid = it)) },
                )
                PlotHudSwitchRow(
                    label = stringResource(R.string.rc_vehicle_stick_show_axes),
                    checked = style.showAxes,
                    colors = colors,
                    accent = accent,
                    onCheckedChange = { onStyleChange(style.copy(showAxes = it)) },
                )
                PlotHudSectionLabel(
                    text = stringResource(R.string.rc_vehicle_stick_point_section),
                    color = colors.sectionLabel,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PlotHudFilterChip(
                        label = stringResource(R.string.rc_vehicle_stick_point_left),
                        selected = selectedIndex == 0,
                        colors = colors,
                        onClick = { selectedIndex = 0 },
                    )
                    PlotHudFilterChip(
                        label = stringResource(R.string.rc_vehicle_stick_point_right),
                        selected = selectedIndex == 1,
                        colors = colors,
                        onClick = { selectedIndex = 1 },
                    )
                }
                PlotHudSectionLabel(
                    text = stringResource(R.string.rc_vehicle_plot_settings_color),
                    color = colors.sectionLabel,
                )
                RcHudColorRow(
                    selectedArgb = point.colorArgb,
                    onSelect = { updatePoint(point.copy(colorArgb = it)) },
                )
                PlotHudSwitchRow(
                    label = stringResource(R.string.rc_vehicle_plot_settings_visible),
                    checked = point.visible,
                    colors = colors,
                    accent = accent,
                    onCheckedChange = { updatePoint(point.copy(visible = it)) },
                )
                PlotHudSwitchRow(
                    label = stringResource(R.string.rc_vehicle_stick_show_value),
                    checked = point.showLabel,
                    colors = colors,
                    accent = accent,
                    onCheckedChange = { updatePoint(point.copy(showLabel = it)) },
                )
                PlotHudSwitchRow(
                    label = stringResource(R.string.rc_vehicle_stick_show_trail),
                    checked = point.showTrail,
                    colors = colors,
                    accent = accent,
                    onCheckedChange = { updatePoint(point.copy(showTrail = it)) },
                )
                PlotHudSwitchRow(
                    label = stringResource(R.string.rc_vehicle_stick_thick_trail),
                    checked = point.thickTrail,
                    colors = colors,
                    accent = accent,
                    onCheckedChange = { updatePoint(point.copy(thickTrail = it)) },
                )
                PlotHudSwitchRow(
                    label = stringResource(R.string.rc_vehicle_stick_large_point),
                    checked = point.largePoint,
                    colors = colors,
                    accent = accent,
                    onCheckedChange = { updatePoint(point.copy(largePoint = it)) },
                )
                PlotHudSectionLabel(
                    text = stringResource(R.string.rc_vehicle_plot_settings_source),
                    color = colors.sectionLabel,
                )
                Text(
                    text = stringResource(R.string.rc_vehicle_stick_channel_none_hint),
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.bodyText,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
                RcHudStickAxisChannelPicker(
                    axisLabel = stringResource(R.string.rc_joystick_axis_horizontal),
                    selected = point.horizontal,
                    colors = colors,
                    onChannelClick = { channel ->
                        updatePoint(point.togglingChannel(JoystickAxis.HORIZONTAL, channel))
                    },
                    onNoneClick = {
                        updatePoint(point.copy(horizontal = null))
                    },
                )
                RcHudStickAxisChannelPicker(
                    axisLabel = stringResource(R.string.rc_joystick_axis_vertical),
                    selected = point.vertical,
                    colors = colors,
                    onChannelClick = { channel ->
                        updatePoint(point.togglingChannel(JoystickAxis.VERTICAL, channel))
                    },
                    onNoneClick = {
                        updatePoint(point.copy(vertical = null))
                    },
                )
            },
            actions = { PlotHudDoneRow(accent = accent, onDone = onDismiss) },
        )
    }
}

@Composable
private fun RcHudStickAxisChannelPicker(
    axisLabel: String,
    selected: TelemetryChannel?,
    colors: PlotHudDialogColors,
    onChannelClick: (TelemetryChannel) -> Unit,
    onNoneClick: () -> Unit,
) {
    PlotHudSectionLabel(text = axisLabel, color = colors.sectionLabel)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PlotHudFilterChip(
            label = stringResource(R.string.rc_vehicle_stick_channel_none),
            selected = selected == null,
            colors = colors,
            onClick = onNoneClick,
        )
    }
    TelemetryChannel.ANALOG_CHANNELS.chunked(4).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            row.forEach { channel ->
                PlotHudFilterChip(
                    label = stringResource(channel.labelRes()),
                    selected = channel == selected,
                    colors = colors,
                    onClick = { onChannelClick(channel) },
                )
            }
            repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun RcHudColorRow(
    selectedArgb: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RcHudPlotTraceStyle.PALETTE_ARGB.take(8).forEach { argb ->
            val selected = selectedArgb == argb
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
                    .clickable { onSelect(argb) },
            )
        }
    }
}

@Composable
private fun PlotHudSwitchRow(
    label: String,
    checked: Boolean,
    colors: PlotHudDialogColors,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.sectionLabel,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.chipSelectedLabel,
                checkedTrackColor = accent,
                uncheckedThumbColor = colors.bodyText,
                uncheckedTrackColor = colors.chipUnselectedBg,
                uncheckedBorderColor = colors.chipUnselectedBorder,
            ),
        )
    }
}

@Composable
private fun PlotHudDoneRow(
    accent: Color,
    onDone: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = accent.copy(alpha = 0.28f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDone) {
                Text(
                    text = stringResource(R.string.control_panel_radar_settings_done),
                    color = accent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
