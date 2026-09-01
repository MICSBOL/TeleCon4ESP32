package com.micsbol.telecon4esp32.domain.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCameraStreamCapabilityTest {

    @Test
    fun `low ram device is at risk`() {
        assertEquals(
            DeviceCameraStreamRisk.AT_RISK,
            assessDeviceCameraStreamRisk(
                DeviceCameraStreamSignals(
                    memoryClassMb = 512,
                    isLowRamDevice = true,
                    sdkInt = 34,
                ),
            ),
        )
    }

    @Test
    fun `old sdk is at risk`() {
        assertEquals(
            DeviceCameraStreamRisk.AT_RISK,
            assessDeviceCameraStreamRisk(
                DeviceCameraStreamSignals(
                    memoryClassMb = 512,
                    isLowRamDevice = false,
                    sdkInt = 25,
                ),
            ),
        )
    }

    @Test
    fun `mid range heap is at risk`() {
        assertEquals(
            DeviceCameraStreamRisk.AT_RISK,
            assessDeviceCameraStreamRisk(
                DeviceCameraStreamSignals(
                    memoryClassMb = 384,
                    isLowRamDevice = false,
                    sdkInt = 28,
                ),
            ),
        )
    }

    @Test
    fun `high memory modern device is ok`() {
        assertEquals(
            DeviceCameraStreamRisk.OK,
            assessDeviceCameraStreamRisk(
                DeviceCameraStreamSignals(
                    memoryClassMb = 512,
                    isLowRamDevice = false,
                    sdkInt = 34,
                ),
            ),
        )
    }

    @Test
    fun `poco x3 model is at risk even with large heap`() {
        assertEquals(
            DeviceCameraStreamRisk.AT_RISK,
            assessDeviceCameraStreamRisk(
                DeviceCameraStreamSignals(
                    memoryClassMb = 512,
                    isLowRamDevice = false,
                    sdkInt = 31,
                    manufacturer = "Xiaomi",
                    model = "M2102J20SG",
                ),
            ),
        )
        assertTrue(isKnownSoftApLaggyClient("Xiaomi", "M2102J20SG"))
        assertTrue(isKnownSoftApLaggyClient("POCO", "POCO X3 NFC"))
        assertFalse(isKnownSoftApLaggyClient("Google", "Pixel 8"))
        assertFalse(isKnownSoftApLaggyClient("samsung", "SM-S918B"))
    }

    @Test
    fun `samsung s23 ultra is ok despite 256 mb heap class`() {
        assertEquals(
            DeviceCameraStreamRisk.OK,
            assessDeviceCameraStreamRisk(
                DeviceCameraStreamSignals(
                    memoryClassMb = 256,
                    isLowRamDevice = false,
                    sdkInt = 34,
                    manufacturer = "samsung",
                    model = "SM-S918B",
                    totalRamMb = 7_400,
                    mediaPerformanceClass = 33,
                ),
            ),
        )
    }

    @Test
    fun `flagship ram is ok without media performance class`() {
        assertEquals(
            DeviceCameraStreamRisk.OK,
            assessDeviceCameraStreamRisk(
                DeviceCameraStreamSignals(
                    memoryClassMb = 256,
                    isLowRamDevice = false,
                    sdkInt = 33,
                    manufacturer = "samsung",
                    model = "SM-S918B",
                    totalRamMb = 7_400,
                    mediaPerformanceClass = 0,
                ),
            ),
        )
    }

    @Test
    fun `six gb mid range ram is at risk`() {
        assertEquals(
            DeviceCameraStreamRisk.AT_RISK,
            assessDeviceCameraStreamRisk(
                DeviceCameraStreamSignals(
                    memoryClassMb = 256,
                    isLowRamDevice = false,
                    sdkInt = 33,
                    manufacturer = "samsung",
                    model = "SM-A546B",
                    totalRamMb = 5_500,
                    mediaPerformanceClass = 0,
                ),
            ),
        )
    }

    @Test
    fun `four gb low end ram is at risk`() {
        assertEquals(
            DeviceCameraStreamRisk.AT_RISK,
            assessDeviceCameraStreamRisk(
                DeviceCameraStreamSignals(
                    memoryClassMb = 192,
                    isLowRamDevice = false,
                    sdkInt = 31,
                    totalRamMb = 3_600,
                ),
            ),
        )
    }
}
