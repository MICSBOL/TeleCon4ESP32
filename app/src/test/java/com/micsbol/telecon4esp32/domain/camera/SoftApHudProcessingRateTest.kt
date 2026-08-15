package com.micsbol.telecon4esp32.domain.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SoftApHudProcessingRateTest {

    @Test
    fun `auto follows each preset maxHudFps`() {
        SoftApPerformancePreset.entries.forEach { preset ->
            assertEquals(
                preset.maxHudFps,
                SoftApHudProcessingRate.AUTO.resolveMaxHudFps(preset),
            )
        }
    }

    @Test
    fun `explicit rates override smooth balanced and high`() {
        SoftApPerformancePreset.entries.forEach { preset ->
            assertEquals(5, SoftApHudProcessingRate.FPS_5.resolveMaxHudFps(preset))
            assertEquals(8, SoftApHudProcessingRate.FPS_8.resolveMaxHudFps(preset))
            assertEquals(10, SoftApHudProcessingRate.FPS_10.resolveMaxHudFps(preset))
            assertEquals(12, SoftApHudProcessingRate.FPS_12.resolveMaxHudFps(preset))
            assertEquals(15, SoftApHudProcessingRate.FPS_15.resolveMaxHudFps(preset))
            assertNull(SoftApHudProcessingRate.UNCAPPED.resolveMaxHudFps(preset))
        }
    }

    @Test
    fun `high quality with capped rate honours explicit fps`() {
        assertEquals(
            5,
            SoftApHudProcessingRate.FPS_5.resolveMaxHudFps(SoftApPerformancePreset.HIGH_QUALITY),
        )
        assertEquals(
            15,
            SoftApHudProcessingRate.FPS_15.resolveMaxHudFps(SoftApPerformancePreset.HIGH_QUALITY),
        )
    }

    @Test
    fun `fromStored null or invalid returns auto`() {
        assertEquals(SoftApHudProcessingRate.AUTO, SoftApHudProcessingRate.fromStored(null))
        assertEquals(SoftApHudProcessingRate.AUTO, SoftApHudProcessingRate.fromStored("nope"))
        assertEquals(
            SoftApHudProcessingRate.FPS_5,
            SoftApHudProcessingRate.fromStored("FPS_5"),
        )
        assertEquals(SoftApHudProcessingRate.DEFAULT, SoftApHudProcessingRate.AUTO)
    }

    @Test
    fun `effective hud options keep preset decode and override max fps`() {
        val preset = SoftApPerformancePreset.SMOOTH
        val rate = SoftApHudProcessingRate.FPS_5
        val opts = preset.toHudPreviewOptions().copy(
            maxHudFps = rate.resolveMaxHudFps(preset),
        )
        assertEquals(preset.hudInSampleSize, opts.inSampleSize)
        assertEquals(preset.useRgb565, opts.useRgb565)
        assertEquals(5, opts.maxHudFps)
        assertEquals(200L, opts.minPublishGapMs(16L))
    }
}
