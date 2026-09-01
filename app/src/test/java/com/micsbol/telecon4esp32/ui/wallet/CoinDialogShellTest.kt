package com.micsbol.telecon4esp32.ui.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinDialogShellTest {

    @Test
    fun huaweiLandscapeIsCompactAndFitsShortHeight() {
        // Typical Huawei landscape HUD after rotation (~780x360 dp).
        val limits = resolveCoinDialogLimits(
            screenWidthDp = 780,
            screenHeightDp = 360,
            isLandscape = true,
        )
        assertTrue(limits.compact)
        assertTrue(
            "card must stay narrower than the HUD: ${limits.maxWidth}",
            limits.maxWidth.value <= 420f,
        )
        assertTrue(
            "card must not exceed the short window: ${limits.maxHeight}",
            limits.maxHeight.value <= 360f * 0.94f + 0.5f,
        )
        assertTrue(limits.maxHeight.value >= 220f)
    }

    @Test
    fun portraitPhoneIsNotCompact() {
        val limits = resolveCoinDialogLimits(
            screenWidthDp = 360,
            screenHeightDp = 780,
            isLandscape = false,
        )
        assertFalse(limits.compact)
        assertEquals(312f, limits.maxWidth.value, 0.1f)
    }

    @Test
    fun tabletPortraitKeepsWideCard() {
        val limits = resolveCoinDialogLimits(
            screenWidthDp = 800,
            screenHeightDp = 1280,
            isLandscape = false,
        )
        assertFalse(limits.compact)
        assertEquals(440f, limits.maxWidth.value, 0.1f)
    }
}
