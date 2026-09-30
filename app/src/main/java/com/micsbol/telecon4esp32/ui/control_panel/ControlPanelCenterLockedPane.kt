package com.micsbol.telecon4esp32.ui.control_panel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ControlPanelCenterMode
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.theme.PlotCyan
import com.micsbol.telecon4esp32.ui.theme.PlotOrange
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ControlPanelCenterLockedPane(
    mode: ControlPanelCenterMode,
    onUnlockClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xCC0A1018)),
        contentAlignment = Alignment.Center,
    ) {
        if (mode.showsFeaturePreview) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            ) {
                FeaturePreview(mode = mode, modifier = Modifier.size(148.dp))
                Column(
                    modifier = Modifier.widthIn(max = 280.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LockedPaneCopy(mode = mode, onUnlockClick = onUnlockClick)
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = brandPrimary(),
                    modifier = Modifier.size(32.dp),
                )
                LockedPaneCopy(mode = mode, onUnlockClick = onUnlockClick)
            }
        }
    }
}

@Composable
private fun LockedPaneCopy(
    mode: ControlPanelCenterMode,
    onUnlockClick: () -> Unit,
) {
    Text(
        text = stringResource(R.string.control_panel_center_locked_title, stringResource(mode.titleRes)),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        textAlign = TextAlign.Center,
    )
    mode.lockedDetailRes?.let { detailRes ->
        Text(
            text = stringResource(detailRes),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
        )
    }
    Text(
        text = stringResource(R.string.control_panel_center_locked_message),
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.75f),
        textAlign = TextAlign.Center,
    )
    TextButton(onClick = onUnlockClick) {
        Text(
            text = stringResource(R.string.control_panel_center_unlock),
            color = brandPrimary(),
            fontWeight = FontWeight.Bold,
        )
    }
}

private val ControlPanelCenterMode.showsFeaturePreview: Boolean
    get() = this == ControlPanelCenterMode.STICK ||
        this == ControlPanelCenterMode.CAMERA ||
        this == ControlPanelCenterMode.RADAR

private val ControlPanelCenterMode.lockedDetailRes: Int?
    get() = when (this) {
        ControlPanelCenterMode.STICK -> R.string.control_panel_center_locked_stick_message
        ControlPanelCenterMode.CAMERA -> R.string.control_panel_center_locked_camera_message
        ControlPanelCenterMode.RADAR -> R.string.control_panel_center_locked_radar_message
        ControlPanelCenterMode.PLOTS -> null
    }

@Composable
private fun FeaturePreview(
    mode: ControlPanelCenterMode,
    modifier: Modifier = Modifier,
) {
    when (mode) {
        ControlPanelCenterMode.STICK -> StickMotionPreview(modifier)
        ControlPanelCenterMode.CAMERA -> CameraScenePreview(modifier)
        ControlPanelCenterMode.RADAR -> RadarSweepPreview(modifier)
        ControlPanelCenterMode.PLOTS -> Unit
    }
}

/** Sample motion so a locked stick pane still shows what the graph is for. */
@Composable
private fun StickMotionPreview(modifier: Modifier = Modifier) {
    val leftTrail = remember { mutableStateListOf<Pair<Float, Float>>() }
    val rightTrail = remember { mutableStateListOf<Pair<Float, Float>>() }
    LaunchedEffect(Unit) {
        var phase = 0.0
        while (true) {
            val left = Pair(
                (cos(phase) * 0.72).toFloat(),
                (sin(phase) * 0.72).toFloat(),
            )
            val right = Pair(
                (sin(phase * 0.7) * 0.48).toFloat(),
                (cos(phase * 1.25) * 0.58).toFloat(),
            )
            pushPreviewTrail(leftTrail, left)
            pushPreviewTrail(rightTrail, right)
            phase += 0.22
            if (phase > PI * 8) phase -= PI * 8
            delay(70)
        }
    }
    val gridColor = Color.White.copy(alpha = 0.28f)
    val axisColor = Color.White.copy(alpha = 0.55f)
    Canvas(modifier = modifier) {
        val leftNow = leftTrail.lastOrNull() ?: Pair(0f, 0f)
        val rightNow = rightTrail.lastOrNull() ?: Pair(0f, 0f)
        drawStickGraph(
            leftNow = leftNow,
            rightNow = rightNow,
            leftTrail = leftTrail.toList(),
            rightTrail = rightTrail.toList(),
            leftColor = PlotCyan,
            rightColor = PlotOrange,
            gridColor = gridColor,
            axisColor = axisColor,
            showLabels = false,
        )
    }
}

private fun pushPreviewTrail(
    trail: MutableList<Pair<Float, Float>>,
    point: Pair<Float, Float>,
) {
    trail.add(point)
    while (trail.size > 48) {
        trail.removeAt(0)
    }
}

/** Moving viewfinder so a locked camera pane still shows a live picture. */
@Composable
private fun CameraScenePreview(modifier: Modifier = Modifier) {
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            phase += 0.045f
            if (phase > PI.toFloat() * 2f) phase -= PI.toFloat() * 2f
            delay(40)
        }
    }
    val frameColor = brandPrimary()
    Canvas(modifier = modifier) {
        val inset = 8.dp.toPx()
        val frame = Size(size.width - inset * 2f, size.height - inset * 2f)
        val topLeft = Offset(inset, inset)
        val radius = CornerRadius(10.dp.toPx())
        drawRoundRect(
            color = Color(0xFF101820),
            topLeft = topLeft,
            size = frame,
            cornerRadius = radius,
        )
        val horizon = topLeft.y + frame.height * 0.58f
        drawRect(
            color = Color(0xFF1A3348),
            topLeft = Offset(topLeft.x + 2f, topLeft.y + 2f),
            size = Size(frame.width - 4f, horizon - topLeft.y - 2f),
        )
        drawRect(
            color = Color(0xFF243028),
            topLeft = Offset(topLeft.x + 2f, horizon),
            size = Size(frame.width - 4f, topLeft.y + frame.height - horizon - 2f),
        )
        val subjectX = topLeft.x + frame.width * (0.5f + sin(phase) * 0.28f)
        val subjectY = horizon - 16.dp.toPx()
        drawCircle(
            color = PlotOrange,
            radius = 9.dp.toPx(),
            center = Offset(subjectX, subjectY),
        )
        drawRoundRect(
            color = frameColor,
            topLeft = topLeft,
            size = frame,
            cornerRadius = radius,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

/** Sweeping beam so a locked radar pane still shows how the scan works. */
@Composable
private fun RadarSweepPreview(modifier: Modifier = Modifier) {
    var bearing by remember { mutableFloatStateOf(-80f) }
    var forward by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            bearing += if (forward) 2.4f else -2.4f
            when {
                bearing >= 80f -> forward = false
                bearing <= -80f -> forward = true
            }
            delay(30)
        }
    }
    val accent = brandPrimary()
    val hits = listOf(-40f to 0.55f, 18f to 0.78f, 52f to 0.38f)
    Canvas(modifier = modifier) {
        val origin = Offset(size.width / 2f, size.height * 0.78f)
        val radius = size.minDimension * 0.42f
        val grid = accent.copy(alpha = 0.45f)
        listOf(0.34f, 0.67f, 1f).forEach { fraction ->
            drawArc(
                color = grid,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(origin.x - radius * fraction, origin.y - radius * fraction),
                size = Size(radius * fraction * 2f, radius * fraction * 2f),
                style = Stroke(width = 1.2f),
            )
        }
        listOf(-80f, -40f, 0f, 40f, 80f).forEach { degrees ->
            val radians = Math.toRadians(degrees.toDouble())
            drawLine(
                color = grid,
                start = origin,
                end = Offset(
                    origin.x + (sin(radians) * radius).toFloat(),
                    origin.y - (cos(radians) * radius).toFloat(),
                ),
                strokeWidth = 1.1f,
            )
        }
        val beam = Path().apply {
            moveTo(origin.x, origin.y)
            val half = 16.0
            val start = Math.toRadians((bearing - half))
            val end = Math.toRadians((bearing + half))
            lineTo(
                origin.x + (sin(start) * radius).toFloat(),
                origin.y - (cos(start) * radius).toFloat(),
            )
            lineTo(
                origin.x + (sin(end) * radius).toFloat(),
                origin.y - (cos(end) * radius).toFloat(),
            )
            close()
        }
        drawPath(path = beam, color = accent.copy(alpha = 0.28f))
        hits.forEach { (angle, range) ->
            val radians = Math.toRadians(angle.toDouble())
            val distance = radius * range
            val center = Offset(
                origin.x + (sin(radians) * distance).toFloat(),
                origin.y - (cos(radians) * distance).toFloat(),
            )
            val near = abs(angle - bearing) < 18f
            drawCircle(
                color = PlotOrange.copy(alpha = if (near) 0.95f else 0.35f),
                radius = if (near) 5.dp.toPx() else 3.5.dp.toPx(),
                center = center,
            )
        }
    }
}
