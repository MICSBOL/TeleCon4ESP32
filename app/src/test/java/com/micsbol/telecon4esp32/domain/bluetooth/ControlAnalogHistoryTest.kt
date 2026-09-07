package com.micsbol.telecon4esp32.domain.bluetooth

import com.micsbol.telecon4esp32.domain.model.TelemetryChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlAnalogHistoryTest {

    @Test
    fun ingestPrefillsDrivenChannelAndDropsUnlinked() {
        val history = ControlAnalogHistory(maxPoints = 4)
        history.ingest(mapOf(TelemetryChannel.CH_1 to 0.25f))
        val first = history.snapshot()
        assertEquals(listOf(0.25f, 0.25f, 0.25f, 0.25f), first[TelemetryChannel.CH_1])

        history.ingest(mapOf(TelemetryChannel.CH_1 to 0.75f))
        assertEquals(0.75f, history.snapshot().getValue(TelemetryChannel.CH_1).last())

        history.ingest(emptyMap())
        assertTrue(history.snapshot().isEmpty())
    }
}
