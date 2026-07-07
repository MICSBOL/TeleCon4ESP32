package com.micsbol.telecon4esp32.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shared shell design tokens (glassmorphism).
 *
 * The [Neo] name is kept for backward compatibility with existing screen code.
 * Pro application dashboards use their own per-app Glass token objects.
 */
object Neo {
    // Surfaces
    val Background = AppGlass.BackgroundTop
    val Surface = AppGlass.CardSurface.copy(alpha = AppGlass.SurfaceAlpha)
    val SurfaceLow = AppGlass.CardSurface.copy(alpha = AppGlass.SurfaceAlpha * 0.65f)

    // Legacy shadow pair — unused by glass surfaces; kept for any remaining references
    val ShadowLight = Color(0xFF3A3A3A)
    val ShadowDark = Color(0xFF0D0D0D)

    // Accent
    val Accent = AppGlass.Accent
    val AccentPressed = AppGlass.AccentPressed
    val AccentHighlight = AppGlass.AccentHighlight
    val OnAccent = AppGlass.OnAccent

    // Text
    val TextPrimary = AppGlass.TextPrimary
    val TextSecondary = AppGlass.TextSecondary
    val TextMuted = AppGlass.TextMuted

    // Status
    val Positive = AppGlass.Positive
    val Negative = AppGlass.Negative
    val Warning = AppGlass.Warning

    // Shapes
    val CardShape = AppGlass.CardShape
    val SmallShape = AppGlass.SmallCardShape
    val ButtonCornerRadius = 16.dp
    val PillShape = AppGlass.PillShape

    // Shadow geometry — legacy; glass surfaces ignore these
    const val ShadowOffset = 6f
    const val ShadowBlur = 14f

    val accentGradient: Brush
        get() = AppGlass.accentGradient
}
