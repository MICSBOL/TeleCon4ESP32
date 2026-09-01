package com.micsbol.telecon4esp32.ui.control_panel

import android.content.pm.ActivityInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class HudScreenOrientationTest {

    @Test
    fun huaweiAlwaysUsesFixedLandscape() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            resolveHudLandscapeOrientation(manufacturer = "HUAWEI", brand = "honor"),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            resolveHudLandscapeOrientation(manufacturer = "HONOR", brand = "honor"),
        )
    }

    @Test
    fun portraitFallbackForcesFixedLandscapeOnAnyBrand() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            resolveHudLandscapeOrientation(
                manufacturer = "samsung",
                brand = "samsung",
                currentlyPortrait = true,
            ),
        )
    }

    @Test
    fun samsungAllowsEitherLandscapeDirectionWhenAlreadyLandscape() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolveHudLandscapeOrientation(
                manufacturer = "samsung",
                brand = "samsung",
                currentlyPortrait = false,
            ),
        )
    }

    @Test
    fun googlePixelAllowsEitherLandscapeDirection() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolveHudLandscapeOrientation(manufacturer = "Google", brand = "google"),
        )
    }
}
