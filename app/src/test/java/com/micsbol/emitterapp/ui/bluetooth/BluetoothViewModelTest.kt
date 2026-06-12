package com.micsbol.emitterapp.ui.bluetooth
import app.cash.turbine.test
import com.micsbol.emitterapp.domain.bluetooth.ConnectionResult
import com.micsbol.emitterapp.domain.model.JoystickMode
import com.micsbol.emitterapp.domain.model.UserSettings
import com.micsbol.emitterapp.ui.rc_settings.SettingsUiState
import com.micsbol.emitterapp.domain.use_case.GetLastDeviceUseCase
import com.micsbol.emitterapp.domain.use_case.GetUserSettingsUseCase
import com.micsbol.emitterapp.domain.use_case.SaveLastDeviceUseCase
import com.micsbol.emitterapp.util.FakeRemoteController
import com.micsbol.emitterapp.util.FakeRemoteDevice
import com.micsbol.emitterapp.util.FakeSettingsRepository
import com.micsbol.emitterapp.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
/**
 * Unit tests for [BluetoothViewModel].
 *
 * Uses hand-written fakes for all dependencies — no Hilt, no Android runtime.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BluetoothViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private lateinit var fakeController: FakeRemoteController
    private lateinit var fakeSettings: FakeSettingsRepository
    private lateinit var viewModel: BluetoothViewModel
    private val testDevice = FakeRemoteDevice(name = "TestESP", address = "AA:BB:CC:DD:EE:FF")
    @Before
    fun setUp() {
        fakeController = FakeRemoteController()
        fakeSettings = FakeSettingsRepository()
        viewModel = BluetoothViewModel(
            remoteController = fakeController,
            getUserSettings  = GetUserSettingsUseCase(fakeSettings),
            getLastDevice    = GetLastDeviceUseCase(fakeSettings),
            saveLastDevice   = SaveLastDeviceUseCase(fakeSettings),
        )
    }
    // ── Initial state ─────────────────────────────────────────────────────────
    @Test
    fun `initial state is not connected and not scanning`() = runTest {
        val state = viewModel.state.value
        assertFalse(state.isConnected)
        assertFalse(state.isConnecting)
        assertFalse(state.isScanning)
        assertNull(state.errorMessage)
    }
    @Test
    fun `initial state has empty device lists`() = runTest {
        val state = viewModel.state.value
        assertTrue(state.scannedDevices.isEmpty())
        assertTrue(state.pairedDevices.isEmpty())
    }
    // ── Discovery ─────────────────────────────────────────────────────────────
    @Test
    fun `startScan sets isScanning true and calls startDiscovery`() = runTest {
        viewModel.state.test {
            awaitItem()
            viewModel.startScan()
            assertTrue(awaitItem().isScanning)
            assertTrue(fakeController.discoveryStarted)
            cancelAndIgnoreRemainingEvents()
        }
    }
    @Test
    fun `stopScan sets isScanning false and calls stopDiscovery`() = runTest {
        viewModel.startScan()
        viewModel.stopScan()
        assertFalse(viewModel.state.value.isScanning)
        assertTrue(fakeController.discoveryStopped)
    }
    @Test
    fun `scanned devices from controller appear in state`() = runTest {
        fakeController.setDiscoveredDevices(listOf(testDevice))
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.scannedDevices.contains(testDevice))
            cancelAndIgnoreRemainingEvents()
        }
    }
    @Test
    fun `paired devices from controller appear in state`() = runTest {
        fakeController.setSavedDevices(listOf(testDevice))
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.pairedDevices.contains(testDevice))
            cancelAndIgnoreRemainingEvents()
        }
    }
    // ── Connection ────────────────────────────────────────────────────────────
    @Test
    fun `connectToDevice sets isConnecting true`() = runTest {
        // No results means connection stays in connecting state
        fakeController.connectionResults = emptyList()
        viewModel.state.test {
            awaitItem()
            viewModel.connectToDevice(testDevice)
            assertTrue(awaitItem().isConnecting)
            cancelAndIgnoreRemainingEvents()
        }
    }
    @Test
    fun `ConnectionEstablished sets isConnected true and clears error`() = runTest {
        fakeController.connectionResults = listOf(ConnectionResult.ConnectionEstablished)
        viewModel.state.test {
            awaitItem()
            viewModel.connectToDevice(testDevice)
            awaitItem() // connecting
            val connected = awaitItem()
            assertTrue(connected.isConnected)
            assertFalse(connected.isConnecting)
            assertNull(connected.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }
    @Test
    fun `ConnectionEstablished saves last device in repository`() = runTest {
        fakeController.connectionResults = listOf(ConnectionResult.ConnectionEstablished)
        viewModel.connectToDevice(testDevice)
        assertNotNull(fakeSettings.savedDevice)
        assertEquals(testDevice.address, fakeSettings.savedDevice?.first)
        assertEquals(testDevice.name, fakeSettings.savedDevice?.second)
    }
    @Test
    fun `Connection Error sets errorMessage and clears connecting state`() = runTest {
        fakeController.connectionResults = listOf(ConnectionResult.Error("Socket refused"))
        viewModel.state.test {
            awaitItem()
            viewModel.connectToDevice(testDevice)
            awaitItem() // connecting
            val failed = awaitItem()
            assertFalse(failed.isConnected)
            assertFalse(failed.isConnecting)
            assertEquals("Socket refused", failed.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }
    @Test
    fun `dismissError clears errorMessage`() = runTest {
        fakeController.connectionResults = listOf(ConnectionResult.Error("oops"))
        viewModel.connectToDevice(testDevice)
        viewModel.dismissError()
        assertNull(viewModel.state.value.errorMessage)
    }
    @Test
    fun `disconnectFromDevice resets connection state and calls disconnect`() = runTest {
        fakeController.connectionResults = listOf(ConnectionResult.ConnectionEstablished)
        viewModel.connectToDevice(testDevice)
        viewModel.disconnectFromDevice()
        val state = viewModel.state.value
        assertFalse(state.isConnected)
        assertFalse(state.isConnecting)
        assertTrue(fakeController.disconnectCalled)
    }
    // ── RC control state ──────────────────────────────────────────────────────
    @Test
    fun `onLeftStickChanged updates left stick position`() = runTest {
        viewModel.onLeftStickChanged(0.5f, -0.3f)
        assertEquals(Pair(0.5f, -0.3f), viewModel.rcControlState.value.leftStickPosition)
    }
    @Test
    fun `onRightStickChanged updates right stick position`() = runTest {
        viewModel.onRightStickChanged(-1f, 1f)
        assertEquals(Pair(-1f, 1f), viewModel.rcControlState.value.rightStickPosition)
    }
    @Test
    fun `onLeftSwitchChanged updates the correct switch in left list`() = runTest {
        viewModel.onLeftSwitchChanged(index = 1, newState = true)
        val switches = viewModel.rcControlState.value.leftSwitches
        assertFalse(switches[0])
        assertTrue(switches[1])
        assertFalse(switches[2])
    }
    @Test
    fun `onRightSwitchChanged updates the correct switch in right list`() = runTest {
        viewModel.onRightSwitchChanged(index = 2, newState = true)
        val switches = viewModel.rcControlState.value.rightSwitches
        assertFalse(switches[0])
        assertFalse(switches[1])
        assertTrue(switches[2])
    }
    @Test
    fun `onLeftKnobChanged updates left knob value`() = runTest {
        viewModel.onLeftKnobChanged(0.8f)
        assertEquals(0.8f, viewModel.rcControlState.value.leftKnobValue)
    }
    @Test
    fun `onRightKnobChanged updates right knob value`() = runTest {
        viewModel.onRightKnobChanged(0.1f)
        assertEquals(0.1f, viewModel.rcControlState.value.rightKnobValue)
    }

    @Test
    fun `init applies persisted settings on cold start`() = runTest {
        fakeSettings.setSettings(
            UserSettings(
                leftStickMode = JoystickMode.Hold(JoystickMode.LEFT),
                leftKnobInitialValue = 0.75f,
                switchInitialStates = (0..5).associateWith { false }
            )
        )
        val coldStartViewModel = BluetoothViewModel(
            remoteController = fakeController,
            getUserSettings = GetUserSettingsUseCase(fakeSettings),
            getLastDevice = GetLastDeviceUseCase(fakeSettings),
            saveLastDevice = SaveLastDeviceUseCase(fakeSettings),
        )
        val collectJob = launch { coldStartViewModel.userSettings.collect { } }

        coldStartViewModel.userSettings
            .filterIsInstance<SettingsUiState.Success>()
            .first { it.settings.leftKnobInitialValue == 0.75f }

        val rc = coldStartViewModel.rcControlState.value
        assertEquals(-1f, rc.leftStickPosition.first, 0.001f)
        assertEquals(0.75f, rc.leftKnobValue)

        collectJob.cancel()
    }

    @Test
    fun `onControlPanelEntered applies settings only when they changed`() = runTest {
        val collectJob = launch { viewModel.userSettings.collect { } }

        viewModel.onLeftStickChanged(0.9f, 0.9f)
        viewModel.onLeftKnobChanged(0.1f)
        viewModel.onLeftSwitchChanged(0, true)

        fakeSettings.setSettings(
            UserSettings(
                leftStickMode = JoystickMode.Hold(JoystickMode.LEFT),
                leftKnobInitialValue = 0.75f,
                switchInitialStates = (0..5).associateWith { false }
            )
        )
        viewModel.userSettings
            .filterIsInstance<SettingsUiState.Success>()
            .first { it.settings.leftKnobInitialValue == 0.75f }

        viewModel.onControlPanelEntered()

        val afterApply = viewModel.rcControlState.value
        assertEquals(-1f, afterApply.leftStickPosition.first, 0.001f)
        assertEquals(0f, afterApply.leftStickPosition.second, 0.001f)
        assertEquals(0.75f, afterApply.leftKnobValue)
        assertFalse(afterApply.leftSwitches[0])

        viewModel.onLeftStickChanged(0.2f, 0.3f)
        viewModel.onControlPanelEntered()

        val afterReturn = viewModel.rcControlState.value
        assertEquals(0.2f, afterReturn.leftStickPosition.first, 0.001f)
        assertEquals(0.3f, afterReturn.leftStickPosition.second, 0.001f)

        viewModel.stopSendingRcData()
        collectJob.cancel()
    }

    // ── Quick connect ─────────────────────────────────────────────────────────
    @Test
    fun `quickConnect navigates to bluetooth screen when no last device is saved`() = runTest {
        viewModel.navigateToScreen.test {
            viewModel.quickConnect()
            assertEquals("bluetooth", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
    @Test
    fun `quickConnect navigates to bluetooth screen when last device is not in paired list`() = runTest {
        fakeSettings.setLastDevice("DE:AD:BE:EF:00:01", "GhostDevice")
        // saved devices list is empty — address won't be found
        viewModel.navigateToScreen.test {
            viewModel.quickConnect()
            assertEquals("bluetooth", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
    @Test
    fun `quickConnect connects directly when last device is in paired list`() = runTest {
        fakeSettings.setLastDevice(testDevice.address, testDevice.name)
        fakeController.setSavedDevices(listOf(testDevice))
        fakeController.connectionResults = listOf(ConnectionResult.ConnectionEstablished)
        viewModel.quickConnect()
        // After successful connect the nav event for home is emitted
        viewModel.navigateToScreen.test {
            assertEquals("home", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
