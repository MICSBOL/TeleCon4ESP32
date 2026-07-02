package com.micsbol.telecon4esp32.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Soft-UI "neumorphic" design tokens.
 *
 * Defined separately from the global [MaterialTheme] color scheme so the
 * neumorphic restyle can be applied to the general app screens without
 * altering the pro application dashboards, which still rely on the existing
 * Material theme and shared component libraries.
 */
object Neo {
    // Surfaces
    val Background = Color(0xFF2B2F36)
    val Surface = Color(0xFF2B2F36)
    val SurfaceLow = Color(0xFF262A30)

    // Soft shadow pair (raised = light top-left, dark bottom-right)
    val ShadowLight = Color(0xFF3A4049)
    val ShadowDark = Color(0xFF1C1F25)

    // Accent
    val Accent = Color(0xFF3C9DF5)
    val AccentPressed = Color(0xFF2E86E0)
    val AccentHighlight = Color(0xFF6FB8FF)
    val OnAccent = Color(0xFFFFFFFF)

    // Text
    val TextPrimary = Color(0xFFE8EBEF)
    val TextSecondary = Color(0xFF8A929E)
    val TextMuted = Color(0xFF646B76)

    // Status
    val Positive = Color(0xFF53D86A)
    val Negative = Color(0xFFFF5A5A)
    val Warning = Color(0xFFF7B955)

    // Shapes
    val CardShape = RoundedCornerShape(24.dp)
    val SmallShape = RoundedCornerShape(16.dp)
    val ButtonCornerRadius = 16.dp
    val PillShape = RoundedCornerShape(ButtonCornerRadius)

    // Shadow geometry (dp values consumed by the neumorphic modifiers)
    const val ShadowOffset = 6f
    const val ShadowBlur = 14f

    val accentGradient: Brush
        get() = Brush.horizontalGradient(listOf(Accent, AccentPressed))
}
