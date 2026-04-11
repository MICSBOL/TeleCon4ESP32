package com.example.emitterapp.ui.rc_screen.components_led_style

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emitterapp.ui.rc_screen.components.IndicatorTitle

@Composable
fun BatteryStatusLedStyle(
    modifier: Modifier = Modifier,
    level: Int,
    title: String = "BATTERY",
) {
    val neonMain = Color(0xFF00E5FF)
    val neonAccent = Color(0xFF7C4DFF)
    val background = Color(0xFF050B16)
    val levelClamped = level.coerceIn(0, 100)
    val levelColor = when {
        levelClamped >= 75 -> neonMain
        levelClamped >= 50 -> neonMain.copy(alpha = 0.9f)
        levelClamped >= 25 -> neonAccent
        else -> Color(0xFFFF4444)  // Red-ish for low battery
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = modifier
                .aspectRatio(969f / 479f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w * 0.5f
                val cy = h * 0.5f
                val barWidth = w * 0.74f
                val barHeight = h * 0.55f
                val cellHorizontalPadding = barWidth * 0.020f
                val cellVerticalPadding = barHeight * 0.050f
                val availableWidth = barWidth - cellHorizontalPadding * 2f
                val cellCount = 10
                val cellGap = availableWidth * 0.024f
                val cellW = (availableWidth - cellGap * (cellCount - 1)) / cellCount
                val barRadius = cellW * 0.18f

                // Draw background container
                drawRoundRect(
                    color = background,
                    topLeft = Offset(cx - barWidth / 2f, cy - barHeight / 2f),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barRadius * 1.5f, barRadius * 1.5f)
                )

                // Draw outer border glow
                drawIntoCanvas { c ->
                    val glowP = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = w * 0.020f
                        color = neonAccent.copy(alpha = 0.3f).toArgb()
                        maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
                    }
                    val rect = android.graphics.RectF(
                        cx - barWidth / 2f,
                        cy - barHeight / 2f,
                        cx + barWidth / 2f,
                        cy + barHeight / 2f
                    )
                    c.nativeCanvas.drawRoundRect(rect, barRadius * 1.5f, barRadius * 1.5f, glowP)

                    val coreP = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = w * 0.006f
                        color = neonAccent.copy(alpha = 0.8f).toArgb()
                    }
                    c.nativeCanvas.drawRoundRect(rect, barRadius * 1.5f, barRadius * 1.5f, coreP)
                }

                // Draw battery cells
                val cellsToFill = (levelClamped / 100f * cellCount).toInt()
                val cellHeight = barHeight - cellVerticalPadding * 2f
                repeat(cellCount) { i ->
                    val cellX = cx - barWidth / 2f + cellHorizontalPadding + i * (cellW + cellGap)
                    val cellY = cy - barHeight / 2f + cellVerticalPadding
                    val isFilled = i < cellsToFill
                    val cellColor = if (isFilled) levelColor else neonAccent.copy(alpha = 0.15f)
                    val cellGlow = if (isFilled) 16f else 6f

                    drawGlowCell(
                        topLeft = Offset(cellX, cellY),
                        cellSize = cellHeight,
                        cellWidth = cellW,
                        color = cellColor,
                        glow = cellGlow,
                        coreStroke = if (isFilled) w * 0.005f else w * 0.003f
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        IndicatorTitle(
            text = "$title: $levelClamped%",
            textColor = neonMain,
            glowColor = neonMain.copy(alpha = 0.6f),
            textSize = 9.sp,
            showFrame = true
        )
    }
}

private fun DrawScope.drawGlowCell(
    topLeft: Offset,
    cellSize: Float,
    cellWidth: Float,
    color: Color,
    glow: Float,
    coreStroke: Float
) {
    drawIntoCanvas { canvas ->
        val cornerR = cellWidth * 0.18f
        val rect = android.graphics.RectF(
            topLeft.x,
            topLeft.y,
            topLeft.x + cellWidth,
            topLeft.y + cellSize
        )

        if (glow > 0f) {
            val glowP = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = coreStroke * 3.2f
                this.color = color.copy(alpha = 0.35f).toArgb()
                maskFilter = BlurMaskFilter(glow, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawRoundRect(rect, cornerR, cornerR, glowP)
        }

        val coreP = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = coreStroke
            this.color = color.toArgb()
        }
        canvas.nativeCanvas.drawRoundRect(rect, cornerR, cornerR, coreP)
    }
}

private fun DrawScope.drawGlowCircle(
    center: Offset,
    radius: Float,
    color: Color,
    glow: Float,
    coreStroke: Float
) {
    drawIntoCanvas { canvas ->
        if (glow > 0f) {
            val glowPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                this.color = color.copy(alpha = 0.35f).toArgb()
                strokeWidth = coreStroke * 3f
                maskFilter = BlurMaskFilter(glow, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawCircle(center.x, center.y, radius, glowPaint)
        }

        val corePaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            this.color = color.toArgb()
            strokeWidth = coreStroke
        }
        canvas.nativeCanvas.drawCircle(center.x, center.y, radius, corePaint)
    }
}

@Preview(showBackground = true)
@Composable
private fun BatteryStatusLedStylePreview() {
    Column {
        BatteryStatusLedStyle(level = 100, title = "BATTERY")
        BatteryStatusLedStyle(level = 75, title = "BATTERY")
        BatteryStatusLedStyle(level = 50, title = "BATTERY")
        BatteryStatusLedStyle(level = 25, title = "BATTERY")
        BatteryStatusLedStyle(level = 10, title = "BATTERY")
        BatteryStatusLedStyle(level = 0, title = "BATTERY")
    }
}

