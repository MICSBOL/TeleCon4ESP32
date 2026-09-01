package com.micsbol.telecon4esp32.ui.applications

import org.junit.Assert.assertEquals
import org.junit.Test

class ApplicationSettingsSectionTest {

    @Test
    fun `fromNav maps known section names`() {
        assertEquals(
            ApplicationSettingsSection.CONNECTION,
            ApplicationSettingsSection.fromNav("CONNECTION"),
        )
        assertEquals(
            ApplicationSettingsSection.CONNECTION,
            ApplicationSettingsSection.fromNav("panel"),
        )
    }

    @Test
    fun `fromNav defaults to connection`() {
        assertEquals(
            ApplicationSettingsSection.CONNECTION,
            ApplicationSettingsSection.fromNav(null),
        )
        assertEquals(
            ApplicationSettingsSection.CONNECTION,
            ApplicationSettingsSection.fromNav("unknown"),
        )
    }
}
