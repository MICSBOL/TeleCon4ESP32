package com.micsbol.telecon4esp32.domain.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class Esp32CameraDefaultsTest {

    @Test
    fun streamUrl_appendsStreamPath() {
        assertEquals(
            "http://192.168.4.1/stream",
            Esp32CameraDefaults.streamUrl(),
        )
        assertEquals(
            "http://192.168.4.1/stream",
            Esp32CameraDefaults.streamUrl("http://192.168.4.1/"),
        )
    }

    @Test
    fun captureUrl_appendsCapturePath() {
        assertEquals(
            "http://192.168.4.1/capture",
            Esp32CameraDefaults.captureUrl(),
        )
    }

    @Test
    fun softApHostFromBaseUrl_stripsSchemeAndPath() {
        assertEquals(
            "192.168.4.1",
            Esp32CameraDefaults.softApHostFromBaseUrl("http://192.168.4.1/capture"),
        )
    }

    @Test
    fun camConfigUrl_appendsPathAndQuery() {
        assertEquals(
            "http://192.168.4.1/camconfig?framesize=qvga&quality=22&fps=10",
            Esp32CameraDefaults.camConfigUrl(
                query = "framesize=qvga&quality=22&fps=10",
            ),
        )
        assertEquals(
            "http://192.168.4.1/camconfig?framesize=vga&quality=15&fps=0",
            Esp32CameraDefaults.camConfigUrl(
                "http://192.168.4.1/",
                "?framesize=vga&quality=15&fps=0",
            ),
        )
    }
}
