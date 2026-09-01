package com.micsbol.telecon4esp32.ui.control_panel

import org.junit.Assert.assertEquals
import org.junit.Test

class PlotOnTopTest {

    @Test
    fun `selecting on top in a pane clears the sibling`() {
        val none = listOf(false, false, false, false)
        assertEquals(
            listOf(true, false, false, false),
            exclusivePlotOnTop(none, selectedIndex = 0, paneStart = 0),
        )
        assertEquals(
            listOf(false, true, false, false),
            exclusivePlotOnTop(
                listOf(true, false, false, false),
                selectedIndex = 1,
                paneStart = 0,
            ),
        )
    }

    @Test
    fun `bottom pane on top does not change the top pane`() {
        val current = listOf(true, false, false, false)
        assertEquals(
            listOf(true, false, false, true),
            exclusivePlotOnTop(current, selectedIndex = 1, paneStart = 2),
        )
    }
}
