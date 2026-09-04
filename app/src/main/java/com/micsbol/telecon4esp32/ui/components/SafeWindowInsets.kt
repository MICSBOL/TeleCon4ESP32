package com.micsbol.telecon4esp32.ui.components

import android.content.res.Configuration
import android.provider.Settings
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * System bars ∪ display cutout — the insets that actually obscure content.
 *
 * Intentionally excludes gesture / "safe content" regions. Some OEM builds
 * (Huawei/EMUI especially) inflate horizontal insets so large that applying raw
 * [androidx.compose.foundation.layout.safeDrawing] collapses the UI into a
 * narrow center column with character-wrapped text.
 */
val WindowInsets.Companion.safeHud: WindowInsets
    @Composable
    get() = systemBars.union(displayCutout)

/**
 * When true, HUD chrome ignores status/navigation bar insets (bars are hidden)
 * and only clears display cutouts. Sticks and knobs can use the vacated space.
 */
val LocalHudSystemBarsHidden = compositionLocalOf { false }

/**
 * When true, [com.micsbol.telecon4esp32.ui.components.NeoDialog] overlays use the
 * RC Vehicle HUD glass shell (cyan border, compact landscape width) instead of
 * the general AppGlass dialog.
 */
val LocalHudGlassDialog = compositionLocalOf { false }

/** When 3-button nav reports 0 bottom inset, pad at least this much. */
private val BottomNavFallback = 48.dp

/**
 * Cap for a single horizontal inset in portrait.
 *
 * Xiaomi/MIUI and other curved-edge phones often report large symmetric
 * left/right display-cutout insets that do not actually obscure content.
 * Keep this tight so portrait UIs are not squeezed into a narrow column.
 */
private val MaxHorizontalInsetPortrait = 16.dp

/** Cap for a single horizontal inset in landscape (side 3-button nav). */
private val MaxHorizontalInsetLandscape = 48.dp

/** When landscape 3-button OEM reports 0 side insets, pad at least this much on the nav side only. */
private val LandscapeSideNavFallback = 48.dp

/**
 * Opposite-of-nav cutout/curve allowance in landscape. Xiaomi often leaves a large
 * unused strip on the left when we mirror nav padding — keep this tiny.
 */
private val LandscapeOppositeSideCap = 8.dp

/**
 * Landscape gesture / system bottom bars are often reported larger than the
 * visible pill. Cap so HUD screens (Control Panel) can use the vertical space.
 */
private val MaxBottomInsetLandscape = 20.dp

/** Portrait: keep content wide on curved-edge OEM phones. */
private const val MaxHorizontalFractionPortrait = 0.10f

/** Landscape: allow side nav / cutouts a bit more room. */
private const val MaxHorizontalFractionLandscape = 0.22f

/**
 * OEM-safe window insets for HUD / screen chrome.
 *
 * Prefer [safeHudPadding] on modifiers. Use this when you need a [WindowInsets]
 * value (Scaffold padding, scroll edge padding, etc.).
 */
@Composable
fun rememberClampedSafeHudInsets(
    includeTop: Boolean = true,
    includeBottom: Boolean = true,
    includeHorizontal: Boolean = true,
): WindowInsets {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val systemBarsHidden = LocalHudSystemBarsHidden.current
    val usesThreeButtonNav = remember(context.contentResolver) {
        Settings.Secure.getInt(context.contentResolver, "navigation_mode", 0) != 2
    } && !systemBarsHidden
    val base = if (systemBarsHidden) {
        WindowInsets.displayCutout
    } else {
        WindowInsets.safeHud
    }

    return remember(
        base,
        includeTop,
        includeBottom,
        includeHorizontal,
        isLandscape,
        usesThreeButtonNav,
        systemBarsHidden,
        configuration.screenWidthDp,
        density.density,
        layoutDirection,
    ) {
        resolveClampedSafeHudInsets(
            base = base,
            density = density,
            layoutDirection = layoutDirection,
            screenWidthDp = configuration.screenWidthDp,
            isLandscape = isLandscape,
            usesThreeButtonNav = usesThreeButtonNav,
            includeTop = includeTop,
            includeBottom = includeBottom,
            includeHorizontal = includeHorizontal,
        )
    }
}

/**
 * Pads content clear of system bars and cutouts, with OEM-safe clamping.
 *
 * Prefer this over raw `WindowInsets.safeDrawing` padding on root screen content.
 */
fun Modifier.safeHudPadding(
    includeTop: Boolean = false,
    includeBottom: Boolean = true,
    includeHorizontal: Boolean = true,
): Modifier = composed {
    windowInsetsPadding(
        rememberClampedSafeHudInsets(
            includeTop = includeTop,
            includeBottom = includeBottom,
            includeHorizontal = includeHorizontal,
        ),
    )
}

internal fun resolveClampedSafeHudInsets(
    base: WindowInsets,
    density: Density,
    layoutDirection: LayoutDirection,
    screenWidthDp: Int,
    isLandscape: Boolean,
    usesThreeButtonNav: Boolean,
    includeTop: Boolean,
    includeBottom: Boolean,
    includeHorizontal: Boolean,
): WindowInsets {
    val rawLeft = if (includeHorizontal) base.getLeft(density, layoutDirection) else 0
    val rawRight = if (includeHorizontal) base.getRight(density, layoutDirection) else 0
    val rawBottom = if (includeBottom) base.getBottom(density) else 0
    val top = if (includeTop) base.getTop(density) else 0
    var bottom = rawBottom

    // Portrait only: some OEMs report 0 for the bottom system bar with 3-button nav.
    // In landscape the 3-button bar sits on the left/right — never invent a bottom gap
    // (that empty strip is what Xiaomi landscape Control Panel was showing).
    if (includeBottom && !isLandscape && rawBottom == 0 && usesThreeButtonNav) {
        bottom = with(density) { BottomNavFallback.roundToPx() }
    }
    if (includeBottom && isLandscape && bottom > 0) {
        val maxBottomPx = with(density) { MaxBottomInsetLandscape.roundToPx() }
        bottom = bottom.coerceAtMost(maxBottomPx)
    }

    val screenWidthPx = with(density) { screenWidthDp.dp.roundToPx() }.coerceAtLeast(1)
    val maxPerSidePx = with(density) {
        (if (isLandscape) MaxHorizontalInsetLandscape else MaxHorizontalInsetPortrait)
            .roundToPx()
    }
    val maxHorizontalFraction =
        if (isLandscape) MaxHorizontalFractionLandscape else MaxHorizontalFractionPortrait
    val maxTotalHorizontalPx = (screenWidthPx * maxHorizontalFraction).roundToInt()
        .coerceAtLeast(maxPerSidePx)

    var left = rawLeft.coerceIn(0, maxPerSidePx)
    var right = rawRight.coerceIn(0, maxPerSidePx)

    if (includeHorizontal && isLandscape && usesThreeButtonNav) {
        val oppositeCapPx = with(density) { LandscapeOppositeSideCap.roundToPx() }
        val navLikelyPx = with(density) { LandscapeSideNavFallback.roundToPx() } / 2
        when {
            // OEM reported neither side — pad only the nav end (usually right in LTR).
            // Mirroring left+right created Xiaomi's unused left strip.
            rawLeft == 0 && rawRight == 0 -> {
                val fallbackPx = with(density) { LandscapeSideNavFallback.roundToPx() }
                    .coerceAtMost(maxPerSidePx)
                if (layoutDirection == LayoutDirection.Ltr) {
                    right = fallbackPx
                    left = 0
                } else {
                    left = fallbackPx
                    right = 0
                }
            }
            // Nav clearly on one side: keep opposite cutout tiny so content can use width.
            rawRight >= rawLeft && rawRight >= navLikelyPx -> {
                left = left.coerceAtMost(oppositeCapPx)
            }
            rawLeft > rawRight && rawLeft >= navLikelyPx -> {
                right = right.coerceAtMost(oppositeCapPx)
            }
        }
    }

    val totalHorizontal = left + right
    if (totalHorizontal > maxTotalHorizontalPx && totalHorizontal > 0) {
        left = (left * maxTotalHorizontalPx) / totalHorizontal
        right = (right * maxTotalHorizontalPx) / totalHorizontal
    }

    val minContentPx = (screenWidthPx * (1f - maxHorizontalFraction)).roundToInt()
    val contentPx = screenWidthPx - left - right
    if (contentPx < minContentPx) {
        val overflow = minContentPx - contentPx
        val reduceLeft = min(left, overflow / 2 + overflow % 2)
        val reduceRight = min(right, overflow - reduceLeft)
        left = max(0, left - reduceLeft)
        right = max(0, right - reduceRight)
    }

    return WindowInsets(left = left, top = top, right = right, bottom = bottom)
}
