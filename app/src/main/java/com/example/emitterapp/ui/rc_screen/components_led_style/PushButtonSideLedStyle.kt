package com.example.emitterapp.ui.rc_screen.components_led_style

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.emitterapp.ui.rc_screen.components.ButtonSide
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@Composable
fun PushButtonSideLedStyle(
    modifier: Modifier,
    side: ButtonSide = ButtonSide.LEFT,
    onPress: () -> Unit
) {
    val frameCount = 20
    var frame by remember { mutableIntStateOf(0) }

    val coroutineScope = rememberCoroutineScope()
    var animationJob by remember { mutableStateOf<Job?>(null) }

    Canvas(
        modifier = modifier
            .pointerInput(side) {
                detectTapGestures(
                    onPress = {
                        onPress()
                        animationJob?.cancel()
                        animationJob = coroutineScope.launch {
                            for (i in 0 until frameCount) {
                                frame = i
                                delay(5)
                            }
                        }

                        val released = tryAwaitRelease()
                        if (released) {
                            animationJob?.cancel()
                            animationJob = coroutineScope.launch {
                                for (i in (frameCount - 1) downTo 0) {
                                    frame = i
                                    delay(5)
                                }
                            }
                        }
                    }
                )
            }
    ) {
        val neonMain = Color(0xFF00E5FF)
        val neonAccent = Color(0xFF7C4DFF)
        val background = Color(0xFF050B16)
        val railDim = Color(0xFF1A2440)

        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val sideDir = if (side == ButtonSide.LEFT) 1f else -1f
        val pressed = frame / (frameCount - 1f)

        // Pressed state translates the front face vertically to simulate button travel.
        val verticalOffset = h * 0.11f * pressed
        val baseAnchorXOffset = w * 0.07f
        // Move the whole animated group left only for RIGHT-side buttons.
        val rightSideLeftShift = if (side == ButtonSide.RIGHT) -w * 0.14f else 0f
        val anchorXOffset = baseAnchorXOffset + rightSideLeftShift
        val anchorYOffset = -h * 0.1f

        val panelInset = minOf(w, h) * 0.07f
        val panelCorner = minOf(w, h) * 0.10f
        val panelWidth = w - panelInset * 2f
        val panelBaseHeight = h - panelInset * 2f
        val panelHeight = panelBaseHeight * 0.7f
        val panelTop = (h - panelHeight) / 2f
        drawRoundRect(
            color = background,
            topLeft = Offset(panelInset, panelTop),
            size = Size(panelWidth, panelHeight),
            cornerRadius = CornerRadius(panelCorner, panelCorner)
        )
        drawRoundRect(
            color = neonAccent.copy(alpha = 0.55f),
            topLeft = Offset(panelInset, panelTop),
            size = Size(panelWidth, panelHeight),
            cornerRadius = CornerRadius(panelCorner, panelCorner),
            style = Stroke(width = w * 0.012f)
        )

        val center = Offset(cx + anchorXOffset, cy + anchorYOffset + verticalOffset)
        val topW = w * 0.70f
        val topH = h * 0.22f
        // Slightly flatter perspective so line inclination is softer.
        val skewX = sideDir * (w * 0.025f)
        val depth = Offset(-sideDir * (w * 0.11f), h * (0.16f - 0.05f * pressed))

        val p1 = Offset(center.x - topW / 2f + skewX, center.y - topH / 2f)
        val p2 = Offset(center.x + topW / 2f + skewX, center.y - topH / 2f)
        val p3 = Offset(center.x + topW / 2f - skewX, center.y + topH / 2f)
        val p4 = Offset(center.x - topW / 2f - skewX, center.y + topH / 2f)

        val b1 = p1 + depth
        val b2 = p2 + depth
        val b3 = p3 + depth
        val b4 = p4 + depth

        fun along(a: Offset, b: Offset, distance: Float): Offset {
            val dx = b.x - a.x
            val dy = b.y - a.y
            val len = sqrt(dx * dx + dy * dy)
            if (len <= 0.001f) return a
            val t = (distance / len).coerceIn(0f, 0.48f)
            return Offset(a.x + dx * t, a.y + dy * t)
        }

        val frontCornerRound = minOf(topW, topH) * 0.23f
        val backCornerRound = frontCornerRound * 0.85f

        // Front rounded edge endpoints.
        val p12s = along(p1, p2, frontCornerRound)
        val p12e = along(p2, p1, frontCornerRound)
        val p23s = along(p2, p3, frontCornerRound)
        val p23e = along(p3, p2, frontCornerRound)
        val p34s = along(p3, p4, frontCornerRound)
        val p34e = along(p4, p3, frontCornerRound)
        val p41s = along(p4, p1, frontCornerRound)
        val p41e = along(p1, p4, frontCornerRound)

        // Back rounded edge endpoints.
        val b12s = along(b1, b2, backCornerRound)
        val b12e = along(b2, b1, backCornerRound)
        val b23s = along(b2, b3, backCornerRound)
        val b23e = along(b3, b2, backCornerRound)
        val b34s = along(b3, b4, backCornerRound)
        val b34e = along(b4, b3, backCornerRound)
        val b41s = along(b4, b1, backCornerRound)
        val b41e = along(b1, b4, backCornerRound)

        // Back wireframe (shadow hull).
        drawGlowLine(b12s, b12e, railDim, glow = 0f, coreStroke = w * 0.020f)
        drawGlowLine(b23s, b23e, railDim, glow = 0f, coreStroke = w * 0.020f)
        drawGlowLine(b34s, b34e, railDim, glow = 0f, coreStroke = w * 0.020f)
        drawGlowLine(b41s, b41e, railDim, glow = 0f, coreStroke = w * 0.020f)

        // Vertical connectors for extrusion.
        drawGlowLine(p1, b1, neonAccent.copy(alpha = 0.75f), glow = 8f, coreStroke = w * 0.018f)
        drawGlowLine(p2, b2, neonAccent.copy(alpha = 0.75f), glow = 8f, coreStroke = w * 0.018f)
        drawGlowLine(p3, b3, neonAccent.copy(alpha = 0.70f), glow = 8f, coreStroke = w * 0.018f)
        drawGlowLine(p4, b4, neonAccent.copy(alpha = 0.70f), glow = 8f, coreStroke = w * 0.018f)

        // Front/top face wireframe.
        drawGlowLine(p12s, p12e, neonMain, glow = 20f, coreStroke = w * 0.026f)
        drawGlowLine(p23s, p23e, neonAccent, glow = 14f, coreStroke = w * 0.022f)
        drawGlowLine(p34s, p34e, neonMain.copy(alpha = 0.95f), glow = 20f, coreStroke = w * 0.026f)
        drawGlowLine(p41s, p41e, neonAccent, glow = 14f, coreStroke = w * 0.022f)

        // Small corner nodes soften hard wireframe corners.
        val cornerNodeRadius = minOf(w, h) * 0.019f
        drawGlowCircle(p1, cornerNodeRadius, neonMain, glow = 10f, coreStroke = 1f)
        drawGlowCircle(p2, cornerNodeRadius, neonAccent, glow = 10f, coreStroke = 1f)
        drawGlowCircle(p3, cornerNodeRadius, neonMain, glow = 10f, coreStroke = 1f)
        drawGlowCircle(p4, cornerNodeRadius, neonAccent, glow = 10f, coreStroke = 1f)

        val innerScale = 0.66f
        fun lerp(a: Offset, b: Offset, t: Float): Offset = Offset(
            x = a.x + (b.x - a.x) * t,
            y = a.y + (b.y - a.y) * t
        )
        val i1 = lerp(center, p1, innerScale)
        val i2 = lerp(center, p2, innerScale)
        val i3 = lerp(center, p3, innerScale)
        val i4 = lerp(center, p4, innerScale)

        val innerCornerRound = frontCornerRound * 0.55f
        val i12s = along(i1, i2, innerCornerRound)
        val i12e = along(i2, i1, innerCornerRound)
        val i23s = along(i2, i3, innerCornerRound)
        val i23e = along(i3, i2, innerCornerRound)
        val i34s = along(i3, i4, innerCornerRound)
        val i34e = along(i4, i3, innerCornerRound)
        val i41s = along(i4, i1, innerCornerRound)
        val i41e = along(i1, i4, innerCornerRound)

        drawGlowLine(i12s, i12e, neonAccent.copy(alpha = 0.8f), glow = 10f, coreStroke = w * 0.016f)
        drawGlowLine(i23s, i23e, neonMain.copy(alpha = 0.8f), glow = 10f, coreStroke = w * 0.016f)
        drawGlowLine(i34s, i34e, neonAccent.copy(alpha = 0.8f), glow = 10f, coreStroke = w * 0.016f)
        drawGlowLine(i41s, i41e, neonMain.copy(alpha = 0.8f), glow = 10f, coreStroke = w * 0.016f)

        // Center light becomes tighter/brighter as button is pressed.
        drawGlowCircle(
            center = center,
            radius = minOf(w, h) * (0.065f - 0.015f * pressed),
            color = neonMain,
            glow = 18f + 10f * pressed,
            coreStroke = 2f
        )
        drawGlowCircle(
            center = center,
            radius = minOf(w, h) * 0.030f,
            color = Color.White.copy(alpha = 0.85f),
            glow = 7f,
            coreStroke = 1.2f
        )

        // Decorative side node on the outer edge.
        val node = Offset(center.x + sideDir * (w * 0.38f), center.y - h * 0.02f)
        drawGlowCircle(node, minOf(w, h) * 0.048f, neonAccent, glow = 12f, coreStroke = 1.6f)
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

@Preview(showBackground = true, widthDp = 200, heightDp = 140)
@Composable
private fun PushButtonSideLedStyleLeftPreview() {
    PushButtonSideLedStyle(
        modifier = Modifier.size(144.dp, 94.dp),
        side = ButtonSide.LEFT,
        onPress = {}
    )
}

@Preview(showBackground = true, widthDp = 200, heightDp = 140)
@Composable
private fun PushButtonSideLedStyleRightPreview() {
    PushButtonSideLedStyle(
        modifier = Modifier.size(144.dp, 94.dp),
        side = ButtonSide.RIGHT,
        onPress = {}
    )
}