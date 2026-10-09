package com.micsbol.telecon4esp32.domain.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkTrafficLogTest {

    @Test
    fun `changed payload is printed and an identical one repeats in place`() {
        val log = LinkTrafficLog()
        log.record(LinkTrafficDirection.SENT, "RC:CTRL,1500,1500")
        log.record(LinkTrafficDirection.SENT, "RC:CTRL,1500,1500")
        log.record(LinkTrafficDirection.SENT, "RC:CTRL,1600,1400")
        log.record(LinkTrafficDirection.RECEIVED, "RC:DATA,1,2")
        log.record(LinkTrafficDirection.RECEIVED, "RC:DATA,1,2")
        log.record(LinkTrafficDirection.SENT, "RC:BTN,id,1")

        val lines = log.entries.value
        assertEquals(4, lines.size)
        assertEquals("RC:CTRL,1500,1500", lines[0].text)
        assertEquals(1, lines[0].repeatCount)
        assertEquals(1L, lines[0].repeatEpoch)
        assertEquals("RC:CTRL,1600,1400", lines[1].text)
        assertEquals(0, lines[1].repeatCount)
        assertEquals("RC:DATA,1,2", lines[2].text)
        assertEquals(1, lines[2].repeatCount)
        assertEquals("RC:BTN,id,1", lines[3].text)
        assertTrue(lines.map { it.id }.distinct().size == lines.size)
    }

    @Test
    fun `identical send repeats across a newer receive`() {
        val log = LinkTrafficLog()
        log.record(LinkTrafficDirection.SENT, "BIN AA55 18B AA")
        log.record(LinkTrafficDirection.RECEIVED, "BIN CC33 14B CC")
        log.record(LinkTrafficDirection.SENT, "BIN AA55 18B AA")

        val lines = log.entries.value
        assertEquals(2, lines.size)
        assertEquals("BIN CC33 14B CC", lines[0].text)
        assertEquals(0, lines[0].repeatCount)
        assertEquals("BIN AA55 18B AA", lines[1].text)
        assertEquals(1, lines[1].repeatCount)
    }

    @Test
    fun `binary frames keep a short hex header`() {
        val formatted = formatLinkBinary(byteArrayOf(0xAA.toByte(), 0x55, 0x01, 0x02))
        assertTrue(formatted.startsWith("BIN AA55 4B"))
        val log = LinkTrafficLog()
        log.record(LinkTrafficDirection.SENT, formatted)
        log.record(LinkTrafficDirection.SENT, formatted)
        log.record(LinkTrafficDirection.SENT, formatLinkBinary(byteArrayOf(0xAA.toByte(), 0x55, 0x03)))
        log.record(LinkTrafficDirection.RECEIVED, formatLinkBinary(byteArrayOf(0xCC.toByte(), 0x33, 0x01)))
        assertEquals(3, log.entries.value.size)
        assertEquals(1, log.entries.value[0].repeatCount)
        assertTrue(log.entries.value[0].text.startsWith("BIN AA55"))
        assertTrue(log.entries.value[1].text.startsWith("BIN AA55"))
        assertTrue(log.entries.value[2].text.startsWith("BIN CC33"))
    }
}
