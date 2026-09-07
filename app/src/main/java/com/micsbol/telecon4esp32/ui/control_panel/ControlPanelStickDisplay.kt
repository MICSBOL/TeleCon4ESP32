package com.micsbol.telecon4esp32.ui.control_panel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.domain.model.JoystickAxis
import com.micsbol.telecon4esp32.domain.model.StickChannelLink
import com.micsbol.telecon4esp32.domain.model.TelemetryChannel
import com.micsbol.telecon4esp32.domain.model.displayHistory
import com.micsbol.telecon4esp32.domain.model.displayXy
import com.micsbol.telecon4esp32.ui.components.NeoDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialogTextAction
import com.micsbol.telecon4esp32.ui.components.NeoDialogTitle
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.rc_settings.labelRes
import com.micsbol.telecon4esp32.ui.theme.Neo
import com.micsbol.telecon4esp32.ui.theme.PlotCyan
import com.micsbol.telecon4esp32.ui.theme.PlotOrange
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

private const val StickTrailMaxPoints = 80
private const val StickTrailEpsilon = 0.008f

internal val StickChannelLinkSaver = Saver<StickChannelLink, String>(
    save = { it.encode() },
    restore = { StickChannelLink.decode(it) },
)

@Composable
fun ControlPanelStickDisplay(
    leftStickXy: Pair<Float, Float>,
    rightStickXy: Pair<Float, Float>,
    leftLink: StickChannelLink,
    rightLink: StickChannelLink,
    onLeftLinkChange: (StickChannelLink) -> Unit,
    onRightLinkChange: (StickChannelLink) -> Unit,
    modifier: Modifier = Modifier,
    u8Series: List<PlotData> = emptyList(),
    showSettings: Boolean = false,
    onShowSettingsChange: (Boolean) -> Unit = {},
    showSettingsChip: Boolean = true,
) {
    val lastU8: (TelemetryChannel) -> Float? = { channel ->
        u8Series.getOrNull(channel.u8Index())?.dataPoints?.lastOrNull()
    }
    val historyU8: (TelemetryChannel) -> List<Float>? = { channel ->
        u8Series.getOrNull(channel.u8Index())?.dataPoints?.takeIf { it.isNotEmpty() }
    }
    val leftNow = leftLink.displayXy(leftStickXy.first, leftStickXy.second, lastU8)
    val rightNow = rightLink.displayXy(rightStickXy.first, rightStickXy.second, lastU8)
    val leftTrail = remember { mutableStateListOf<Pair<Float, Float>>() }
    val rightTrail = remember { mutableStateListOf<Pair<Float, Float>>() }
    LaunchedEffect(leftNow) {
        appendStickTrail(leftTrail, leftNow)
    }
    LaunchedEffect(rightNow) {
        appendStickTrail(rightTrail, rightNow)
    }
    val leftDrawnTrail = stickGraphTrail(leftLink, leftTrail, historyU8)
    val rightDrawnTrail = stickGraphTrail(rightLink, rightTrail, historyU8)
    val gridColor = Color.White.copy(alpha = 0.28f)
    val axisColor = Color.White.copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val plotSize = minOf(maxWidth, maxHeight)
            Canvas(modifier = Modifier.size(plotSize)) {
                drawStickGraph(
                    leftNow = leftNow,
                    rightNow = rightNow,
                    leftTrail = leftDrawnTrail,
                    rightTrail = rightDrawnTrail,
                    leftColor = PlotCyan,
                    rightColor = PlotOrange,
                    gridColor = gridColor,
                    axisColor = axisColor,
                )
            }
        }
        if (showSettingsChip) {
            StickGraphSettingsChip(
                onClick = { onShowSettingsChange(true) },
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
    if (showSettings) {
        ControlPanelStickSettingsDialog(
            leftLink = leftLink,
            rightLink = rightLink,
            onLeftLinkChange = onLeftLinkChange,
            onRightLinkChange = onRightLinkChange,
            onDismiss = { onShowSettingsChange(false) },
        )
    }
}

@Composable
internal fun StickGraphSettingsChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = brandPrimary()
    val shape = RoundedCornerShape(12.dp)
    val description = stringResource(R.string.control_panel_stick_settings_content_description)
    Box(
        modifier = modifier
            .padding(end = 4.dp, top = 2.dp, start = 6.dp, bottom = 6.dp)
            .clip(shape)
            .background(Color(0xE6121824))
            .border(1.dp, accent.copy(alpha = 0.45f), shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics {
                role = Role.Button
                contentDescription = description
            }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun ControlPanelStickSettingsDialog(
    leftLink: StickChannelLink,
    rightLink: StickChannelLink,
    onLeftLinkChange: (StickChannelLink) -> Unit,
    onRightLinkChange: (StickChannelLink) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val link = if (selectedIndex == 0) leftLink else rightLink
    val onLinkChange = if (selectedIndex == 0) onLeftLinkChange else onRightLinkChange
    val accent = brandPrimary()
    NeoDialog(
        onDismissRequest = onDismiss,
        horizontalMargin = 48.dp,
        title = {
            NeoDialogTitle(text = stringResource(R.string.rc_vehicle_stick_settings_title))
        },
        content = {
            Text(
                text = stringResource(R.string.rc_vehicle_stick_channel_none_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Neo.TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StickSettingsStickChip(
                    label = stringResource(R.string.rc_vehicle_stick_point_left),
                    selected = selectedIndex == 0,
                    accent = accent,
                    onClick = { selectedIndex = 0 },
                    modifier = Modifier.weight(1f),
                )
                StickSettingsStickChip(
                    label = stringResource(R.string.rc_vehicle_stick_point_right),
                    selected = selectedIndex == 1,
                    accent = accent,
                    onClick = { selectedIndex = 1 },
                    modifier = Modifier.weight(1f),
                )
            }
            StickSettingsAxisPicker(
                axisLabel = stringResource(R.string.rc_joystick_axis_horizontal),
                selected = link.horizontal,
                accent = accent,
                onChannelClick = { channel ->
                    onLinkChange(link.toggling(JoystickAxis.HORIZONTAL, channel))
                },
            )
            StickSettingsAxisPicker(
                axisLabel = stringResource(R.string.rc_joystick_axis_vertical),
                selected = link.vertical,
                accent = accent,
                onChannelClick = { channel ->
                    onLinkChange(link.toggling(JoystickAxis.VERTICAL, channel))
                },
            )
        },
        actions = {
            NeoDialogTextAction(
                text = stringResource(R.string.control_panel_radar_settings_done),
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun StickSettingsStickChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.22f) else Color.Transparent)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else accent.copy(alpha = 0.28f),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) accent else Neo.TextSecondary,
        )
    }
}

@Composable
private fun StickSettingsAxisPicker(
    axisLabel: String,
    selected: TelemetryChannel?,
    accent: Color,
    onChannelClick: (TelemetryChannel) -> Unit,
) {
    Text(
        text = axisLabel,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = Neo.TextSecondary,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
    )
    TelemetryChannel.ANALOG_CHANNELS.chunked(4).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            row.forEach { channel ->
                StickSettingsChannelChip(
                    label = stringResource(channel.labelRes()),
                    selected = channel == selected,
                    accent = accent,
                    onClick = { onChannelClick(channel) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StickSettingsChannelChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.22f) else Color.Transparent)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else accent.copy(alpha = 0.28f),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) accent else Neo.TextSecondary,
        )
    }
}

private fun stickGraphTrail(
    link: StickChannelLink,
    liveTrail: List<Pair<Float, Float>>,
    history01: (TelemetryChannel) -> List<Float>?,
): List<Pair<Float, Float>> {
    val (xs, ys) = link.displayHistory(
        stickX = liveTrail.map { it.first },
        stickY = liveTrail.map { it.second },
        history01 = history01,
    )
    val count = minOf(xs.size, ys.size)
    if (count == 0) return liveTrail
    return List(count) { index -> Pair(xs[index], ys[index]) }
}

private fun DrawScope.drawStickGraph(
    leftNow: Pair<Float, Float>,
    rightNow: Pair<Float, Float>,
    leftTrail: List<Pair<Float, Float>>,
    rightTrail: List<Pair<Float, Float>>,
    leftColor: Color,
    rightColor: Color,
    gridColor: Color,
    axisColor: Color,
) {
    val pad = 10.dp.toPx()
    val side = min(size.width, size.height) - pad * 2f
    if (side <= 0f) return
    val left = (size.width - side) / 2f
    val top = (size.height - side) / 2f
    val right = left + side
    val bottom = top + side
    val cx = left + side / 2f
    val cy = top + side / 2f
    fun mapX(value: Float) = cx + value.coerceIn(-1f, 1f) * side / 2f
    fun mapY(value: Float) = cy - value.coerceIn(-1f, 1f) * side / 2f

    drawRect(
        color = gridColor,
        topLeft = Offset(left, top),
        size = Size(side, side),
        style = Stroke(width = 1.4f),
    )
    listOf(-0.5f, 0.5f).forEach { tick ->
        val x = mapX(tick)
        val y = mapY(tick)
        drawLine(gridColor, Offset(x, top), Offset(x, bottom), strokeWidth = 1f)
        drawLine(gridColor, Offset(left, y), Offset(right, y), strokeWidth = 1f)
    }
    drawLine(axisColor, Offset(left, cy), Offset(right, cy), strokeWidth = 1.6f)
    drawLine(axisColor, Offset(cx, top), Offset(cx, bottom), strokeWidth = 1.6f)
    drawCircle(
        color = gridColor.copy(alpha = 0.45f),
        radius = side / 2f,
        center = Offset(cx, cy),
        style = Stroke(
            width = 1.2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
        ),
    )
    val pointsClose = abs(leftNow.first - rightNow.first) < 0.12f &&
        abs(leftNow.second - rightNow.second) < 0.12f
    drawStickTrailAndPoint(
        trail = leftTrail,
        now = leftNow,
        color = leftColor,
        mapX = ::mapX,
        mapY = ::mapY,
        preferLabelRight = false,
        verticalBias = if (pointsClose) -1f else 0f,
    )
    drawStickTrailAndPoint(
        trail = rightTrail,
        now = rightNow,
        color = rightColor,
        mapX = ::mapX,
        mapY = ::mapY,
        preferLabelRight = true,
        verticalBias = if (pointsClose) 1f else 0f,
    )
}

private fun DrawScope.drawStickTrailAndPoint(
    trail: List<Pair<Float, Float>>,
    now: Pair<Float, Float>,
    color: Color,
    mapX: (Float) -> Float,
    mapY: (Float) -> Float,
    preferLabelRight: Boolean,
    verticalBias: Float,
) {
    if (trail.size > 1) {
        val path = Path()
        trail.forEachIndexed { index, sample ->
            val x = mapX(sample.first)
            val y = mapY(sample.second)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color.copy(alpha = 0.22f),
            style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
    val point = Offset(mapX(now.first), mapY(now.second))
    drawCircle(color = color, radius = 6.5f, center = point)
    drawCircle(color = Color.White, radius = 2.6f, center = point)
    drawStickXyLabel(
        text = formatControlPanelStickXy(now.first, now.second),
        point = point,
        color = color,
        preferRight = preferLabelRight,
        verticalBias = verticalBias,
    )
}

private fun DrawScope.drawStickXyLabel(
    text: String,
    point: Offset,
    color: Color,
    preferRight: Boolean,
    verticalBias: Float,
) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            this.color = color.toArgb()
            textSize = 9.dp.toPx()
            isAntiAlias = true
            isFakeBoldText = true
        }
        val gap = 8.dp.toPx()
        val labelWidth = paint.measureText(text)
        var toRight = preferRight
        if (toRight && point.x + gap + labelWidth > size.width - 4f) toRight = false
        if (!toRight && point.x - gap - labelWidth < 4f) toRight = true
        paint.textAlign = if (toRight) {
            android.graphics.Paint.Align.LEFT
        } else {
            android.graphics.Paint.Align.RIGHT
        }
        val x = if (toRight) point.x + gap else point.x - gap
        val y = point.y + paint.textSize * 0.35f + verticalBias * (paint.textSize + 4.dp.toPx())
        canvas.nativeCanvas.drawText(text, x, y, paint)
    }
}

private fun appendStickTrail(
    trail: MutableList<Pair<Float, Float>>,
    point: Pair<Float, Float>,
) {
    val last = trail.lastOrNull()
    if (
        last != null &&
        abs(last.first - point.first) < StickTrailEpsilon &&
        abs(last.second - point.second) < StickTrailEpsilon
    ) {
        return
    }
    trail.add(point)
    while (trail.size > StickTrailMaxPoints) {
        trail.removeAt(0)
    }
}

internal fun formatControlPanelStickXy(x: Float, y: Float): String =
    "(%.2f,%.2f)".format(
        Locale.US,
        x.coerceIn(-1f, 1f),
        y.coerceIn(-1f, 1f),
    )
