package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RcVehicleProTutorialViewModelTest {

    @Test
    fun startOpensOnTheCameraStep() {
        val viewModel = RcVehicleProTutorialViewModel()

        viewModel.start()

        assertTrue(viewModel.active.value)
        assertEquals(RcVehicleProTutorialStep.CAMERA, viewModel.step.value)
    }

    @Test
    fun stickMenuAdvancesOnlyDuringStickSetup() {
        val viewModel = RcVehicleProTutorialViewModel()
        viewModel.start()
        viewModel.show(RcVehicleProTutorialStep.THROTTLE)

        viewModel.onStickOptionsOpened()
        assertEquals(RcVehicleProTutorialStep.THROTTLE, viewModel.step.value)

        viewModel.show(RcVehicleProTutorialStep.STICK_SETUP)
        viewModel.onStickOptionsOpened()
        assertEquals(RcVehicleProTutorialStep.TRIM, viewModel.step.value)
    }

    @Test
    fun dismissHidesTheTour() {
        val viewModel = RcVehicleProTutorialViewModel()
        viewModel.start()

        viewModel.dismiss()

        assertFalse(viewModel.active.value)
    }
}
