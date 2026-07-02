package com.micsbol.telecon4esp32.ui.cyber.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.ui.cyber.theme.CyberColors
import com.micsbol.telecon4esp32.ui.cyber.theme.CyberType

/**
 * Primary call-to-action. The highest-priority element: thick chamfered frame,
 * intense animated cyan glow pulse, moving sweep highlight, and press feedback.
 */
@Composable
fun StartControlButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Rocket,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "ctaScale")

    val pulse = rememberInfiniteTransition(label = "ctaPulse")
    val glowPulse by pulse.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ctaGlow",
    )
    val sweep by pulse.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ctaSweep",
    )

    val chamfer = 18.dp
    val shape = remember(chamfer) { ChamferShape(chamfer) }
    val activeGlow = if (pressed) glowPulse * 1.25f else glowPulse

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .scale(scale)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .drawBehind {
                val c = chamfer.toPx()
                val outer = chamferPath(size, c, inset = 2f)
                val inner = chamferPath(size, c * 0.8f, inset = 9.dp.toPx())
                // Intense layered glow.
                drawNeonGlow(outer, CyberColors.NeonPrimary, blurRadius = 26f * activeGlow, strokeWidth = 5f, alpha = 0.7f)
                drawNeonGlow(outer, CyberColors.NeonPrimary, blurRadius = 12f, strokeWidth = 3f, alpha = 0.9f)
                drawPath(outer, color = CyberColors.NeonPrimary, style = Stroke(width = 2.6f))
                drawPath(inner, color = CyberColors.NeonPrimary.copy(alpha = 0.35f), style = Stroke(width = 1.4f))
                // Sweep highlight.
                clipPath(chamferPath(size, c)) {
                    val bandW = size.width * 0.18f
                    val x = sweep * size.width
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, CyberColors.NeonPrimary.copy(alpha = 0.22f), Color.Transparent),
                            startX = x - bandW,
                            endX = x + bandW,
                        ),
                        topLeft = Offset(x - bandW, 0f),
                        size = Size(bandW * 2f, size.height),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0C2A3F), Color(0xFF06121F)),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HexIcon(icon = icon, size = 56.dp)
            Spacer(modifier = Modifier.width(18.dp))
            Column {
                Text(
                    text = title.uppercase(),
                    style = CyberType.Title.copy(letterSpacing = 2.5.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = CyberType.Label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Secondary outlined action button (e.g. CONNECT) with chamfered frame + press scale. */
@Composable
fun NeonActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = CyberColors.NeonPrimary,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "btnScale")
    val chamfer = 8.dp
    val shape = remember(chamfer) { ChamferShape(chamfer) }

    Box(
        modifier = modifier
            .height(40.dp)
            .scale(scale)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .drawBehind {
                val c = chamfer.toPx()
                val outer = chamferPath(size, c, inset = 1.5f)
                drawNeonGlow(outer, accent, blurRadius = if (pressed) 14f else 8f, strokeWidth = 2.5f, alpha = 0.6f)
                drawPath(outer, color = accent, style = Stroke(width = 1.6f))
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(accent.copy(alpha = 0.08f)),
        )
        Text(
            text = text.uppercase(),
            style = CyberType.Label.copy(color = accent),
            modifier = Modifier.padding(horizontal = 18.dp),
        )
    }
}

/** Hexagonal frame holding a glyph; used for the CTA icon and header logo. */
@Composable
fun HexIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = CyberColors.NeonPrimary,
) {
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                val path = hexagonPath(this.size)
                drawNeonGlow(path, tint, blurRadius = 10f, strokeWidth = 2.5f, alpha = 0.5f)
                drawPath(path, color = tint.copy(alpha = 0.12f))
                drawPath(path, color = tint, style = Stroke(width = 1.8f))
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/**
 * Regular flat-top hexagon centred in [size], symmetric on both axes.
 * Sized off the smaller dimension so it always fits and never skews.
 */
/** Hexagonal neon frame holding an image (e.g. the app brand logo). */
@Composable
fun HexImage(
    painter: Painter,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = CyberColors.NeonPrimary,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                val path = hexagonPath(this.size)
                drawNeonGlow(path, tint, blurRadius = 10f, strokeWidth = 2.5f, alpha = 0.5f)
                drawPath(path, color = tint.copy(alpha = 0.10f))
                drawPath(path, color = tint, style = Stroke(width = 1.8f))
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.size(size * 0.56f),
        )
    }
}

private fun hexagonPath(size: Size): Path {
    val inset = 2f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = minOf(size.width, size.height) / 2f - inset
    val halfH = r * 0.8660254f // sqrt(3) / 2
    return Path().apply {
        moveTo(cx + r, cy)
        lineTo(cx + r / 2f, cy + halfH)
        lineTo(cx - r / 2f, cy + halfH)
        lineTo(cx - r, cy)
        lineTo(cx - r / 2f, cy - halfH)
        lineTo(cx + r / 2f, cy - halfH)
        close()
    }
}
