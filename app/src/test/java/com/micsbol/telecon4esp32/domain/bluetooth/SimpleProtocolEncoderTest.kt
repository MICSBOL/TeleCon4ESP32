package com.micsbol.telecon4esp32.domain.bluetooth

import com.micsbol.telecon4esp32.domain.model.ButtonEvent
import com.micsbol.telecon4esp32.domain.model.RcState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleProtocolEncoderTest {

    @Test
    fun `buildRcCtrlLine encodes sticks knobs and switches`() {
        val line = SimpleProtocolEncoder.buildRcCtrlLine(
            RcState(
                leftStickX = -50,
                leftStickY = 100,
                rightStickX = 0,
                rightStickY = 25,
                switch1 = true,
                switch2 = true,
                leftKnobValue = 512,
                rightKnobValue = 256,
            ),
        )

        assertTrue(line.startsWith("RC:CTRL,"))
        assertTrue(line.contains("lx,-50"))
        assertTrue(line.contains("sw,03"))
    }

    @Test
    fun `buildRcButtonLine encodes button id`() {
        val line = SimpleProtocolEncoder.buildRcButtonLine(ButtonEvent.CENTER_TOP_LEFT)
        assertEquals("RC:BTN,id,1", line)
    }

    @Test
    fun `buildSetLine uses application prefix`() {
        val line = SimpleProtocolEncoder.buildSetLine("RC", mapOf("steer_center" to 1))
        assertEquals("RC:SET,steer_center,1", line)
    }

    @Test
    fun `buildSteerCenterSaveLine encodes center flag and rx`() {
        val line = SimpleProtocolEncoder.buildSteerCenterSaveLine(rxChannel = -3)
        assertTrue(line.startsWith("RC:SET,"))
        assertTrue(line.contains("steer_center,1"))
        assertTrue(line.contains("rx,-3"))
    }

    @Test
    fun `buildRcButtonLine encodes steer center save id`() {
        val line = SimpleProtocolEncoder.buildRcButtonLine(ButtonEvent.STEER_CENTER_SAVE)
        assertEquals("RC:BTN,id,16", line)
    }
}
