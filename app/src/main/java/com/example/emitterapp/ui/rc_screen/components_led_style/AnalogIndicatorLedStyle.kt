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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emitterapp.ui.rc_screen.components.IndicatorTitle
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnalogIndicatorLedStyle(
    value: Int,
    modifier: Modifier = Modifier,
    title: String = "SPEED",
) {
    val neonMain = Color(0xFF00E5FF)
    val neonAccent = Color(0xFF7C4DFF)
    val background = Color(0xFF050B16)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Box(
            modifier = modifier
                .aspectRatio(969f / 479f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            ) {
                val w = size.width
                val h = size.height

                val pivot = Offset(w * 0.5f, h * 0.96f)
                val outerR   = minOf(w * 0.47f, h * 0.90f)
                val trackR   = outerR * 0.88f
                val innerArcR = outerR * 0.72f
                val hubR     = outerR * 0.085f
                val holeR    = hubR * 0.48f

                val startAngle = -180f
                val sweepAngle = 180f
                val valueFraction = value.coerceIn(0, 100) / 100f
                val valueAngle = startAngle + valueFraction * sweepAngle

                fun arcRect(r: Float) = Pair(
                    Offset(pivot.x - r, pivot.y - r),
                    Size(r * 2f, r * 2f)
                )

                // ── Background filled semicircle ──────────────────────────────────
                val (bgTL, bgSz) = arcRect(outerR)
                drawArc(
                    color = background,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = bgTL,
                    size = bgSz
                )

                // ── Outer border ring (violet glow) ───────────────────────────────
                drawIntoCanvas { c ->
                    val r = outerR
                    val glowP = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = w * 0.025f
                        color = neonAccent.copy(alpha = 0.35f).toArgb()
                        maskFilter = BlurMaskFilter(22f, BlurMaskFilter.Blur.NORMAL)
                    }
                    c.nativeCanvas.drawArc(
                        pivot.x - r, pivot.y - r, pivot.x + r, pivot.y + r,
                        startAngle, sweepAngle, false, glowP
                    )
                    val coreP = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = w * 0.007f
                        color = neonAccent.copy(alpha = 0.9f).toArgb()
                    }
                    c.nativeCanvas.drawArc(
                        pivot.x - r, pivot.y - r, pivot.x + r, pivot.y + r,
                        startAngle, sweepAngle, false, coreP
                    )
                }

                // ── Grey track arc (full range, dim) ──────────────────────────────
                val (trTL, trSz) = arcRect(trackR)
                drawArc(
                    color = neonAccent.copy(alpha = 0.18f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = trTL,
                    size = trSz,
                    style = Stroke(width = w * 0.030f)
                )

                // ── Active progress arc (cyan glow) ───────────────────────────────
                drawIntoCanvas { c ->
                    val r = trackR
                    val glowP = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = w * 0.048f
                        color = neonMain.copy(alpha = 0.28f).toArgb()
                        maskFilter = BlurMaskFilter(26f, BlurMaskFilter.Blur.NORMAL)
                    }
                    c.nativeCanvas.drawArc(
                        pivot.x - r, pivot.y - r, pivot.x + r, pivot.y + r,
                        startAngle, valueFraction * sweepAngle, false, glowP
                    )
                    val coreP = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = w * 0.013f
                        color = neonMain.toArgb()
                    }
                    c.nativeCanvas.drawArc(
                        pivot.x - r, pivot.y - r, pivot.x + r, pivot.y + r,
                        startAngle, valueFraction * sweepAngle, false, coreP
                    )
                }

                // ── Inner decorative arc (violet, subtle) ─────────────────────────
                val (iaTL, iaSz) = arcRect(innerArcR)
                drawArc(
                    color = neonAccent.copy(alpha = 0.30f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = iaTL,
                    size = iaSz,
                    style = Stroke(width = w * 0.005f)
                )

                // ── Tick marks ────────────────────────────────────────────────────
                val totalTicks = 21
                repeat(totalTicks) { i ->
                    val t = i / (totalTicks - 1f)
                    val isMajor = i % 5 == 0
                    val tickAngle = startAngle + t * sweepAngle
                    val rad = Math.toRadians(tickAngle.toDouble())
                    val outer = outerR * if (isMajor) 0.84f else 0.88f
                    val inner = outerR * if (isMajor) 0.72f else 0.79f
                    drawGlowLine(
                        start = Offset(pivot.x + outer * cos(rad).toFloat(), pivot.y + outer * sin(rad).toFloat()),
                        end   = Offset(pivot.x + inner * cos(rad).toFloat(), pivot.y + inner * sin(rad).toFloat()),
                        color = if (isMajor) neonMain else neonAccent.copy(alpha = 0.55f),
                        glow  = if (isMajor) 10f else 5f,
                        coreStroke = if (isMajor) w * 0.006f else w * 0.004f
                    )
                }

                // ── Needle ────────────────────────────────────────────────────────
                val needleRad = Math.toRadians(valueAngle.toDouble())
                val tipX  = pivot.x + (trackR * 0.94f) * cos(needleRad).toFloat()
                val tipY  = pivot.y + (trackR * 0.94f) * sin(needleRad).toFloat()
                val tailX = pivot.x - (hubR * 1.4f) * cos(needleRad).toFloat()
                val tailY = pivot.y - (hubR * 1.4f) * sin(needleRad).toFloat()

                // Needle glow
                drawIntoCanvas { c ->
                    val glowP = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        strokeWidth = w * 0.016f
                        color = neonMain.copy(alpha = 0.45f).toArgb()
                        maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
                    }
                    c.nativeCanvas.drawLine(tailX, tailY, tipX, tipY, glowP)
                }
                // Needle core – tapered: wide at base, thin at tip
                val perpRad = needleRad + Math.PI / 2
                val hw = w * 0.009f
                drawIntoCanvas { c ->
                    val path = android.graphics.Path().apply {
                        moveTo(tailX + hw * cos(perpRad).toFloat(), tailY + hw * sin(perpRad).toFloat())
                        lineTo(tailX - hw * cos(perpRad).toFloat(), tailY - hw * sin(perpRad).toFloat())
                        lineTo(tipX, tipY)
                        close()
                    }
                    val fillP = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        color = neonMain.toArgb()
                    }
                    c.nativeCanvas.drawPath(path, fillP)
                }
                // Bright spine line
                drawGlowLine(
                    start = Offset(tailX, tailY),
                    end   = Offset(tipX, tipY),
                    color = Color.White.copy(alpha = 0.75f),
                    glow  = 5f,
                    coreStroke = w * 0.0028f
                )

                // ── Hub ───────────────────────────────────────────────────────────
                drawGlowCircle(pivot, hubR, neonAccent, glow = 18f, coreStroke = w * 0.006f)
                drawGlowCircle(pivot, hubR * 0.65f, neonMain, glow = 12f, coreStroke = w * 0.004f)

                // Transparent center hole
                drawCircle(
                    color = Color.Transparent,
                    radius = holeR,
                    center = pivot,
                    blendMode = BlendMode.Clear
                )

                // Crisp white ring around the hole
                drawCircle(
                    color = Color.White.copy(alpha = 0.92f),
                    radius = holeR,
                    center = pivot,
                    style = Stroke(width = (w * 0.004f).coerceAtLeast(1.4f))
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        IndicatorTitle(
            text = title,
            textColor = neonMain,
            glowColor = neonMain.copy(alpha = 0.6f),
            textSize = 10.sp,
            showFrame = true
        )
    }
}

private fun DrawScope.drawGlowLine(
    start: Offset,
    end: Offset,
    color: Color,
    glow: Float,
    coreStroke: Float
) {
    drawIntoCanvas { canvas ->
        if (glow > 0f) {
            val glowPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeCap = android.graphics.Paint.Cap.ROUND
                this.color = color.copy(alpha = 0.4f).toArgb()
                strokeWidth = coreStroke * 3.1f
                maskFilter = BlurMaskFilter(glow, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawLine(start.x, start.y, end.x, end.y, glowPaint)
        }

        val corePaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            this.color = color.toArgb()
            strokeWidth = coreStroke
        }
        canvas.nativeCanvas.drawLine(start.x, start.y, end.x, end.y, corePaint)
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
private fun AnalogIndicatorLedStylePreview() {
    AnalogIndicatorLedStyle(
        value = 65,
        title = "SPEED"
    )
}