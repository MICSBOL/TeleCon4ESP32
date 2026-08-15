package com.micsbol.telecon4esp32.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Caps [LocalDensity.fontScale] so dialog / overlay copy stays usable on devices
 * with aggressive system font scaling (some OEM defaults look much larger than
 * flagships like Pixel / Galaxy S23 at the same dp size).
 *
 * Density (px per dp) is preserved — only fontScale is clamped.
 */
@Composable
fun ProvideCappedFontScale(
    maxFontScale: Float = 1.15f,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val capped = remember(density.density, density.fontScale, maxFontScale) {
        Density(
            density = density.density,
            fontScale = density.fontScale.coerceAtMost(maxFontScale),
        )
    }
    CompositionLocalProvider(LocalDensity provides capped, content = content)
}
