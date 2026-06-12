package com.micsbol.emitterapp.ui.control_panel.components

import android.annotation.SuppressLint
import android.graphics.BlurMaskFilter
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.micsbol.emitterapp.R

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun SevenSegmentedPanel(
    value: Float,
    on: Boolean,
    onColor: Color,
    modifier: Modifier = Modifier,
    width: Dp = 400.dp,
    title: String = "",
) {
    val offColor = onColor.copy(alpha = 0.1f)

    val clampedValue = value.coerceIn(0f, 9999.9f)
    val formatted = String.format(java.util.Locale.US, "%06.1f", clampedValue)
    val digits = formatted.replace(".", "")
    val autoDecimalPoints = listOf(false, false, false, true, false)

    BoxWithConstraints(
        modifier = modifier.size(height = width * 0.66f, width = width),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = modifier
                .width(maxWidth * 0.9f)
                .height(maxHeight * 0.7f)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray)
            ) {
                val numDigits = 5
                val digitWidth = size.width * 0.13f
                val digitHeight = digitWidth * 2f
                val digitSpacing = size.width * 0.03f
                val totalWidth = (numDigits * digitWidth) + ((numDigits - 1) * digitSpacing)
                val startX = (size.width - totalWidth) / 2
                val startY = (size.height - digitHeight) / 3

                digits.take(numDigits).forEachIndexed { index, digitChar ->
                    val digitTopLeft = Offset(
                        x = startX + index * (digitWidth + digitSpacing),
                        y = startY
                    )

                    val isLeadingZero = index < 3 && 
                                        digitChar == '0' && 
                                        digits.substring(0, index + 1).all { it == '0' }

                    drawDigit(
                        digit = digitChar,
                        topLeft = digitTopLeft,
                        width = digitWidth,
                        height = digitHeight,
                        on = on && !isLeadingZero,
                        onColor = onColor,
                        offColor = offColor,
                        blurRadius = 20f
                    )
                }

                val pointRadius = digitWidth / 14f
                val pointY = startY + digitHeight - pointRadius
                val glowPointPaint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.FILL
                    this.color = onColor.toArgb()
                    maskFilter = BlurMaskFilter(pointRadius * 2, BlurMaskFilter.Blur.NORMAL)
                }

                autoDecimalPoints.forEachIndexed { index, isPointOn ->
                    if (isPointOn && on) {
                        val pointX = startX + (index + 1) * digitWidth + (index * digitSpacing) + (digitSpacing / 2)
                        val pointCenter = Offset(pointX, pointY)

                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawCircle(pointCenter.x, pointCenter.y, pointRadius, glowPointPaint)
                        }
                        drawCircle(color = onColor, radius = pointRadius, center = pointCenter)
                    }
                }
            }
            UnitDisplay(
                title = title,
                on = true,
                onColor = onColor,
                width = width,
            )
        }
        Image(
            painter = painterResource(id = R.drawable.panel_background_extended),
            contentDescription = "Panel Background",
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun DrawScope.drawDigit(
    digit: Char,
    topLeft: Offset,
    width: Float,
    height: Float,
    on: Boolean,
    onColor: Color,
    offColor: Color,
    blurRadius: Float
) {
    val segmentsToDraw = digitToSegment[digit] ?: emptyList()
    val strokeWidth = width / 7f

    val glowPaint = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeJoin = android.graphics.Paint.Join.ROUND
        strokeCap = android.graphics.Paint.Cap.ROUND
        this.strokeWidth = strokeWidth
        this.color = onColor.toArgb()
        maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
    }

    val segmentPaths = getSegmentPaths(topLeft, width, height, strokeWidth)
    segmentPaths.values.forEach { path ->
        drawPath(path = path, color = offColor, style = Stroke(width = strokeWidth))
    }

    if (on) {
        segmentsToDraw.forEach { segment ->
            val path = segmentPaths[segment] ?: return@forEach

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawPath(path.asAndroidPath(), glowPaint)
            }
            drawPath(path = path, color = onColor, style = Stroke(width = strokeWidth))
        }
    }
}

private fun getSegmentPaths(
    topLeft: Offset,
    width: Float,
    height: Float,
    stroke: Float
): Map<Segment, Path> {
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
        Segment.A to pathA, Segment.B to pathB, Segment.C to pathC,
        Segment.D to pathD, Segment.E to pathE, Segment.F to pathF, Segment.G to pathG
    )
}

@Preview(showBackground = true)
@Composable
private fun SevenSegmentedPanelPreview() {
    Column {
        SevenSegmentedPanel(
            title = "RPM",
            value = 1234.5f,
            on = true,
            onColor = Color.Cyan
        )
        SevenSegmentedPanel(
            title = "VOLTS",
            value = 12.3f,
            on = true,
            onColor = Color.Green
        )
    }
}

private enum class Segment {
    A, B, C, D, E, F, G
}

private val digitToSegment = mapOf(
    '0' to listOf(Segment.A, Segment.B, Segment.C, Segment.D, Segment.E, Segment.F),
    '1' to listOf(Segment.B, Segment.C),
    '2' to listOf(Segment.A, Segment.B, Segment.G, Segment.E, Segment.D),
    '3' to listOf(Segment.A, Segment.B, Segment.G, Segment.C, Segment.D),
    '4' to listOf(Segment.F, Segment.G, Segment.B, Segment.C),
    '5' to listOf(Segment.A, Segment.F, Segment.G, Segment.C, Segment.D),
    '6' to listOf(Segment.A, Segment.F, Segment.G, Segment.E, Segment.C, Segment.D),
    '7' to listOf(Segment.A, Segment.B, Segment.C),
    '8' to listOf(Segment.A, Segment.B, Segment.C, Segment.D, Segment.E, Segment.F, Segment.G),
    '9' to listOf(Segment.A, Segment.B, Segment.C, Segment.D, Segment.F, Segment.G),
    ' ' to emptyList()
)

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun UnitDisplay(
    title: String,
    on: Boolean,
    onColor: Color,
    modifier: Modifier = Modifier,
    width: Dp = 150.dp,
){
    val offColor = onColor.copy(alpha = 0.1f)
    val textToDraw = title

    BoxWithConstraints(
        modifier = modifier.size(width, width * 0.66f)
    ) {
        Box(
            modifier = Modifier
                .width(maxWidth * 1f)
                .height(maxHeight * 0.3f)
                .background(Color.Transparent)
                .align(Alignment.BottomEnd)
                .padding(end = maxWidth * 0.1f, bottom = maxHeight * 0.05f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val currentColor = if (on) onColor else offColor

                val glowPaint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.FILL
                    textAlign = android.graphics.Paint.Align.LEFT
                    textSize = size.height * 0.6f
                    color = currentColor.toArgb()
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    maskFilter = BlurMaskFilter(textSize * 0.4f, BlurMaskFilter.Blur.NORMAL)
                }

                val textPaint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.FILL
                    textAlign = android.graphics.Paint.Align.LEFT
                    textSize = size.height * 0.5f
                    color = currentColor.toArgb()
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                }

                val textBounds = android.graphics.Rect()
                textPaint.getTextBounds(textToDraw, 0, textToDraw.length, textBounds)
                val xPos = size.width - (textBounds.width() * 1.2f)
                val yPos = (size.height / 2f) + (textBounds.height() / 2f)

                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(textToDraw, xPos, yPos, glowPaint)
                    canvas.nativeCanvas.drawText(textToDraw, xPos, yPos, textPaint)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UnitDisplayPreview() {
    UnitDisplay(
        title  = "RPMs",
        on = true,
        onColor = Color.Green
    )
}
