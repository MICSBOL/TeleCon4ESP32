package com.micsbol.telecon4esp32.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeWindowInsetsTest {

    private val density = Density(density = 2.5f, fontScale = 1f)

    @Test
    fun clampsAbsurdHorizontalInsetsInPortrait() {
        // Simulate Huawei-style inflated left/right (~120dp each on a 360dp screen).
        val base = WindowInsets(left = 300, top = 60, right = 300, bottom = 120)
        val resolved = resolveClampedSafeHudInsets(
            base = base,
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            screenWidthDp = 360,
            isLandscape = false,
            usesThreeButtonNav = true,
            includeTop = true,
            includeBottom = true,
            includeHorizontal = true,
        )

        val screenWidthPx = with(density) { 360.dp.roundToPx() }
        val left = resolved.getLeft(density, LayoutDirection.Ltr)
        val right = resolved.getRight(density, LayoutDirection.Ltr)
        val contentPx = screenWidthPx - left - right
        val maxSide = with(density) { 16.dp.roundToPx() }

        assertTrue("left too large: $left", left <= maxSide)
        assertTrue("right too large: $right", right <= maxSide)
        assertTrue(
            "content too narrow: $contentPx / $screenWidthPx",
            contentPx >= (screenWidthPx * 0.90f).toInt(),
        )
        assertEquals(60, resolved.getTop(density))
        assertEquals(120, resolved.getBottom(density))
    }

    @Test
    fun landscapeThreeButtonZeroInsetsGetsSideFallbackOnly() {
        val base = WindowInsets(left = 0, top = 40, right = 0, bottom = 0)
        val resolved = resolveClampedSafeHudInsets(
            base = base,
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            screenWidthDp = 800,
            isLandscape = true,
            usesThreeButtonNav = true,
            includeTop = true,
            includeBottom = true,
            includeHorizontal = true,
        )

        val expected = with(density) { 48.dp.roundToPx() }
        // Pad only the nav end (right in LTR) — do not invent a matching left gap (Xiaomi).
        assertEquals(0, resolved.getLeft(density, LayoutDirection.Ltr))
        assertEquals(expected, resolved.getRight(density, LayoutDirection.Ltr))
        assertEquals(0, resolved.getBottom(density))
    }

    @Test
    fun landscapeThreeButtonNavOnRightKeepsLeftCutoutTiny() {
        val navRight = with(density) { 48.dp.roundToPx() }
        val fatLeftCutout = with(density) { 40.dp.roundToPx() }
        val base = WindowInsets(left = fatLeftCutout, top = 40, right = navRight, bottom = 0)
        val resolved = resolveClampedSafeHudInsets(
            base = base,
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            screenWidthDp = 800,
            isLandscape = true,
            usesThreeButtonNav = true,
            includeTop = true,
            includeBottom = true,
            includeHorizontal = true,
        )

        val oppositeCap = with(density) { 8.dp.roundToPx() }
        assertTrue(
            "left cutout should stay tiny when nav is on the right",
            resolved.getLeft(density, LayoutDirection.Ltr) <= oppositeCap,
        )
        assertEquals(navRight.coerceAtMost(with(density) { 48.dp.roundToPx() }),
            resolved.getRight(density, LayoutDirection.Ltr))
    }

    @Test
    fun gestureNavLandscapeDoesNotForceFallback() {
        val base = WindowInsets(left = 0, top = 40, right = 0, bottom = 24)
        val resolved = resolveClampedSafeHudInsets(
            base = base,
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            screenWidthDp = 800,
            isLandscape = true,
            usesThreeButtonNav = false,
            includeTop = true,
            includeBottom = true,
            includeHorizontal = true,
        )

        assertEquals(0, resolved.getLeft(density, LayoutDirection.Ltr))
        assertEquals(0, resolved.getRight(density, LayoutDirection.Ltr))
        assertEquals(24, resolved.getBottom(density))
    }

    @Test
    fun landscapeCapsLargeGestureBottomInset() {
        // Huawei/OEM gesture bars often report ~48dp+; keep HUD usable.
        val largeBottom = with(density) { 56.dp.roundToPx() }
        val base = WindowInsets(left = 0, top = 40, right = 0, bottom = largeBottom)
        val resolved = resolveClampedSafeHudInsets(
            base = base,
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            screenWidthDp = 800,
            isLandscape = true,
            usesThreeButtonNav = false,
            includeTop = true,
            includeBottom = true,
            includeHorizontal = true,
        )

        val maxBottom = with(density) { 20.dp.roundToPx() }
        assertEquals(maxBottom, resolved.getBottom(density))
    }

    @Test
    fun threeButtonZeroBottomInsetGetsFallback() {
        val base = WindowInsets(left = 0, top = 60, right = 0, bottom = 0)
        val resolved = resolveClampedSafeHudInsets(
            base = base,
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            screenWidthDp = 360,
            isLandscape = false,
            usesThreeButtonNav = true,
            includeTop = true,
            includeBottom = true,
            includeHorizontal = true,
        )

        val expected = with(density) { 48.dp.roundToPx() }
        assertEquals(expected, resolved.getBottom(density))
    }

    @Test
    fun gestureNavZeroBottomDoesNotForceFallback() {
        val base = WindowInsets(left = 0, top = 60, right = 0, bottom = 0)
        val resolved = resolveClampedSafeHudInsets(
            base = base,
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            screenWidthDp = 360,
            isLandscape = false,
            usesThreeButtonNav = false,
            includeTop = true,
            includeBottom = true,
            includeHorizontal = true,
        )

        assertEquals(0, resolved.getBottom(density))
    }

    @Test
    fun portraitZeroTopInsetGetsStatusBarFallback() {
        val base = WindowInsets(left = 0, top = 0, right = 0, bottom = 60)
        val resolved = resolveClampedSafeHudInsets(
            base = base,
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            screenWidthDp = 360,
            isLandscape = false,
            usesThreeButtonNav = false,
            systemBarsHidden = false,
            includeTop = true,
            includeBottom = true,
            includeHorizontal = true,
        )

        val expected = with(density) { 32.dp.roundToPx() }
        assertEquals(expected, resolved.getTop(density))
    }

    @Test
    fun hiddenSystemBarsDoNotInventPortraitStatusBarFallback() {
        val base = WindowInsets(left = 0, top = 0, right = 0, bottom = 0)
        val resolved = resolveClampedSafeHudInsets(
            base = base,
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            screenWidthDp = 360,
            isLandscape = false,
            usesThreeButtonNav = false,
            systemBarsHidden = true,
            includeTop = true,
            includeBottom = true,
            includeHorizontal = true,
        )

        assertEquals(0, resolved.getTop(density))
    }
}
