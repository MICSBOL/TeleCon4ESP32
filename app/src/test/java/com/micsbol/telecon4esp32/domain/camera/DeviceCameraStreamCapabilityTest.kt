package com.micsbol.telecon4esp32.domain.camera

import org.junit.Assert.assertEquals
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
}
