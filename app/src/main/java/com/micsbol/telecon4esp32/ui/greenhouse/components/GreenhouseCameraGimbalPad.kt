package com.micsbol.telecon4esp32.ui.greenhouse.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.GhCameraGimbal
import com.micsbol.telecon4esp32.ui.greenhouse.GreenhouseGlass
import kotlin.math.roundToInt

/**
 * Two-axis spring pad for greenhouse camera pan (X) and tilt (Y).
 * Values are 0…100 with 50 = center; drag up increases tilt.
 */
@Composable
fun GreenhouseCameraGimbalPad(
    panPercent: Int,
    tiltPercent: Int,
    onGimbalChange: (panPercent: Int, tiltPercent: Int) -> Unit,
    onGimbalChangeFinished: () -> Unit,
    onCenterClick: () -> Unit,
    modifier: Modifier = Modifier,
    padSize: Dp = 132.dp,
    enabled: Boolean = true,
) {
    val description = stringResource(
        R.string.greenhouse_camera_gimbal_value,
        panPercent,
        tiltPercent,
    )
    val currentOnChange by rememberUpdatedState(onGimbalChange)
    val currentOnFinished by rememberUpdatedState(onGimbalChangeFinished)
    val density = LocalDensity.current
    val maxKnobTravelPx = with(density) { (padSize / 2f - 22.dp).toPx() }

    val knobOffsetX = GhCameraGimbal.toNormalizedAxis(panPercent) * maxKnobTravelPx
    // Screen Y grows downward; tilt up should move knob up.
    val knobOffsetY = -GhCameraGimbal.toNormalizedAxis(tiltPercent) * maxKnobTravelPx
    val surfaceAlpha = if (enabled) 0.48f else 0.28f
    val knobAlpha = if (enabled) 0.92f else 0.45f

    Column(
        modifier = modifier.semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.greenhouse_camera_gimbal_label),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = if (enabled) 0.85f else 0.45f),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(padSize)
                .clip(GreenhouseGlass.SmallCardShape)
                .background(Color.Black.copy(alpha = surfaceAlpha))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = if (enabled) 0.28f else 0.14f),
                    shape = GreenhouseGlass.SmallCardShape,
                )
                .then(
                    if (enabled) {
                        Modifier.pointerInput(maxKnobTravelPx) {
                            fun applyPointer(position: Offset) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = (position.x - center.x)
                                    .coerceIn(-maxKnobTravelPx, maxKnobTravelPx)
                                val dy = (position.y - center.y)
                                    .coerceIn(-maxKnobTravelPx, maxKnobTravelPx)
                                val pan = GhCameraGimbal.fromNormalizedAxis(dx / maxKnobTravelPx)
                                val tilt = GhCameraGimbal.fromNormalizedAxis(-dy / maxKnobTravelPx)
                                currentOnChange(pan, tilt)
                            }
                            detectDragGestures(
                                onDragStart = { offset -> applyPointer(offset) },
                                onDragEnd = { currentOnFinished() },
                                onDragCancel = { currentOnFinished() },
                                onDrag = { change, _ ->
                                    change.consume()
                                    applyPointer(change.position)
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.matchParentSize().padding(10.dp)) {
                val strokeWidth = 1.5.dp.toPx()
                val crossColor = Color.White.copy(alpha = if (enabled) 0.22f else 0.12f)
                drawLine(
                    color = crossColor,
                    start = Offset(size.width / 2f, 0f),
                    end = Offset(size.width / 2f, size.height),
                    strokeWidth = strokeWidth,
                )
                drawLine(
                    color = crossColor,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = strokeWidth,
                )
                drawCircle(
                    color = GreenhouseGlass.LeafBright.copy(alpha = if (enabled) 0.35f else 0.18f),
                    radius = size.minDimension * 0.12f,
                    style = Stroke(width = strokeWidth),
                )
            }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(knobOffsetX.roundToInt(), knobOffsetY.roundToInt())
                    }
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GreenhouseGlass.AccentGreen.copy(alpha = knobAlpha))
                    .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(enabled = enabled, onClick = onCenterClick)
                .background(Color.Black.copy(alpha = surfaceAlpha))
                .border(
                    1.dp,
                    Color.White.copy(alpha = if (enabled) 0.28f else 0.14f),
                    CircleShape,
                )
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = stringResource(R.string.greenhouse_camera_gimbal_center),
                tint = GreenhouseGlass.LeafLime.copy(alpha = if (enabled) 1f else 0.45f),
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.greenhouse_camera_gimbal_readout,
                panPercent,
                tiltPercent,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = if (enabled) 0.75f else 0.4f),
        )
    }
}
