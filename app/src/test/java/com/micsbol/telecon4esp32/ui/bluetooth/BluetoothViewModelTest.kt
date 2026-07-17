package com.micsbol.telecon4esp32.ui.bluetooth
import app.cash.turbine.test
import com.micsbol.telecon4esp32.domain.bluetooth.ConnectionResult
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.EspMessage
import com.micsbol.telecon4esp32.domain.bluetooth.ProtocolHandshake
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.rc_settings.SettingsUiState
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationProtocolModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationTransportTypeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetLastApplicationUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetLastDeviceUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetUserSettingsUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveLastApplicationUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveLastDeviceUseCase
import com.micsbol.telecon4esp32.ui.navigation.mainRoute
import com.micsbol.telecon4esp32.util.FakeRemoteController
import com.micsbol.telecon4esp32.util.FakeRemoteDevice
import com.micsbol.telecon4esp32.util.FakeSettingsRepository
import com.micsbol.telecon4esp32.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
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
            getLastApplication = GetLastApplicationUseCase(fakeSettings),
            saveLastApplication = SaveLastApplicationUseCase(fakeSettings),
            getApplicationProtocolMode = GetApplicationProtocolModeUseCase(fakeSettings),
            getApplicationTransportType = GetApplicationTransportTypeUseCase(fakeSettings),
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
      private fun requestControlPanelConnect() {
        viewModel.requestApplicationConnection(
            ApplicationId.CONTROL_PANEL,
            BluetoothProtocolMode.ADVANCED,
        )
    }

  // ── Connection ────────────────────────────────────────────────────────────
    @Test
    fun `connectToDevice sets isConnecting true`() = runTest {
        fakeController.connectionResults = emptyList()
        viewModel.state.test {
            awaitItem()
            requestControlPanelConnect()
            viewModel.connectToDevice(testDevice)
            assertTrue(awaitItem().isConnecting)
            cancelAndIgnoreRemainingEvents()
        }
    }
    @Test
    fun `SocketEstablished with ACK sets isConnected true and active session`() = runTest {
        fakeController.connectionResults = listOf(ConnectionResult.SocketEstablished)
        launch {
            fakeController.emitMessage(
                EspMessage(
                    app = "RC",
                    type = ProtocolHandshake.ACK_TYPE,
                    values = mapOf("app" to "RC"),
                ),
            )
        }
        viewModel.state.test {
            awaitItem()
            requestControlPanelConnect()
            viewModel.connectToDevice(testDevice)
            awaitItem() // connecting
            val connected = awaitItem()
            assertTrue(connected.isConnected)
            assertFalse(connected.isConnecting)
            assertEquals(ApplicationId.CONTROL_PANEL, connected.activeSession?.applicationId)
            cancelAndIgnoreRemainingEvents()
        }
    }
    @Test
    fun `SocketEstablished saves last device after handshake`() = runTest {
        fakeController.connectionResults = listOf(ConnectionResult.SocketEstablished)
        launch {
            fakeController.emitMessage(
                EspMessage(app = "RC", type = ProtocolHandshake.ACK_TYPE, values = mapOf("app" to "RC")),
            )
        }
        requestControlPanelConnect()
        viewModel.connectToDevice(testDevice)
        runCurrent()
        assertNotNull(fakeSettings.savedDevice)
        assertEquals(testDevice.address, fakeSettings.savedDevice?.first)
        assertEquals(testDevice.name, fakeSettings.savedDevice?.second)
    }
    @Test
    fun `Connection Error sets errorMessage and clears connecting state`() = runTest {
        fakeController.connectionResults = listOf(ConnectionResult.Error("Socket refused"))
        viewModel.state.test {
            awaitItem()
            requestControlPanelConnect()
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
        requestControlPanelConnect()
        viewModel.connectToDevice(testDevice)
        viewModel.dismissError()
        assertNull(viewModel.state.value.errorMessage)
    }
    @Test
    fun `disconnectFromDevice resets connection state and calls disconnect`() = runTest {
        fakeController.connectionResults = listOf(ConnectionResult.SocketEstablished)
        launch {
            fakeController.emitMessage(
                EspMessage(app = "RC", type = ProtocolHandshake.ACK_TYPE, values = mapOf("app" to "RC")),
            )
        }
        requestControlPanelConnect()
        viewModel.connectToDevice(testDevice)
        runCurrent()
        viewModel.disconnectFromDevice()
        val state = viewModel.state.value
        assertFalse(state.isConnected)
        assertFalse(state.isConnecting)
        assertTrue(fakeController.disconnectCalled)
    }

    @Test
    fun `unexpected remote link drop clears session for reconnect`() = runTest {
        fakeController.connectionResults = listOf(ConnectionResult.SocketEstablished)
        launch {
            fakeController.emitMessage(
                EspMessage(app = "RC", type = ProtocolHandshake.ACK_TYPE, values = mapOf("app" to "RC")),
            )
        }
        viewModel.state.test {
            awaitItem()
            requestControlPanelConnect()
            viewModel.connectToDevice(testDevice)
            awaitItem() // connecting
            val connected = awaitItem()
            assertTrue(connected.isConnected)
            assertTrue(viewModel.isSessionActiveFor(ApplicationId.CONTROL_PANEL))

            fakeController.setConnected(false)
            val dropped = awaitItem()
            assertFalse(dropped.isConnected)
            assertFalse(dropped.isConnecting)
            assertNull(dropped.activeSession)
            assertFalse(viewModel.isSessionActiveFor(ApplicationId.CONTROL_PANEL))
            cancelAndIgnoreRemainingEvents()
        }
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
            getLastApplication = GetLastApplicationUseCase(fakeSettings),
            saveLastApplication = SaveLastApplicationUseCase(fakeSettings),
            getApplicationProtocolMode = GetApplicationProtocolModeUseCase(fakeSettings),
            getApplicationTransportType = GetApplicationTransportTypeUseCase(fakeSettings),
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

    @Test
    fun `simple protocol sends rc control only when control state changes`() = runTest {
        fakeSettings.setSettings(UserSettings())
        runCurrent()
        fakeSettings.saveProtocolMode(ApplicationId.CONTROL_PANEL, BluetoothProtocolMode.SIMPLE)
        runCurrent()

        val simpleViewModel = BluetoothViewModel(
            remoteController = fakeController,
            getUserSettings = GetUserSettingsUseCase(fakeSettings),
            getLastDevice = GetLastDeviceUseCase(fakeSettings),
            saveLastDevice = SaveLastDeviceUseCase(fakeSettings),
            getLastApplication = GetLastApplicationUseCase(fakeSettings),
            saveLastApplication = SaveLastApplicationUseCase(fakeSettings),
            getApplicationProtocolMode = GetApplicationProtocolModeUseCase(fakeSettings),
            getApplicationTransportType = GetApplicationTransportTypeUseCase(fakeSettings),
        )
        val collectJob = launch { simpleViewModel.userSettings.collect { } }
        runCurrent()

        simpleViewModel.onControlPanelEntered()
        runCurrent()
        assertEquals(1, fakeController.sentLines.size)

        simpleViewModel.onLeftStickChanged(0.5f, 0.5f)
        runCurrent()
        assertEquals(2, fakeController.sentLines.size)

        advanceTimeBy(200)
        runCurrent()
        assertEquals(2, fakeController.sentLines.size)

        simpleViewModel.stopSendingRcData()
        collectJob.cancel()
    }

    // ── Continue session / recent project ─────────────────────────────────────
    @Test
    fun `continueLastSession navigates to applications when no active session`() = runTest {
        var navigated: String? = null
        viewModel.continueLastSession { navigated = it }
        assertEquals(Screen.Applications.route, navigated)
    }

    @Test
    fun `openRecentProject navigates to bluetooth then app after connect`() = runTest {
        var navigated: String? = null
        viewModel.openRecentProject(ApplicationId.GREENHOUSE) { navigated = it }
        runCurrent()
        assertEquals(Screen.Bluetooth.route, navigated)
        assertEquals(ApplicationId.GREENHOUSE, fakeSettings.lastApplicationFlow.first())

        fakeController.connectionResults = listOf(ConnectionResult.SocketEstablished)
        launch {
            fakeController.emitMessage(
                EspMessage(app = "GH", type = ProtocolHandshake.ACK_TYPE, values = mapOf("app" to "GH")),
            )
        }
        viewModel.connectToDevice(testDevice)
        viewModel.navigateToScreen.test {
            assertEquals(ApplicationId.GREENHOUSE.mainRoute(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `connect pops back when preparePostConnectPopBack was called`() = runTest {
        viewModel.preparePostConnectPopBack()
        fakeController.connectionResults = listOf(ConnectionResult.SocketEstablished)
        launch {
            fakeController.emitMessage(
                EspMessage(app = "RC", type = ProtocolHandshake.ACK_TYPE, values = mapOf("app" to "RC")),
            )
        }
        requestControlPanelConnect()
        viewModel.connectToDevice(testDevice)
        viewModel.navigateToScreen.test {
            assertEquals(BluetoothViewModel.POP_BACK_ON_CONNECT, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
