package com.micsbol.telecon4esp32.domain.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LineProtocolCodecTest {

    @Test
    fun `encode builds comma-separated key value line`() {
        val line = LineProtocolCodec.encode(
            app = "RC",
            type = "SET",
            pairs = mapOf("steer_center" to 1, "echo" to 0),
        )

        assertEquals("RC:SET,steer_center,1,echo,0", line)
    }

    @Test
    fun `decode parses data line`() {
        val message = LineProtocolCodec.decode("RC:DATA,batt,74,left,150,status,0")

        assertEquals("RC", message?.app)
        assertEquals("DATA", message?.type)
        assertEquals("74", message?.values?.get("batt"))
        assertEquals("150", message?.values?.get("left"))
        assertEquals("0", message?.values?.get("status"))
    }

    @Test
    fun `decode returns null for malformed line`() {
        assertNull(LineProtocolCodec.decode("not-a-valid-line"))
        assertNull(LineProtocolCodec.decode("RC:"))
        assertNull(LineProtocolCodec.decode(":DATA,temp,1"))
    }
}
