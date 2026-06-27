package com.micsbol.telecon4esp32.domain.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LineProtocolCodecTest {

    @Test
    fun `encode builds comma-separated key value line`() {
        val line = LineProtocolCodec.encode(
            app = "GH",
            type = "SET",
            pairs = mapOf("fan" to 1, "pump" to 0),
        )

        assertEquals("GH:SET,fan,1,pump,0", line)
    }

    @Test
    fun `decode parses data line`() {
        val message = LineProtocolCodec.decode("WT:DATA,level,74,pump,0,status,0")

        assertEquals("WT", message?.app)
        assertEquals("DATA", message?.type)
        assertEquals("74", message?.values?.get("level"))
        assertEquals("0", message?.values?.get("pump"))
        assertEquals("0", message?.values?.get("status"))
    }

    @Test
    fun `decode returns null for malformed line`() {
        assertNull(LineProtocolCodec.decode("not-a-valid-line"))
        assertNull(LineProtocolCodec.decode("RC:"))
        assertNull(LineProtocolCodec.decode(":DATA,temp,1"))
    }
}
