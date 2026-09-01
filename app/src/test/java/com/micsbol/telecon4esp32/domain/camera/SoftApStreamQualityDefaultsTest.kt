package com.micsbol.telecon4esp32.domain.camera

import com.micsbol.telecon4esp32.domain.model.ApplicationId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoftApStreamQualityDefaultsTest {

    @Test
    fun recommendedSoftApStreamQualityForAtRiskDevice_usesSmoothAndFps8() {
        val defaults = recommendedSoftApStreamQualityForAtRiskDevice()
        assertEquals(SoftApPerformancePreset.SMOOTH, defaults.preset)
        assertEquals(SoftApHudProcessingRate.FPS_8, defaults.hudRate)
    }

    @Test
    fun resolveSoftApHudPreviewOptions_whenNotStreaming_returnsFullQuality() {
        val options = resolveSoftApHudPreviewOptions(
            preset = SoftApPerformancePreset.SMOOTH,
            hudRate = SoftApHudProcessingRate.FPS_5,
            streaming = false,
        )
        assertEquals(HudPreviewOptions.FULL_QUALITY, options)
    }

    @Test
    fun resolveSoftApHudPreviewOptions_whenStreaming_appliesPresetAndHudRate() {
        val options = resolveSoftApHudPreviewOptions(
            preset = SoftApPerformancePreset.SMOOTH,
            hudRate = SoftApHudProcessingRate.FPS_8,
            streaming = true,
        )
        assertEquals(4, options.inSampleSize)
        assertTrue(options.useRgb565)
        assertEquals(8, options.maxHudFps)
    }

    @Test
    fun resolveSoftApHudPreviewOptions_autoHudRate_followsPreset() {
        val options = resolveSoftApHudPreviewOptions(
            preset = SoftApPerformancePreset.BALANCED,
            hudRate = SoftApHudProcessingRate.AUTO,
            streaming = true,
        )
        assertEquals(15, options.maxHudFps)
    }

    @Test
    fun supportsRuntimeSoftApStreamQuality_onlyRcAndControlPanel() {
        assertTrue(ApplicationId.RC_VEHICLE_PRO.supportsRuntimeSoftApStreamQuality())
        assertTrue(ApplicationId.CONTROL_PANEL.supportsRuntimeSoftApStreamQuality())
        assertFalse(ApplicationId.GREENHOUSE.supportsRuntimeSoftApStreamQuality())
        assertFalse(ApplicationId.SMART_DOOR_LOCK.supportsRuntimeSoftApStreamQuality())
    }

    @Test
    fun presetCamConfigQuery_matchesAndroidContract() {
        assertEquals(
            "framesize=qqvga&quality=28&fps=8&ampdu_rx=0",
            SoftApPerformancePreset.SMOOTH.camConfigQuery,
        )
        assertEquals(
            "framesize=vga&quality=15&fps=12",
            SoftApPerformancePreset.BALANCED.camConfigQuery,
        )
        assertEquals(
            "framesize=vga&quality=12&fps=0",
            SoftApPerformancePreset.HIGH_QUALITY.camConfigQuery,
        )
    }

    @Test
    fun shouldPreferCapturePolling_forSmoothOrAtRiskWhileStreaming() {
        assertTrue(
            shouldPreferCapturePollingForLowLatency(
                preset = SoftApPerformancePreset.SMOOTH,
                atRisk = false,
                streaming = true,
            ),
        )
        assertTrue(
            shouldPreferCapturePollingForLowLatency(
                preset = SoftApPerformancePreset.BALANCED,
                atRisk = true,
                streaming = true,
            ),
        )
        assertFalse(
            shouldPreferCapturePollingForLowLatency(
                preset = SoftApPerformancePreset.BALANCED,
                atRisk = false,
                streaming = true,
            ),
        )
        assertFalse(
            shouldPreferCapturePollingForLowLatency(
                preset = SoftApPerformancePreset.SMOOTH,
                atRisk = true,
                streaming = false,
            ),
        )
    }
}
