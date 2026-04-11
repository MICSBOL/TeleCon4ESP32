package com.example.emitterapp.ui.rc_screen.components_led_style

import android.graphics.BlurMaskFilter
import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.emitterapp.R
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

@Composable
fun SwitchLedStyleButton(
    modifier: Modifier = Modifier,
    isOn: Boolean,
    onStateChange: (Boolean) -> Unit
) {
    val frameCount = 10

    // Same semantic mapping as Switch3DButton:
    // frame 0 = ON (top), frame 9 = OFF (bottom).
    var frame by remember { mutableStateOf(if (isOn) 0 else frameCount - 1) }
    var isBusy by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val mediaPlayer = remember {
        if (isInPreview) null else MediaPlayer.create(context, R.raw.click_sound)
    }

    DisposableEffect(Unit) {
        onDispose { mediaPlayer?.release() }
    }

    LaunchedEffect(isOn) {
        isBusy = true
        val targetFrame = if (isOn) 0 else frameCount - 1
        if (frame < targetFrame) {
            for (i in frame..targetFrame) {
                frame = i
                delay(5)
            }
        } else {
            for (i in frame downTo targetFrame) {
                frame = i
                delay(5)
            }
        }
        isBusy = false
    }

    val currentIsOn by rememberUpdatedState(isOn)
    val currentIsBusy by rememberUpdatedState(isBusy)
    val currentOnStateChange by rememberUpdatedState(onStateChange)

    Canvas(
        modifier = modifier
            .size(70.dp)
            .rotate(180f)
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    if (currentIsBusy) return@detectDragGestures
                    val verticalDrag = dragAmount.y
                    if (verticalDrag > 3f && currentIsOn) {
                        mediaPlayer?.safeStart()
                        currentOnStateChange(false)
                    } else if (verticalDrag < -3f && !currentIsOn) {
                        mediaPlayer?.safeStart()
                        currentOnStateChange(true)
                    }
                }
            }
    ) {
        val neonMain = Color(0xFF00E5FF)
        val neonAccent = Color(0xFF7C4DFF)
        val background = Color(0xFF050B16)
        val railDim = Color(0xFF1A2440)

        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val centerY = h / 2f

        val bodyRadius = min(w, h) * 0.26f
        val railHalfHeight = h * 0.30f
        val railWidth = w * 0.20f

        val t = frame / (frameCount - 1f)
        val handleY = centerY - railHalfHeight + (2f * railHalfHeight * t)

        // Body backdrop.
        drawCircle(color = background, radius = bodyRadius * 1.4f, center = Offset(centerX, centerY))
        drawGlowCircle(Offset(centerX, centerY), bodyRadius * 1.1f, neonAccent, glow = 18f, coreStroke = 2f)

        // Vertical guide rails.
        val railTop = centerY - railHalfHeight
        val railBottom = centerY + railHalfHeight
        drawGlowLine(
            start = Offset(centerX - railWidth / 2f, railTop),
            end = Offset(centerX - railWidth / 2f, railBottom),
            color = railDim,
            glow = 0f,
            coreStroke = w * 0.055f
        )
        drawGlowLine(
            start = Offset(centerX + railWidth / 2f, railTop),
            end = Offset(centerX + railWidth / 2f, railBottom),
            color = railDim,
            glow = 0f,
            coreStroke = w * 0.055f
        )

        // Active glow band from handle to ON side.
        val activeTop = railTop
        val activeBottom = handleY
        if (activeBottom > activeTop) {
            drawGlowLine(
                start = Offset(centerX, activeTop),
                end = Offset(centerX, activeBottom),
                color = neonMain,
                glow = if (isBusy) 20f else 12f,
                coreStroke = w * 0.05f
            )
        }

        // Handle with brighter glow while animating.
        val handleCenter = Offset(centerX, handleY)
        val handleOuter = max(w, h) * 0.12f
        drawGlowCircle(
            center = handleCenter,
            radius = handleOuter,
            color = if (isOn) neonMain else neonAccent,
            glow = if (isBusy) 26f else 18f,
            coreStroke = 3f
        )
        drawGlowCircle(
            center = handleCenter,
            radius = handleOuter * 0.58f,
            color = Color.White.copy(alpha = 0.9f),
            glow = 8f,
            coreStroke = 1.6f
        )

        // ON/OFF caps.
        drawGlowCircle(Offset(centerX, railTop), w * 0.045f, neonMain.copy(alpha = 0.7f), glow = 10f, coreStroke = 1.8f)
        drawGlowCircle(Offset(centerX, railBottom), w * 0.045f, neonAccent.copy(alpha = 0.7f), glow = 10f, coreStroke = 1.8f)
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

private fun MediaPlayer.safeStart() {
    if (isPlaying) {
        stop()
        prepare()
    }
    start()
}

@Preview(showBackground = true, widthDp = 120, heightDp = 160)
@Composable
private fun SwitchLedStyleButtonPreview() {
    var isOn by remember { mutableStateOf(true) }
    SwitchLedStyleButton(
        isOn = isOn,
        onStateChange = { isOn = it }
    )
}