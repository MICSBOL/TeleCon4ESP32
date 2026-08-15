package com.micsbol.telecon4esp32.domain.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoftApPerformancePresetTest {

    @Test
    fun `smooth maps to heavier downsample lower fps and slower ctrl`() {
        val preset = SoftApPerformancePreset.SMOOTH
        assertEquals(4, preset.hudInSampleSize)
        assertTrue(preset.useRgb565)
        assertEquals(10, preset.maxHudFps)
        assertEquals(150L, preset.softApCtrlPeriodMs)
        assertTrue(preset.softApCtrlPeriodMs <= SoftApPerformancePreset.MAX_CTRL_PERIOD_MS)
    }

    @Test
    fun `balanced is default with moderate decode and 100ms ctrl`() {
        val preset = SoftApPerformancePreset.BALANCED
        assertEquals(SoftApPerformancePreset.DEFAULT, preset)
        assertEquals(2, preset.hudInSampleSize)
        assertTrue(preset.useRgb565)
        assertEquals(15, preset.maxHudFps)
        assertEquals(100L, preset.softApCtrlPeriodMs)
    }

    @Test
    fun `high quality uses full decode and uncapped hud fps`() {
        val preset = SoftApPerformancePreset.HIGH_QUALITY
        assertEquals(1, preset.hudInSampleSize)
        assertFalse(preset.useRgb565)
        assertNull(preset.maxHudFps)
        assertEquals(100L, preset.softApCtrlPeriodMs)
        val opts = preset.toHudPreviewOptions()
        assertEquals(1, opts.inSampleSize)
        assertFalse(opts.useRgb565)
        assertNull(opts.maxHudFps)
    }

    @Test
    fun `fromStored falls back to balanced`() {
        assertEquals(SoftApPerformancePreset.BALANCED, SoftApPerformancePreset.fromStored(null))
        assertEquals(SoftApPerformancePreset.BALANCED, SoftApPerformancePreset.fromStored("nope"))
        assertEquals(
            SoftApPerformancePreset.SMOOTH,
            SoftApPerformancePreset.fromStored("SMOOTH"),
        )
    }

    @Test
    fun `camconfig query maps presets for firmware Phase 2`() {
        assertEquals(
            "framesize=qvga&quality=22&fps=10",
            SoftApPerformancePreset.SMOOTH.camConfigQuery,
        )
        assertEquals(
            "framesize=vga&quality=15&fps=0",
            SoftApPerformancePreset.BALANCED.camConfigQuery,
        )
        assertEquals(
            "framesize=vga&quality=12&fps=0",
            SoftApPerformancePreset.HIGH_QUALITY.camConfigQuery,
        )
        assertTrue(SoftApPerformancePreset.SMOOTH.mayChangeFramesize)
        assertFalse(SoftApPerformancePreset.BALANCED.mayChangeFramesize)
        assertFalse(SoftApPerformancePreset.HIGH_QUALITY.mayChangeFramesize)
    }

    @Test
    fun `min publish gap respects max fps over floor`() {
        assertEquals(100L, HudPreviewOptions(maxHudFps = 10).minPublishGapMs(16L))
        assertEquals(16L, HudPreviewOptions(maxHudFps = 60).minPublishGapMs(16L))
        assertEquals(16L, HudPreviewOptions.FULL_QUALITY.minPublishGapMs(16L))
    }
}
