package com.micsbol.telecon4esp32.ui.cyber.screens

import com.micsbol.telecon4esp32.domain.model.ApplicationId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeModuleCardDeckTest {

    @Test
    fun `featured module starts in front`() {
        val modules = listOf(ApplicationId.CONTROL_PANEL, ApplicationId.RC_VEHICLE_PRO)
        assertEquals(
            listOf(ApplicationId.RC_VEHICLE_PRO, ApplicationId.CONTROL_PANEL),
            initialHomeModuleDeckOrder(modules, ApplicationId.RC_VEHICLE_PRO),
        )
    }

    @Test
    fun `horizontal swipe cycles both directions with more than two modules`() {
        val order = listOf("control", "rc", "smart")
        val next = rotateHomeModuleDeckNext(order)
        val previous = rotateHomeModuleDeckPrevious(order)
        assertEquals(listOf("rc", "smart", "control"), next)
        assertEquals(listOf("smart", "control", "rc"), previous)
        assertEquals(order, rotateHomeModuleDeckPrevious(next))
        assertEquals(order, rotateHomeModuleDeckNext(previous))
    }

    @Test
    fun `two modules swap with either swipe direction`() {
        val order = listOf("control", "rc")
        assertEquals(listOf("rc", "control"), rotateHomeModuleDeckNext(order))
        assertEquals(listOf("rc", "control"), rotateHomeModuleDeckPrevious(order))
    }

    @Test
    fun `fan pose tilts next right and previous left`() {
        val front = homeModuleFanPose(depth = 0, count = 3)
        val next = homeModuleFanPose(depth = 1, count = 3)
        val previous = homeModuleFanPose(depth = 2, count = 3)
        assertEquals(-1.5f, front.rotation, 0.01f)
        assertTrue(front.scale > next.scale)
        assertTrue(next.rotation > 0f)
        assertTrue(previous.rotation < 0f)
    }
}
