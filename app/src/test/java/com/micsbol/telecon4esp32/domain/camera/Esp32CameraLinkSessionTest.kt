package com.micsbol.telecon4esp32.domain.camera

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Esp32CameraLinkSessionTest {

    private lateinit var fake: FakeCameraStreamRepository
    private lateinit var session: Esp32CameraLinkSession

    @Before
    fun setUp() {
        fake = FakeCameraStreamRepository()
        session = Esp32CameraLinkSession(
            repository = fake,
            baseUrl = Esp32CameraDefaults.DEFAULT_BASE_URL,
            skipOnEmulator = false,
        )
    }

    @Test
    fun startStream_onlyOnce_whileAlreadyStreaming() {
        session.setCameraEnabled(false)
        session.setProfile(CameraLinkProfile.WIFI_CAMERA_DEVKIT_BLE)
        session.setCameraEnabled(true)
        session.onVisible()
        assertEquals(1, fake.startCount)

        // DataStore / profile re-emits must not tear down HTTP MJPEG.
        session.setProfile(CameraLinkProfile.WIFI_CAMERA_DEVKIT_BLE)
        session.setCameraEnabled(true)
        session.onVisible()
        assertEquals(1, fake.startCount)
        assertEquals(0, fake.stopCount)
    }

    @Test
    fun stop_whenDisabledOrHidden() {
        session.setProfile(CameraLinkProfile.WIFI_SOFTAP)
        session.onVisible()
        assertEquals(1, fake.startCount)

        session.setCameraEnabled(false)
        assertEquals(1, fake.stopCount)
        assertFalse(fake.isStreaming)

        session.setCameraEnabled(true)
        assertEquals(2, fake.startCount)
        assertTrue(fake.isStreaming)

        session.onHidden()
        assertEquals(2, fake.stopCount)
    }

    @Test
    fun restartIfStreaming_forceStopsThenStarts() {
        session.setProfile(CameraLinkProfile.WIFI_SOFTAP)
        session.onVisible()
        assertEquals(1, fake.startCount)

        session.restartIfStreaming()
        assertEquals(1, fake.stopCount)
        assertEquals(2, fake.startCount)
        assertTrue(fake.isStreaming)
    }

    @Test
    fun restartIfStreaming_noopWhenNotStreaming() {
        session.setProfile(CameraLinkProfile.WIFI_SOFTAP)
        session.restartIfStreaming()
        assertEquals(0, fake.startCount)
        assertEquals(0, fake.stopCount)
    }

    private class FakeCameraStreamRepository : CameraStreamRepository {
        private val _state = MutableStateFlow<CameraStreamState>(CameraStreamState.Idle)
        override val streamState: StateFlow<CameraStreamState> = _state.asStateFlow()

        var startCount = 0
        var stopCount = 0
        var isStreaming = false

        override fun startStream(baseUrl: String) {
            startCount++
            isStreaming = true
            _state.value = CameraStreamState.Connecting
        }

        override fun stopStream() {
            stopCount++
            isStreaming = false
            _state.value = CameraStreamState.Idle
        }
    }
}
