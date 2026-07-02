package com.micsbol.telecon4esp32.ui.cyber.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.cyber.theme.CyberColors
import kotlin.math.min

/**
 * Builds a polygonal, chamfered (cut-corner) frame path. This is the mechanical,
 * non-rounded silhouette used by every panel in the dashboard.
 */
fun chamferPath(size: Size, chamfer: Float, inset: Float = 0f): Path {
    val l = inset
    val t = inset
    val r = size.width - inset
    val b = size.height - inset
    val c = min(chamfer, min(r - l, b - t) / 2f)
    return Path().apply {
        moveTo(l + c, t)
        lineTo(r - c, t)
        lineTo(r, t + c)
        lineTo(r, b - c)
        lineTo(r - c, b)
        lineTo(l + c, b)
        lineTo(l, b - c)
        lineTo(l, t + c)
        close()
    }
}

/** [Shape] variant of [chamferPath] used for clipping fills and content. */
class ChamferShape(private val chamfer: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val c = with(density) { chamfer.toPx() }
        return Outline.Generic(chamferPath(size, c))
    }
}

/**
 * Strokes [path] with a soft neon glow using a blur mask filter drawn straight
 * into the native canvas. Used for all panel borders and the CTA halo.
 */
fun DrawScope.drawNeonGlow(
    path: Path,
    color: Color,
    blurRadius: Float,
    strokeWidth: Float,
    alpha: Float = 1f,
) {
    if (blurRadius <= 0f) return
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            this.color = color.copy(alpha = color.alpha * alpha)
            style = PaintingStyle.Stroke
            this.strokeWidth = strokeWidth
            asFrameworkPaint().maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, paint)
    }
}

/**
 * Reusable industrial frame panel.
 *
 * - chamfered (cut) corners, never rounded
 * - dark blue vertical gradient fill
 * - neon outer glow + crisp stroke + dim inner border (soft inner light)
 * - optional animated diagonal sweep highlight
 */
@Composable
fun CyberPanel(
    modifier: Modifier = Modifier,
    chamfer: Dp = 14.dp,
    glowColor: Color = CyberColors.NeonPrimary,
    strokeColor: Color = CyberColors.NeonSecondary,
    fillTop: Color = CyberColors.PanelFillTop,
    fillBottom: Color = CyberColors.PanelFillBottom,
    glowIntensity: Float = 1f,
    sweep: Boolean = false,
    contentPadding: Dp = 18.dp,
    cornerTicks: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = remember(chamfer) { ChamferShape(chamfer) }

    val sweepProgress by if (sweep) {
        val transition = rememberInfiniteTransition(label = "panelSweep")
        transition.animateFloat(
            initialValue = -0.3f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(3200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "sweepX",
        )
    } else {
        remember { androidx.compose.runtime.mutableStateOf(0f) }
    }

    Box(
        modifier = modifier.drawBehind {
            val c = chamfer.toPx()
            val outer = chamferPath(size, c, inset = 1.5f)
            val inner = chamferPath(size, c * 0.78f, inset = 6.dp.toPx())

            // Outer neon glow halo.
            drawNeonGlow(outer, glowColor, blurRadius = 14f * glowIntensity, strokeWidth = 3f, alpha = 0.55f * glowIntensity)
            // Crisp main stroke.
            drawPath(outer, color = strokeColor, style = Stroke(width = 1.6f))
            // Soft inner cyan edge.
            drawPath(inner, color = CyberColors.InnerEdge, style = Stroke(width = 1.2f))

            if (cornerTicks) drawCornerTicks(c, glowColor)

            if (sweep) {
                clipPath(chamferPath(size, c)) {
                    val bandW = size.width * 0.22f
                    val x = sweepProgress * size.width
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                glowColor.copy(alpha = 0.12f),
                                Color.Transparent,
                            ),
                            startX = x - bandW,
                            endX = x + bandW,
                        ),
                        topLeft = Offset(x - bandW, 0f),
                        size = Size(bandW * 2f, size.height),
                    )
                }
            }
        },
    ) {
        // Fill layer, clipped to the chamfered silhouette.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(Brush.verticalGradient(listOf(fillTop, fillBottom))),
        )
        Box(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** Small mechanical accent ticks just inside each chamfered corner. */
private fun DrawScope.drawCornerTicks(chamfer: Float, color: Color) {
    val len = chamfer * 0.5f
    val sw = 1.4f
    val tint = color.copy(alpha = 0.85f)
    val w = size.width
    val h = size.height
    // top-left
    drawLine(tint, Offset(2f, chamfer + 2f), Offset(2f, chamfer + 2f + len), sw)
    drawLine(tint, Offset(chamfer + 2f, 2f), Offset(chamfer + 2f + len, 2f), sw)
    // top-right
    drawLine(tint, Offset(w - 2f, chamfer + 2f), Offset(w - 2f, chamfer + 2f + len), sw)
    drawLine(tint, Offset(w - chamfer - 2f, 2f), Offset(w - chamfer - 2f - len, 2f), sw)
    // bottom-left
    drawLine(tint, Offset(2f, h - chamfer - 2f), Offset(2f, h - chamfer - 2f - len), sw)
    drawLine(tint, Offset(chamfer + 2f, h - 2f), Offset(chamfer + 2f + len, h - 2f), sw)
    // bottom-right
    drawLine(tint, Offset(w - 2f, h - chamfer - 2f), Offset(w - 2f, h - chamfer - 2f - len), sw)
    drawLine(tint, Offset(w - chamfer - 2f, h - 2f), Offset(w - chamfer - 2f - len, h - 2f), sw)
}

/**
 * Layered screen background:
 * 1. solid near-black base
 * 2. radial vignette (brighter centre, dark edges)
 * 3. faint metallic noise speckle drawn on a Compose canvas
 */
@Composable
fun CyberBackground(modifier: Modifier = Modifier) {
    val noise = remember { generateNoise(seed = 42, count = 1400) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberColors.BackgroundDeep)
            .drawBehind {
                // Radial vignette.
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0B1726).copy(alpha = 0.9f),
                            CyberColors.BackgroundDeep,
                            CyberColors.Background,
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.32f),
                        radius = size.maxDimension * 0.85f,
                    ),
                )
                // Metallic noise speckle (~6% opacity).
                noise.forEach { (fx, fy, a) ->
                    drawCircle(
                        color = CyberColors.NeonPrimary.copy(alpha = a * 0.06f),
                        radius = 0.7f,
                        center = Offset(fx * size.width, fy * size.height),
                    )
                }
            },
    )
}

private fun generateNoise(seed: Int, count: Int): List<Triple<Float, Float, Float>> {
    val rng = java.util.Random(seed.toLong())
    return List(count) {
        Triple(rng.nextFloat(), rng.nextFloat(), 0.3f + rng.nextFloat() * 0.7f)
    }
}
