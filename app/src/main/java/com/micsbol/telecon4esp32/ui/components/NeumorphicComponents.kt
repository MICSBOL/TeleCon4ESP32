package com.micsbol.telecon4esp32.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.micsbol.telecon4esp32.ui.theme.AppGlass
import com.micsbol.telecon4esp32.ui.theme.AppGlassBackground
import com.micsbol.telecon4esp32.ui.theme.Neo

/** Frosted glass card surface with a thin light border. */
fun Modifier.glassSurface(
    cornerRadius: Dp = 24.dp,
    alpha: Float = AppGlass.SurfaceAlpha,
    borderAlpha: Float = AppGlass.BorderAlpha,
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .clip(shape)
        .background(AppGlass.CardSurface.copy(alpha = alpha))
        .border(1.dp, AppGlass.BorderColor.copy(alpha = borderAlpha), shape)
}

/**
 * Raised glass surface (replaces legacy neumorphic raised modifier).
 */
fun Modifier.neuRaised(
    cornerRadius: Dp = 24.dp,
    surface: Color = Neo.Surface,
): Modifier = glassSurface(
    cornerRadius = cornerRadius,
    alpha = surface.alpha.takeIf { it > 0f } ?: AppGlass.SurfaceAlpha,
)

/** Inset glass surface — slightly darker / lower alpha for pressed or recessed areas. */
fun Modifier.neuInset(
    cornerRadius: Dp = 24.dp,
    surface: Color = Neo.SurfaceLow,
): Modifier = glassSurface(
    cornerRadius = cornerRadius,
    alpha = surface.alpha.takeIf { it > 0f } ?: AppGlass.SurfaceAlpha * 0.65f,
    borderAlpha = AppGlass.BorderAlpha * 0.7f,
)

@Composable
fun NeumorphicBackground(modifier: Modifier = Modifier) {
    AppGlassBackground(modifier = modifier)
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
    val shape = RoundedCornerShape(cornerRadius)
    val base = modifier
        .glassSurface(cornerRadius = cornerRadius)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(contentPadding)
    Column(modifier = base, content = content)
}

/** Soft orange glow behind selected icon buttons (no BlurMaskFilter — safer on emulators). */
private fun DrawScope.drawAccentGlow(cornerRadius: Float, pressed: Boolean) {
    val glowColor = if (pressed) Neo.AccentPressed else Neo.Accent
    drawRoundRect(
        color = glowColor.copy(alpha = 0.35f),
        topLeft = Offset(1f, 3f),
        size = Size(size.width - 2f, size.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
    )
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(Neo.AccentHighlight, Neo.Accent, Neo.AccentPressed),
            center = Offset(size.width * 0.35f, size.height * 0.3f),
            radius = size.maxDimension * 0.9f,
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
    val shape = clipShape
    Box(
        modifier = modifier
            .size(size)
            .then(
                when {
                    selected -> Modifier.drawBehind { drawAccentGlow(cornerRadius.toPx(), pressed) }
                    pressed -> Modifier.glassSurface(cornerRadius, alpha = AppGlass.SurfaceAlpha * 0.65f)
                    else -> Modifier.glassSurface(cornerRadius, alpha = AppGlass.TopBarButtonAlpha)
                },
            )
            .clip(shape)
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
/** Orange gradient pill body with a soft outer halo (no BlurMaskFilter — safer on emulators). */
private fun DrawScope.drawAccentPill(cornerRadius: Float, pressed: Boolean) {
    if (!pressed) {
        drawRoundRect(
            color = Neo.Accent.copy(alpha = 0.28f),
            topLeft = Offset(1f, 3f),
            size = Size(size.width - 2f, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
        )
    }
    val bodyBrush = if (pressed) {
        Brush.verticalGradient(listOf(Neo.AccentPressed, Neo.AccentPressed))
    } else {
        AppGlass.accentVerticalGradient
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
                0f to Color.White.copy(alpha = 0.28f),
                0.4f to Color.White.copy(alpha = 0.08f),
                0.65f to Color.Transparent,
            ),
            topLeft = Offset(2f, 2f),
            size = Size(size.width - 4f, size.height * 0.5f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                (cornerRadius - 2f).coerceAtLeast(0f),
                (cornerRadius - 2f).coerceAtLeast(0f),
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
    iconPainter: Painter? = null,
    trailingPainter: Painter? = null,
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
            .heightIn(min = height)
            .drawBehind {
                val r = cornerRadius.toPx()
                if (enabled) {
                    drawAccentPill(r, pressed)
                } else {
                    // disabled state
                }
            }
            .then(
                if (!enabled) Modifier.glassSurface(cornerRadius, alpha = AppGlass.SurfaceAlpha * 0.5f)
                else Modifier,
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
        if (iconPainter != null) {
            Image(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(if (compact) 24.dp else 22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(if (compact) 18.dp else 20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        if (trailingPainter != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Image(
                painter = trailingPainter,
                contentDescription = null,
                modifier = Modifier.size(if (compact) 18.dp else 20.dp),
            )
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
                if (pressed) Modifier.glassSurface(cornerRadius, alpha = AppGlass.SurfaceAlpha * 0.65f)
                else Modifier.glassSurface(cornerRadius),
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
            .glassSurface(
                cornerRadius = trackHeight / 2,
                alpha = if (checked) AppGlass.SurfaceAlphaStrong else AppGlass.SurfaceAlpha * 0.65f,
            )
            .drawBehind {
                if (checked) {
                    drawRoundRect(
                        color = Neo.Accent.copy(alpha = 0.35f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            trackHeight.toPx() / 2f,
                            trackHeight.toPx() / 2f,
                        ),
                    )
                }
            }
            .clip(RoundedCornerShape(trackHeight / 2))
            .clickable { onCheckedChange(!checked) }
            .padding(4.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .then(
                    if (checked) {
                        Modifier.drawBehind {
                            drawAccentGlow(12.dp.toPx(), pressed = false)
                        }
                    } else {
                        Modifier.glassSurface(12.dp, alpha = AppGlass.SurfaceAlphaStrong)
                    },
                ),
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
            .glassSurface(cornerRadius = 20.dp)
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
 * Rounded-square icon container on a frosted glass surface.
 * When [selected] it fills with the glowing orange accent.
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
            .then(
                if (selected) {
                    Modifier.drawBehind { drawAccentGlow(cornerRadius.toPx(), pressed = false) }
                } else {
                    Modifier.glassSurface(cornerRadius, alpha = AppGlass.TopBarButtonAlpha)
                },
            )
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
                .glassSurface(cornerRadius = diameter / 2, alpha = AppGlass.SurfaceAlpha * 0.65f)
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
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .safeHudPadding(
                includeTop = true,
                includeBottom = false,
                includeHorizontal = true,
            ),
    ) {
        val compact = maxWidth < 400.dp
        val edgePad = if (compact) 10.dp else 14.dp
        val backSize = if (compact) 40.dp else 44.dp
        val titleGap = if (compact) 8.dp else 14.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = edgePad, vertical = if (compact) 10.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onNavigateBack != null) {
                NeoIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onNavigateBack,
                    contentDescription = null,
                    size = backSize,
                )
                Spacer(modifier = Modifier.width(titleGap))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 0.dp),
            ) {
                Text(
                    text = title,
                    color = Neo.TextPrimary,
                    fontSize = if (compact) 17.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = Neo.TextSecondary,
                        fontSize = if (compact) 12.sp else 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            actions()
        }
    }
}
/**
 * Glass screen shell: photo background, frosted top bar, and content column.
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
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .safeHudPadding(
                        includeTop = false,
                        includeBottom = true,
                        includeHorizontal = true,
                    ),
            ) {
                val contentPad = if (maxWidth < 400.dp) 12.dp else 18.dp
                content(PaddingValues(horizontal = contentPad))
            }
        }
    }
}

/** Frosted glass dialog shell for help, codes export, and similar overlays. */
@Composable
fun NeoDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalMargin: Dp = 16.dp,
    surfaceColor: Color = AppGlass.DialogSurface,
    surfaceAlpha: Float = AppGlass.DialogSurfaceAlpha,
    scrimAlpha: Float = 0f,
    wrapContentHeight: Boolean = false,
    title: (@Composable () -> Unit)? = null,
    subtitle: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
    actions: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val heightFraction = if (configuration.screenHeightDp < 500) 0.92f else 0.86f
    val maxDialogHeight = (configuration.screenHeightDp * heightFraction)
        .dp
        .coerceAtLeast(220.dp)

    ProvideCappedFontScale(maxFontScale = 1.15f) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = true,
                dismissOnClickOutside = true,
                dismissOnBackPress = true,
            ),
        ) {
            val scrimInteraction = remember { MutableInteractionSource() }
            val cardInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (scrimAlpha > 0f) {
                            Modifier.background(Color.Black.copy(alpha = scrimAlpha))
                        } else {
                            Modifier
                        },
                    )
                    .clickable(
                        interactionSource = scrimInteraction,
                        indication = null,
                        onClick = onDismissRequest,
                    )
                    .padding(horizontal = horizontalMargin, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                val cardModifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(surfaceColor.copy(alpha = surfaceAlpha))
                    .border(
                        1.dp,
                        AppGlass.BorderColor.copy(alpha = AppGlass.BorderAlpha),
                        RoundedCornerShape(24.dp),
                    )
                    .clickable(
                        interactionSource = cardInteraction,
                        indication = null,
                        onClick = {},
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)

                if (wrapContentHeight) {
                    Column(
                        modifier = cardModifier
                            .wrapContentHeight(unbounded = false, align = Alignment.Top)
                            .heightIn(max = maxDialogHeight),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        title?.invoke()
                        subtitle?.invoke()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            content = content,
                        )
                        actions()
                    }
                } else {
                    Column(
                        modifier = cardModifier.heightIn(max = maxDialogHeight),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        title?.invoke()
                        subtitle?.invoke()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            content = content,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        actions()
                    }
                }
            }
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
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
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

