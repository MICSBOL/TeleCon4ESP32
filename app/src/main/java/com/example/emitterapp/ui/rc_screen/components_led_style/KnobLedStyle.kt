package com.example.emitterapp.ui.rc_screen.components_led_style

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A knob control that mirrors the interaction model of [Knob3D] (drag-based rotation,
 * value in [0, 1]) but is rendered entirely with the neon LED aesthetic of [StickLedStyle].
 *
 * Visual anatomy:
 *  - Dark circular background
 *  - Outer accent ring (purple glow)
 *  - 300° track arc from 7 o'clock → 5 o'clock (dim colour)
 *  - Active arc filled from start up to the current value (cyan glow)
 *  - Radial indicator line + glowing dot marking the current position
 *  - Two concentric body rings and a centre dot
 *  - Small tick marks at minimum, centre, and maximum positions
 */
@Composable
fun KnobLedStyle(
    modifier: Modifier = Modifier,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    val minAngle = -135f
    val maxAngle =  135f

    // Internal drag-tracking angle; kept in sync with external value changes.
    var rotationAngle by remember { mutableStateOf(minAngle + value * (maxAngle - minAngle)) }
    var dragStartAngle by remember { mutableStateOf(0f) }
    var center by remember { mutableStateOf(Offset.Zero) }
    var radius by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Sync angle when value is changed externally (e.g. initial load / programmatic update).
    LaunchedEffect(value) {
        rotationAngle = minAngle + value * (maxAngle - minAngle)
    }

    // Always read the latest callback without restarting the gesture handler.
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    Box(
        modifier = modifier
            .onSizeChanged { size ->
                center = Offset(size.width / 2f, size.height / 2f)
                radius = min(size.width.toFloat(), size.height.toFloat()) / 2f
            }
            .pointerInput(Unit) {          // Unit key → handler is never restarted
                detectDragGestures(
                    onDragStart = { startPos ->
                        isDragging = true
                        val v = startPos - center
                        dragStartAngle = atan2(v.y, v.x) * 180f / Math.PI.toFloat()
                    },
                    onDragEnd    = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, _ ->
                        val v = change.position - center
                        val currentAngle = atan2(v.y, v.x) * 180f / Math.PI.toFloat()

                        var delta = currentAngle - dragStartAngle
                        if (delta >  180f) delta -= 360f
                        else if (delta < -180f) delta += 360f

                        rotationAngle = (rotationAngle + delta).coerceIn(minAngle, maxAngle)
                        val normalized = (rotationAngle - minAngle) / (maxAngle - minAngle)
                        currentOnValueChange(normalized)
                        dragStartAngle = currentAngle
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (radius <= 0f) return@Canvas

            // ── Palette (matches StickLedStyle) ──────────────────────────────────
            val neonMain   = Color(0xFF00E5FF)   // cyan
            val neonAccent = Color(0xFF7C4DFF)   // purple
            val background = Color(0xFF050B16)
            val trackDim   = Color(0xFF1A2440)

            // ── Background ───────────────────────────────────────────────────────
            drawCircle(color = background, radius = radius * 1.05f, center = center)

            // ── Outer ring ───────────────────────────────────────────────────────
            drawGlowCircle(center, radius * 0.97f, neonAccent, glow = 22f, coreStroke = 2f)

            // ── Arc geometry ─────────────────────────────────────────────────────
            //   120° = 7 o'clock in Canvas coordinates (clockwise from 3 o'clock)
            //   300° sweep → ends at 420° ≡ 60° = 5 o'clock
            val arcRadius       = radius * 0.76f
            val trackStart      = 120f
            val trackSweep      = 300f

            // Dim full-range track
            drawArcNeon(
                center      = center,
                arcRadius   = arcRadius,
                startAngle  = trackStart,
                sweepAngle  = trackSweep,
                color       = trackDim,
                strokeWidth = radius * 0.07f,
                glowRadius  = 0f
            )

            // Glowing active portion
            val activeSweep = (value * trackSweep).coerceIn(0f, trackSweep)
            if (activeSweep > 0f) {
                drawArcNeon(
                    center      = center,
                    arcRadius   = arcRadius,
                    startAngle  = trackStart,
                    sweepAngle  = activeSweep,
                    color       = neonMain,
                    strokeWidth = radius * 0.055f,
                    glowRadius  = radius * 0.10f
                )
            }

            // ── Indicator ────────────────────────────────────────────────────────
            val indicatorRad = Math.toRadians((trackStart + value * trackSweep).toDouble()).toFloat()
            val indicatorPos = Offset(
                x = center.x + arcRadius * cos(indicatorRad),
                y = center.y + arcRadius * sin(indicatorRad)
            )

            // Radial line from centre to dot
            drawGlowLine(
                start      = center,
                end        = indicatorPos,
                color      = if (isDragging) neonMain else neonAccent,
                glow       = if (isDragging) 32f else 22f,
                coreStroke = 2.5f
            )

            // ── Knob body rings ───────────────────────────────────────────────────
            drawGlowCircle(center, radius * 0.52f, neonAccent,              glow = 18f, coreStroke = 1.8f)
            drawGlowCircle(center, radius * 0.34f, neonMain.copy(alpha = 0.7f), glow = 12f, coreStroke = 1.4f)

            // ── Centre dot ───────────────────────────────────────────────────────
            drawGlowCircle(center, radius * 0.10f, neonMain, glow = 14f, coreStroke = 3f)

            // ── Indicator dot ─────────────────────────────────────────────────────
            val dotScale = if (isDragging) 1.3f else 1f
            drawGlowCircle(indicatorPos, radius * 0.09f * dotScale, neonMain,               glow = 28f, coreStroke = 3.5f)
            drawGlowCircle(indicatorPos, radius * 0.05f * dotScale, Color.White.copy(0.85f), glow = 10f, coreStroke = 1.5f)

            // ── Tick marks at min / centre / max ──────────────────────────────────
            listOf(trackStart, trackStart + trackSweep / 2f, trackStart + trackSweep).forEach { tickDeg ->
                val tickRad = Math.toRadians(tickDeg.toDouble()).toFloat()
                val inner = Offset(center.x + (arcRadius - radius * 0.11f) * cos(tickRad),
                                   center.y + (arcRadius - radius * 0.11f) * sin(tickRad))
                val outer = Offset(center.x + (arcRadius + radius * 0.11f) * cos(tickRad),
                                   center.y + (arcRadius + radius * 0.11f) * sin(tickRad))
                drawGlowLine(inner, outer, neonAccent.copy(alpha = 0.7f), glow = 8f, coreStroke = 1.8f)
            }
        }
    }
}

// ── Private drawing helpers (identical to those in StickLedStyle) ─────────────

private fun DrawScope.drawArcNeon(
    center: Offset,
    arcRadius: Float,
    startAngle: Float,
    sweepAngle: Float,
    color: Color,
    strokeWidth: Float,
    glowRadius: Float
) {
    drawIntoCanvas { canvas ->
        val l = center.x - arcRadius
        val t = center.y - arcRadius
        val r = center.x + arcRadius
        val b = center.y + arcRadius

        if (glowRadius > 0f) {
            val glow = Paint().asFrameworkPaint().apply {
                isAntiAlias  = true
                style        = android.graphics.Paint.Style.STROKE
                this.color   = color.copy(alpha = 0.45f).toArgb()
                this.strokeWidth = strokeWidth * 2.8f
                strokeCap    = android.graphics.Paint.Cap.ROUND
                maskFilter   = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawArc(l, t, r, b, startAngle, sweepAngle, false, glow)
        }

        val core = Paint().asFrameworkPaint().apply {
            isAntiAlias  = true
            style        = android.graphics.Paint.Style.STROKE
            this.color   = color.toArgb()
            this.strokeWidth = strokeWidth
            strokeCap    = android.graphics.Paint.Cap.ROUND
        }
        canvas.nativeCanvas.drawArc(l, t, r, b, startAngle, sweepAngle, false, core)
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
        val paint = Paint().asFrameworkPaint().apply {
            isAntiAlias  = true
            style        = android.graphics.Paint.Style.STROKE
            strokeCap    = android.graphics.Paint.Cap.ROUND
            this.color   = color.copy(alpha = 0.4f).toArgb()
            strokeWidth  = coreStroke * 3.2f
            maskFilter   = BlurMaskFilter(glow, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.nativeCanvas.drawLine(start.x, start.y, end.x, end.y, paint)

        paint.maskFilter  = null
        paint.color       = color.toArgb()
        paint.strokeWidth = coreStroke
        canvas.nativeCanvas.drawLine(start.x, start.y, end.x, end.y, paint)
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
        val paint = Paint().asFrameworkPaint().apply {
            isAntiAlias  = true
            style        = android.graphics.Paint.Style.STROKE
            this.color   = color.copy(alpha = 0.35f).toArgb()
            strokeWidth  = coreStroke * 3f
            maskFilter   = BlurMaskFilter(glow, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.nativeCanvas.drawCircle(center.x, center.y, radius, paint)

        paint.maskFilter  = null
        paint.color       = color.toArgb()
        paint.strokeWidth = coreStroke
        canvas.nativeCanvas.drawCircle(center.x, center.y, radius, paint)
    }
}

// ── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 260, heightDp = 260)
@Composable
private fun KnobLedStylePreview() {
    var previewValue by remember { mutableStateOf(0.5f) }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        KnobLedStyle(
            modifier = Modifier.size(200.dp),
            value = previewValue,
            onValueChange = { previewValue = it }
        )
    }
}