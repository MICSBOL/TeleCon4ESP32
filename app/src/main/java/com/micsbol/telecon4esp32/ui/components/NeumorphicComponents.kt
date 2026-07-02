package com.micsbol.telecon4esp32.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.micsbol.telecon4esp32.ui.theme.Neo
import android.app.Activity
import android.graphics.BlurMaskFilter
import androidx.core.view.WindowCompat

/**
 * Raised neumorphic surface: a light soft shadow on the top-left and a dark
 * soft shadow on the bottom-right, with the surface fill drawn on top. The
 * shadows are rendered behind the content so callers just place children with
 * padding.
 */
fun Modifier.neuRaised(
    cornerRadius: Dp = 24.dp,
    surface: Color = Neo.Surface,
): Modifier = this.drawBehind { drawNeuRaised(cornerRadius.toPx(), surface) }

/** Draws a raised neumorphic surface (light top-left + dark bottom-right shadows + fill). */
private fun DrawScope.drawNeuRaised(radius: Float, surface: Color) {
    val offset = Neo.ShadowOffset
    val blur = Neo.ShadowBlur
    drawIntoCanvas { canvas ->
        val paint = androidx.compose.ui.graphics.Paint().asFrameworkPaint()
        paint.isAntiAlias = true
        paint.maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
        // Dark shadow, bottom-right
        paint.color = Neo.ShadowDark.toArgb()
        canvas.nativeCanvas.drawRoundRect(
            offset, offset, size.width + offset, size.height + offset, radius, radius, paint,
        )
        // Light shadow, top-left
        paint.color = Neo.ShadowLight.toArgb()
        canvas.nativeCanvas.drawRoundRect(
            -offset, -offset, size.width - offset, size.height - offset, radius, radius, paint,
        )
    }
    drawRoundRect(
        color = surface,
        topLeft = Offset.Zero,
        size = Size(size.width, size.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
    )
}

/**
 * Inset (pressed) neumorphic surface: the surface fill with inner soft shadows
 * so the element looks carved into the background.
 */
fun Modifier.neuInset(
    cornerRadius: Dp = 24.dp,
    surface: Color = Neo.SurfaceLow,
): Modifier = this.drawBehind { drawNeuInset(cornerRadius.toPx(), surface) }

/** Draws an inset (carved-in) neumorphic surface. */
private fun DrawScope.drawNeuInset(radius: Float, surface: Color) {
    val blur = Neo.ShadowBlur
    val offset = Neo.ShadowOffset
    drawRoundRect(
        color = surface,
        topLeft = Offset.Zero,
        size = Size(size.width, size.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
    )
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val save = native.saveLayer(0f, 0f, size.width, size.height, null)
        val clip = androidx.compose.ui.graphics.Paint().asFrameworkPaint()
        clip.isAntiAlias = true
        clip.style = android.graphics.Paint.Style.STROKE
        clip.strokeWidth = blur
        clip.maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
        native.clipRect(0f, 0f, size.width, size.height)
        clip.color = Neo.ShadowDark.toArgb()
        native.drawRoundRect(
            offset, offset, size.width + offset, size.height + offset, radius, radius, clip,
        )
        clip.color = Neo.ShadowLight.toArgb()
        native.drawRoundRect(
            -offset, -offset, size.width - offset, size.height - offset, radius, radius, clip,
        )
        native.restoreToCount(save)
    }
}
@Composable
fun NeumorphicBackground(modifier: Modifier = Modifier) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Neo.Background.toArgb()
                window.navigationBarColor = Neo.Background.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Neo.Background),
    )
}
@Composable
fun NeoSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(start = 4.dp, bottom = 10.dp),
        color = Neo.TextSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
    )
}

@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = 24.dp,
    contentPadding: Dp = 18.dp,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val base = modifier
        .neuRaised(cornerRadius = cornerRadius)
        .let { if (onClick != null) it.clip(RoundedCornerShape(cornerRadius)).clickable(onClick = onClick) else it }
        .padding(contentPadding)
    Column(modifier = base, content = content)
}
/**
 * Draws the glowing accent fill used by selected icon buttons: a soft outer
 * halo plus a radial-gradient face (bright highlight near the top-left fading
 * into the accent/pressed blue).
 */
private fun DrawScope.drawAccentGlow(cornerRadius: Float, pressed: Boolean) {
    val glowColor = if (pressed) Neo.AccentPressed else Neo.Accent
    drawIntoCanvas { canvas ->
        val paint = androidx.compose.ui.graphics.Paint().asFrameworkPaint()
        paint.isAntiAlias = true
        paint.maskFilter = BlurMaskFilter(Neo.ShadowBlur * 1.6f, BlurMaskFilter.Blur.NORMAL)
        paint.color = glowColor.copy(alpha = 0.55f).toArgb()
        canvas.nativeCanvas.drawRoundRect(
            2f, 4f, size.width - 2f, size.height + 6f, cornerRadius, cornerRadius, paint,
        )
    }
    val highlight = if (pressed) Neo.Accent else Neo.AccentHighlight
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(highlight, Neo.Accent, Neo.AccentPressed),
            center = Offset(size.width * 0.35f, size.height * 0.3f),
            radius = size.maxDimension * 0.9f,
        ),
        topLeft = Offset.Zero,
        size = Size(size.width, size.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
    )
}

/** Soft top-left sheen overlay that gives a raised surface a spherical look. */
private fun DrawScope.drawRaisedSheen(cornerRadius: Float) {
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(Neo.ShadowLight.copy(alpha = 0.55f), Color.Transparent),
            center = Offset(size.width * 0.32f, size.height * 0.28f),
            radius = size.maxDimension * 0.75f,
        ),
        topLeft = Offset.Zero,
        size = Size(size.width, size.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
    )
}

@Composable
fun NeoIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    selected: Boolean = false,
    enabled: Boolean = true,
    iconTint: Color = if (selected) Neo.OnAccent else Neo.TextSecondary,
) {
    NeoIconSurface(
        icon = icon,
        onClick = onClick,
        contentDescription = contentDescription,
        modifier = modifier,
        size = size,
        cornerRadius = size / 2,
        clipShape = CircleShape,
        selected = selected,
        enabled = enabled,
        iconTint = iconTint,
    )
}

@Composable
fun NeoSquareIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    cornerRadius: Dp = 18.dp,
    selected: Boolean = false,
    enabled: Boolean = true,
    iconTint: Color = if (selected) Neo.OnAccent else Neo.TextSecondary,
) {
    NeoIconSurface(
        icon = icon,
        onClick = onClick,
        contentDescription = contentDescription,
        modifier = modifier,
        size = size,
        cornerRadius = cornerRadius,
        clipShape = RoundedCornerShape(cornerRadius),
        selected = selected,
        enabled = enabled,
        iconTint = iconTint,
    )
}

@Composable
private fun NeoIconSurface(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier,
    size: Dp,
    cornerRadius: Dp,
    clipShape: androidx.compose.ui.graphics.Shape,
    selected: Boolean,
    enabled: Boolean,
    iconTint: Color,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                val radiusPx = cornerRadius.toPx()
                when {
                    selected -> drawAccentGlow(radiusPx, pressed)
                    pressed -> drawNeuInset(radiusPx, Neo.SurfaceLow)
                    else -> {
                        drawNeuRaised(radiusPx, Neo.Surface)
                        drawRaisedSheen(radiusPx)
                    }
                }
            }
            .clip(clipShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) Neo.OnAccent else iconTint,
            modifier = Modifier.size(size * 0.42f),
        )
    }
}
/** Cushioned 3D accent pill: drop shadow, blue gradient body, top inner highlight. */
private fun DrawScope.drawCushionedAccentPill(cornerRadius: Float, pressed: Boolean) {
    if (!pressed) {
        drawIntoCanvas { canvas ->
            val paint = androidx.compose.ui.graphics.Paint().asFrameworkPaint()
            paint.isAntiAlias = true
            paint.maskFilter = BlurMaskFilter(Neo.ShadowBlur * 1.5f, BlurMaskFilter.Blur.NORMAL)
            paint.color = Neo.ShadowDark.copy(alpha = 0.70f).toArgb()
            canvas.nativeCanvas.drawRoundRect(
                Neo.ShadowOffset,
                Neo.ShadowOffset * 1.4f,
                size.width + Neo.ShadowOffset,
                size.height + Neo.ShadowOffset * 1.4f,
                cornerRadius,
                cornerRadius,
                paint,
            )
        }
        drawIntoCanvas { canvas ->
            val paint = androidx.compose.ui.graphics.Paint().asFrameworkPaint()
            paint.isAntiAlias = true
            paint.maskFilter = BlurMaskFilter(Neo.ShadowBlur * 1.2f, BlurMaskFilter.Blur.NORMAL)
            paint.color = Neo.Accent.copy(alpha = 0.35f).toArgb()
            canvas.nativeCanvas.drawRoundRect(
                2f, 5f, size.width - 2f, size.height + 5f, cornerRadius, cornerRadius, paint,
            )
        }
    }
    val bodyBrush = if (pressed) {
        Brush.verticalGradient(listOf(Neo.AccentPressed, Neo.AccentPressed))
    } else {
        Brush.verticalGradient(
            0f to Neo.AccentHighlight,
            0.42f to Neo.Accent,
            1f to Neo.AccentPressed,
        )
    }
    drawRoundRect(
        brush = bodyBrush,
        topLeft = Offset.Zero,
        size = Size(size.width, size.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
    )
    if (!pressed) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.38f),
                0.38f to Color.White.copy(alpha = 0.10f),
                0.65f to Color.Transparent,
            ),
            topLeft = Offset(3f, 3f),
            size = Size(size.width - 6f, size.height * 0.52f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                (cornerRadius - 3f).coerceAtLeast(0f),
                (cornerRadius - 3f).coerceAtLeast(0f),
            ),
        )
    }
}

@Composable
fun NeoPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    compact: Boolean = false,
    fillMaxWidth: Boolean = false,
) {
    val cornerRadius = Neo.ButtonCornerRadius
    val shape = Neo.PillShape
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val contentColor = if (enabled) Neo.OnAccent else Neo.TextMuted
    val height = if (compact) 44.dp else 56.dp
    val horizontalPadding = if (compact) 18.dp else 24.dp
    Row(
        modifier = modifier
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
            .height(height)
            .drawBehind {
                val r = cornerRadius.toPx()
                if (enabled) {
                    drawCushionedAccentPill(r, pressed)
                } else {
                    drawNeuRaised(r, Neo.SurfaceLow)
                }
            }
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(if (compact) 18.dp else 20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            color = contentColor,
            fontSize = if (compact) 14.sp else 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
@Composable
fun NeoSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    fillMaxWidth: Boolean = false,
    compact: Boolean = false,
) {
    val cornerRadius = Neo.ButtonCornerRadius
    val shape = Neo.PillShape
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val height = if (compact) 44.dp else 56.dp
    val horizontalPadding = if (compact) 18.dp else 24.dp
    Row(
        modifier = modifier
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
            .height(height)
            .then(
                if (pressed) Modifier.neuInset(cornerRadius = cornerRadius)
                else Modifier.neuRaised(cornerRadius = cornerRadius)
            )
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Neo.TextPrimary,
                modifier = Modifier.size(if (compact) 18.dp else 20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            color = if (enabled) Neo.TextPrimary else Neo.TextMuted,
            fontSize = if (compact) 14.sp else 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
@Composable
fun NeoToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackWidth = 56.dp
    val trackHeight = 32.dp
    Box(
        modifier = modifier
            .size(trackWidth, trackHeight)
            .neuInset(cornerRadius = trackHeight / 2, surface = if (checked) Neo.Accent else Neo.SurfaceLow)
            .clip(RoundedCornerShape(trackHeight / 2))
            .clickable { onCheckedChange(!checked) }
            .padding(4.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .neuRaised(cornerRadius = 12.dp, surface = if (checked) Neo.OnAccent else Neo.TextSecondary),
        )
    }
}
@Composable
fun NeoStatTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Neo.TextPrimary,
) {
    Column(
        modifier = modifier
            .neuRaised(cornerRadius = 20.dp)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        NeoIconBadge(icon = icon)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Neo.TextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Rounded-square icon container. Raised (popped-out) neumorphic surface with a
 * light icon by default; when [selected] it fills with the glowing accent blue
 * and the icon turns white -- matching the room-selector tiles in the design.
 */
@Composable
fun NeoIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    tint: Color = Neo.TextPrimary,
    selected: Boolean = false,
) {
    val cornerRadius = size * 0.3f
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                val radiusPx = cornerRadius.toPx()
                if (selected) drawAccentGlow(radiusPx, pressed = false)
                else drawNeuRaised(radiusPx, Neo.Surface)
            }
            .clip(RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Neo.OnAccent else tint,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}
@Composable
fun NeoDialControl(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 180.dp,
    accent: Color = Neo.Accent,
    centerContent: @Composable () -> Unit,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .neuInset(cornerRadius = diameter / 2)
                .drawBehind {
                    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 14.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                    val inset = 18.dp.toPx()
                    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                    val topLeft = Offset(inset, inset)
                    drawArc(
                        color = Neo.ShadowDark,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke,
                    )
                    drawArc(
                        color = accent,
                        startAngle = 135f,
                        sweepAngle = 270f * clamped,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke,
                    )
                },
        )
        centerContent()
    }
}
@Composable
fun NeoTopBar(
    title: String,
    subtitle: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onNavigateBack != null) {
            NeoIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onNavigateBack,
                contentDescription = null,
                size = 44.dp,
            )
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Neo.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = Neo.TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        actions()
    }
}
/**
 * Neumorphic screen shell: charcoal background, top bar, and a content column.
 * The [content] receives top padding already applied for the top bar; it should
 * add its own horizontal padding and scrolling as needed.
 */
@Composable
fun NeoScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        NeumorphicBackground()
        Column(modifier = Modifier.fillMaxSize()) {
            NeoTopBar(
                title = title,
                subtitle = subtitle,
                onNavigateBack = onNavigateBack,
                actions = actions,
            )
            Box(modifier = Modifier.weight(1f)) {
                content(PaddingValues(horizontal = 18.dp))
            }
        }
    }
}

/** Floating neumorphic dialog shell for help, codes export, and similar overlays. */
@Composable
fun NeoDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalMargin: Dp = 16.dp,
    title: (@Composable () -> Unit)? = null,
    subtitle: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
    actions: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalMargin)
                .neuRaised(cornerRadius = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            title?.invoke()
            subtitle?.invoke()
            content()
            Spacer(modifier = Modifier.height(4.dp))
            actions()
        }
    }
}

@Composable
fun NeoDialogTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Neo.TextPrimary,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        color = color,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun NeoDialogBody(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        color = Neo.TextSecondary,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
}

@Composable
fun NeoDialogTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        color = Neo.TextSecondary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
    )
}

