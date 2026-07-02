package com.micsbol.telecon4esp32.ui.watertank.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.theme.TechBlueBright
import com.micsbol.telecon4esp32.ui.theme.TechCyanBright
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme

private const val TankAspectRatio = 560f / 660f

/** Water-surface Y in frame 000 (empty) — bottom scale line for 0%. */
private const val TankZeroLevelFraction = 573f / 660f

/** Water-surface Y in frame 100 (full) — top scale line for 100%. */
private const val TankFullLevelFraction = 116f / 660f

private val ScaleWidth = 58.dp
private val PointerTrackWidth = 12.dp
private val TickTrackWidth = 8.dp

private fun levelToImageFraction(levelPercent: Int): Float {
    val level = levelPercent.coerceIn(0, 100) / 100f
    return TankZeroLevelFraction - (TankZeroLevelFraction - TankFullLevelFraction) * level
}

@Composable
fun Tank3DVisualization(
    levelPercent: Int,
    modifier: Modifier = Modifier,
    width: Dp = 140.dp,
    showScale: Boolean = true,
    animateLevel: Boolean = true,
    showGlow: Boolean = false,
) {
    val clampedLevel = levelPercent.coerceIn(0, 100)
    val animatedLevel by animateIntAsState(
        targetValue = clampedLevel,
        animationSpec = tween(durationMillis = if (animateLevel) 600 else 0),
        label = "tankLevel",
    )
    val height = width / TankAspectRatio

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(TankLevelDrawables.forLevel(animatedLevel)),
            contentDescription = stringResource(
                R.string.water_tank_visualization_content_description,
                clampedLevel,
            ),
            modifier = Modifier
                .width(width)
                .height(height)
                .then(if (showGlow) Modifier.tankGlow() else Modifier),
            contentScale = ContentScale.Fit,
        )
        if (showScale) {
            LevelScaleOverlay(
                levelPercent = animatedLevel,
                height = height,
            )
        }
    }
}

/** Soft radial halo drawn behind the tank PNG so it reads as a glowing hero element. */
private fun Modifier.tankGlow(): Modifier = drawBehind {
    val radius = size.maxDimension * 0.74f
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to TechCyanBright.copy(alpha = 0.30f),
                0.45f to TechBlueBright.copy(alpha = 0.12f),
                0.75f to TechBlueBright.copy(alpha = 0.04f),
                1f to Color.Transparent,
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

@Composable
private fun LevelScaleOverlay(
    levelPercent: Int,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val tickValues = (0..100 step 25).toList().reversed()
    val scaleColor = Color(0xFF94A3B8)
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)

    Row(
        modifier = modifier
            .width(ScaleWidth)
            .height(height),
    ) {
        Box(
            modifier = Modifier
                .width(PointerTrackWidth)
                .height(height),
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val pointerY = levelToImageFraction(levelPercent) * size.height
                val tipX = 0f
                val baseX = size.width
                val pointerPath = Path().apply {
                    moveTo(tipX, pointerY)
                    lineTo(baseX, pointerY - 5.dp.toPx())
                    lineTo(baseX, pointerY + 5.dp.toPx())
                    close()
                }
                drawPath(pointerPath, color = TechCyanBright)
            }
        }

        Box(
            modifier = Modifier
                .width(TickTrackWidth)
                .height(height),
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                tickValues.forEach { tick ->
                    val y = levelToImageFraction(tick) * size.height
                    drawLine(
                        color = scaleColor.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                val pointerY = levelToImageFraction(levelPercent) * size.height
                drawLine(
                    color = TechCyanBright.copy(alpha = 0.6f),
                    start = Offset(0f, pointerY),
                    end = Offset(size.width, pointerY),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(height),
        ) {
            tickValues.forEach { tick ->
                Text(
                    text = stringResource(R.string.water_tank_percent_value, tick),
                    style = labelStyle,
                    color = scaleColor,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(y = height * levelToImageFraction(tick) - 6.dp),
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "Tank 75% with scale",
    backgroundColor = 0xFF0A0E14,
    widthDp = 220,
    heightDp = 200,
)
@Composable
private fun Tank3DVisualization75Preview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
        ) {
            Tank3DVisualization(
                levelPercent = 60,
                animateLevel = false,
            )
        }
    }
}

@Preview(
    showBackground = true,
    name = "Tank empty",
    backgroundColor = 0xFF0A0E14,
    widthDp = 220,
    heightDp = 200,
)
@Composable
private fun Tank3DVisualizationEmptyPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
        ) {
            Tank3DVisualization(
                levelPercent = 0,
                animateLevel = false,
            )
        }
    }
}

@Preview(
    showBackground = true,
    name = "Tank full",
    backgroundColor = 0xFF0A0E14,
    widthDp = 220,
    heightDp = 200,
)
@Composable
private fun Tank3DVisualizationFullPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
        ) {
            Tank3DVisualization(
                levelPercent = 100,
                animateLevel = false,
            )
        }
    }
}

@Preview(
    showBackground = true,
    name = "Tank without scale",
    backgroundColor = 0xFF0A0E14,
    widthDp = 180,
    heightDp = 200,
)
@Composable
private fun Tank3DVisualizationNoScalePreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
        ) {
            Tank3DVisualization(
                levelPercent = 50,
                showScale = false,
                animateLevel = false,
            )
        }
    }
}
