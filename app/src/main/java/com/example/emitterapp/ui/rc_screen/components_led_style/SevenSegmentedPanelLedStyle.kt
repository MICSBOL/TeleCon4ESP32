package com.example.emitterapp.ui.rc_screen.components_led_style

import android.annotation.SuppressLint
import android.graphics.BlurMaskFilter
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun SevenSegmentedPanelLedStyle(
    value: Float,
    on: Boolean,
    modifier: Modifier = Modifier,
    width: Dp = 400.dp,
    title: String = "",
    neonColor: Color = Color(0xFF00E5FF),
    backgroundColor: Color = Color(0xFF050B16),
) {
    val onColorAlpha = neonColor.copy(alpha = 0.25f)

    val clampedValue = value.coerceIn(0f, 9999.9f)
    val formatted = String.format(java.util.Locale.US, "%06.1f", clampedValue)
    val digits = formatted.replace(".", "")
    val autoDecimalPoints = listOf(false, false, false, true, false)

    BoxWithConstraints(
        modifier = modifier.size(height = width * 0.66f, width = width),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(maxWidth * 0.9f)
                .height(maxHeight * 0.7f)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val numDigits = 5
                val digitWidth = size.width * 0.13f
                val digitHeight = digitWidth * 2f
                val digitSpacing = size.width * 0.03f
                val totalWidth = (numDigits * digitWidth) + ((numDigits - 1) * digitSpacing)
                val startX = (size.width - totalWidth) / 2
                val startY = (size.height - digitHeight) / 3
                val frameStroke = digitWidth * 0.06f
                val framePadding = digitWidth * 0.5f
                val frameBottomPadding = digitWidth * 0.9f

                // Draw frame around the numbers with extra bottom padding
                drawGlowFrame(
                    topLeft = Offset(startX - framePadding, startY - framePadding),
                    width = totalWidth + (framePadding * 2),
                    height = digitHeight + framePadding + frameBottomPadding,
                    color = neonColor,
                    glowRadius = 16f,
                    stroke = frameStroke
                )

                digits.take(numDigits).forEachIndexed { index, digitChar ->
                    val digitTopLeft = Offset(
                        x = startX + index * (digitWidth + digitSpacing),
                        y = startY
                    )

                    val isLeadingZero = index < 3 && 
                                        digitChar == '0' && 
                                        digits.substring(0, index + 1).all { it == '0' }

                    drawDigitLedStyle(
                        digit = digitChar,
                        topLeft = digitTopLeft,
                        width = digitWidth,
                        height = digitHeight,
                        on = on && !isLeadingZero,
                        neonColor = neonColor,
                        offColor = onColorAlpha,
                        blurRadius = 28f
                    )
                }

                val pointRadius = digitWidth / 14f
                val pointY = startY + digitHeight - pointRadius

                autoDecimalPoints.forEachIndexed { index, isPointOn ->
                    if (isPointOn && on) {
                        val pointX = startX + (index + 1) * digitWidth + (index * digitSpacing) + (digitSpacing / 2)
                        val pointCenter = Offset(pointX, pointY)

                        drawLedPoint(
                            center = pointCenter,
                            radius = pointRadius,
                            color = neonColor,
                            glowRadius = 18f
                        )
                    }
                }

                // Draw unit label inside the frame at the bottom
                if (title.isNotEmpty()) {
                    drawUnitLabelInFrame(
                        title = title,
                        frameLabelX = startX - framePadding + (totalWidth + (framePadding * 2)) - (framePadding * 0.35f),
                        frameLabelY = startY + digitHeight + frameBottomPadding - (framePadding * 0.85f),
                        on = on,
                        neonColor = neonColor,
                        digitWidth = digitWidth
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawDigitLedStyle(
    digit: Char,
    topLeft: Offset,
    width: Float,
    height: Float,
    on: Boolean,
    neonColor: Color,
    offColor: Color,
    blurRadius: Float
) {
    val segmentsToDraw = digitToSegmentLed[digit] ?: emptyList()
    val strokeWidth = width / 7f

    val glowPaint = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeJoin = android.graphics.Paint.Join.ROUND
        strokeCap = android.graphics.Paint.Cap.ROUND
        this.strokeWidth = strokeWidth
        this.color = neonColor.toArgb()
        maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
    }

    val segmentPaths = getSegmentPathsLed(topLeft, width, height, strokeWidth)
    
    // Draw all segments off (dim)
    segmentPaths.values.forEach { path ->
        drawPath(path = path, color = offColor, style = Stroke(width = strokeWidth))
    }

    // Draw active segments with glow
    if (on) {
        segmentsToDraw.forEach { segment ->
            val path = segmentPaths[segment] ?: return@forEach

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawPath(path.asAndroidPath(), glowPaint)
            }
            drawPath(
                path = path,
                color = neonColor,
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

private fun DrawScope.drawGlowFrame(
    topLeft: Offset,
    width: Float,
    height: Float,
    color: Color,
    glowRadius: Float,
    stroke: Float
) {
    drawIntoCanvas { canvas ->
        val glowPaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeJoin = android.graphics.Paint.Join.ROUND
            strokeCap = android.graphics.Paint.Cap.ROUND
            this.strokeWidth = stroke
            this.color = color.copy(alpha = 0.5f).toArgb()
            maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
        }

        val rect = android.graphics.RectF(topLeft.x, topLeft.y, topLeft.x + width, topLeft.y + height)
        canvas.nativeCanvas.drawRect(rect, glowPaint)

        val corePaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeJoin = android.graphics.Paint.Join.ROUND
            strokeCap = android.graphics.Paint.Cap.ROUND
            this.strokeWidth = stroke
            this.color = color.toArgb()
        }
        canvas.nativeCanvas.drawRect(rect, corePaint)
    }
}

private fun DrawScope.drawLedPoint(
    center: Offset,
    radius: Float,
    color: Color,
    glowRadius: Float
) {
    drawIntoCanvas { canvas ->
        val glowPaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
            this.color = color.copy(alpha = 0.5f).toArgb()
            maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.nativeCanvas.drawCircle(center.x, center.y, radius, glowPaint)

        val corePaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
            this.color = color.toArgb()
        }
        canvas.nativeCanvas.drawCircle(center.x, center.y, radius, corePaint)
    }
}

private fun getSegmentPathsLed(
    topLeft: Offset,
    width: Float,
    height: Float,
    stroke: Float
): Map<SegmentLed, Path> {
    val halfStroke = stroke / 2f
    val x = topLeft.x
    val y = topLeft.y

    val pathA = Path().apply {
        moveTo(x + halfStroke + stroke, y + halfStroke)
        lineTo(x + width - halfStroke - stroke, y + halfStroke)
    }
    val pathB = Path().apply {
        moveTo(x + width - halfStroke, y + halfStroke + stroke)
        lineTo(x + width - halfStroke, y + (height / 2) - (halfStroke / 2))
    }
    val pathC = Path().apply {
        moveTo(x + width - halfStroke, y + (height / 2) + (halfStroke / 2))
        lineTo(x + width - halfStroke, y + height - halfStroke - stroke)
    }
    val pathD = Path().apply {
        moveTo(x + halfStroke + stroke, y + height - halfStroke)
        lineTo(x + width - halfStroke - stroke, y + height - halfStroke)
    }
    val pathE = Path().apply {
        moveTo(x + halfStroke, y + (height / 2) + (halfStroke / 2))
        lineTo(x + halfStroke, y + height - halfStroke - stroke)
    }
    val pathF = Path().apply {
        moveTo(x + halfStroke, y + halfStroke + stroke)
        lineTo(x + halfStroke, y + (height / 2) - (halfStroke / 2))
    }
    val pathG = Path().apply {
        moveTo(x + halfStroke + stroke, y + (height / 2))
        lineTo(x + width - halfStroke - stroke, y + (height / 2))
    }
    return mapOf(
        SegmentLed.A to pathA, SegmentLed.B to pathB, SegmentLed.C to pathC,
        SegmentLed.D to pathD, SegmentLed.E to pathE, SegmentLed.F to pathF, SegmentLed.G to pathG
    )
}

@Preview(showBackground = true)
@Composable
fun SevenSegmentedPanelLedStylePreview() {
    Column {
        SevenSegmentedPanelLedStyle(
            title = "RPM",
            value = 1234.5f,
            on = true,
            neonColor = Color(0xFF00E5FF),
            backgroundColor = Color(0xFF050B16)
        )
        SevenSegmentedPanelLedStyle(
            title = "VOLTS",
            value = 12.3f,
            on = true,
            neonColor = Color(0xFF00FF00),
            backgroundColor = Color(0xFF0A0A0A)
        )
    }
}

private fun DrawScope.drawUnitLabelInFrame(
    title: String,
    frameLabelX: Float,
    frameLabelY: Float,
    on: Boolean,
    neonColor: Color,
    digitWidth: Float
) {
    drawIntoCanvas { canvas ->
        val currentColor = if (on) neonColor else neonColor.copy(alpha = 0.25f)
        val textSize = digitWidth * 0.5f

        val glowPaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
            textAlign = android.graphics.Paint.Align.RIGHT
            this.textSize = textSize * 1.2f
            color = currentColor.toArgb()
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            maskFilter = BlurMaskFilter(textSize * 0.5f, BlurMaskFilter.Blur.NORMAL)
        }

        val textPaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
            textAlign = android.graphics.Paint.Align.RIGHT
            this.textSize = textSize
            color = currentColor.toArgb()
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val textBounds = android.graphics.Rect()
        textPaint.getTextBounds(title, 0, title.length, textBounds)
        val yPos = frameLabelY + (textBounds.height() / 2f)

        canvas.nativeCanvas.drawText(title, frameLabelX, yPos, glowPaint)
        canvas.nativeCanvas.drawText(title, frameLabelX, yPos, textPaint)
    }
}

private enum class SegmentLed {
    A, B, C, D, E, F, G
}

private val digitToSegmentLed = mapOf(
    '0' to listOf(SegmentLed.A, SegmentLed.B, SegmentLed.C, SegmentLed.D, SegmentLed.E, SegmentLed.F),
    '1' to listOf(SegmentLed.B, SegmentLed.C),
    '2' to listOf(SegmentLed.A, SegmentLed.B, SegmentLed.G, SegmentLed.E, SegmentLed.D),
    '3' to listOf(SegmentLed.A, SegmentLed.B, SegmentLed.G, SegmentLed.C, SegmentLed.D),
    '4' to listOf(SegmentLed.F, SegmentLed.G, SegmentLed.B, SegmentLed.C),
    '5' to listOf(SegmentLed.A, SegmentLed.F, SegmentLed.G, SegmentLed.C, SegmentLed.D),
    '6' to listOf(SegmentLed.A, SegmentLed.F, SegmentLed.G, SegmentLed.E, SegmentLed.C, SegmentLed.D),
    '7' to listOf(SegmentLed.A, SegmentLed.B, SegmentLed.C),
    '8' to listOf(SegmentLed.A, SegmentLed.B, SegmentLed.C, SegmentLed.D, SegmentLed.E, SegmentLed.F, SegmentLed.G),
    '9' to listOf(SegmentLed.A, SegmentLed.B, SegmentLed.C, SegmentLed.D, SegmentLed.F, SegmentLed.G),
    ' ' to emptyList()
)
