package com.micsbol.telecon4esp32.ui.bluetooth
import app.cash.turbine.test
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectFailure
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.ConnectionResult
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.Esp32SoftApDevice
import com.micsbol.telecon4esp32.domain.bluetooth.EspMessage
import com.micsbol.telecon4esp32.domain.bluetooth.HandshakeFailure
import com.micsbol.telecon4esp32.domain.bluetooth.ProtocolHandshake
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.model.PlotCalibration
import com.micsbol.telecon4esp32.domain.model.TelemetryChannel
import com.micsbol.telecon4esp32.domain.model.TelemetrySink
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.rc_settings.SettingsUiState
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationBoardUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationConnectionModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationProtocolModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationTransportTypeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetLastApplicationUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetLastDeviceUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetSoftApPerformancePresetUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetUserSettingsUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveLastApplicationUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveLastDeviceUseCase
import com.micsbol.telecon4esp32.ui.navigation.mainRoute
import com.micsbol.telecon4esp32.util.FakeRemoteController
import com.micsbol.telecon4esp32.util.FakeRemoteDevice
import com.micsbol.telecon4esp32.util.FakeSessionCsvRepository
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
    private lateinit var fakeSessionCsv: FakeSessionCsvRepository
    private lateinit var viewModel: BluetoothViewModel
    private val testDevice = FakeRemoteDevice(name = "TestESP", address = "AA:BB:CC:DD:EE:FF")
    @Before
    fun setUp() {
        fakeController = FakeRemoteController()
        fakeSettings = FakeSettingsRepository()
        fakeSessionCsv = FakeSessionCsvRepository()
        viewModel = BluetoothViewModel(
            remoteController = fakeController,
            getUserSettings  = GetUserSettingsUseCase(fakeSettings),
            getLastDevice    = GetLastDeviceUseCase(fakeSettings),
            saveLastDevice   = SaveLastDeviceUseCase(fakeSettings),
            getLastApplication = GetLastApplicationUseCase(fakeSettings),
            saveLastApplication = SaveLastApplicationUseCase(fakeSettings),
            getApplicationProtocolMode = GetApplicationProtocolModeUseCase(fakeSettings),
            getApplicationTransportType = GetApplicationTransportTypeUseCase(fakeSettings),
            getApplicationConnectionMode = GetApplicationConnectionModeUseCase(fakeSettings),
            getApplicationBoard = GetApplicationBoardUseCase(fakeSettings),
            getSoftApPerformancePreset = GetSoftApPerformancePresetUseCase(fakeSettings),
            sessionCsvRepository = fakeSessionCsv,
            saveSettings = fakeSettings.toSaveSettingsUseCases(),
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
    fun `Control Panel CONNECT uses binary proto for advanced mode`() = runTest {
        fakeController.connectionResults = listOf(ConnectionResult.SocketEstablished)
        launch {
            fakeController.emitMessage(
                EspMessage(app = "RC", type = ProtocolHandshake.ACK_TYPE, values = mapOf("app" to "RC")),
            )
        }
        requestControlPanelConnect()
        viewModel.connectToDevice(testDevice)
        runCurrent()
        assertEquals("RC:CONNECT,proto,binary", fakeController.sentLines.first())
    }

    @Test
    fun `Control Panel CONNECT uses simple proto for simple mode`() = runTest {
        fakeController.connectionResults = listOf(ConnectionResult.SocketEstablished)
        launch {
            fakeController.emitMessage(
                EspMessage(app = "RC", type = ProtocolHandshake.ACK_TYPE, values = mapOf("app" to "RC")),
            )
        }
        viewModel.requestApplicationConnection(
            ApplicationId.CONTROL_PANEL,
            BluetoothProtocolMode.SIMPLE,
        )
        viewModel.connectToDevice(testDevice)
        runCurrent()
        assertEquals("RC:CONNECT,proto,simple", fakeController.sentLines.first())
    }

    @Test
    fun `proto_mismatch NAK sets handshakeFailure and does not connect`() = runTest {
        fakeController.connectionResults = listOf(ConnectionResult.SocketEstablished)
        launch {
            fakeController.emitMessage(
                EspMessage(
                    app = "RC",
                    type = ProtocolHandshake.NAK_TYPE,
                    values = mapOf(
                        "reason" to "proto_mismatch",
                        "expected" to "simple",
                        "actual" to "binary",
                    ),
                ),
            )
        }
        viewModel.state.test {
            awaitItem()
            requestControlPanelConnect()
            viewModel.connectToDevice(testDevice)
            awaitItem() // connecting
            val failed = awaitItem()
            assertFalse(failed.isConnected)
            assertFalse(failed.isConnecting)
            assertNull(failed.activeSession)
            val failure = failed.handshakeFailure
            assertTrue(failure is HandshakeFailure.ProtocolMismatch)
            val mismatch = failure as HandshakeFailure.ProtocolMismatch
            assertEquals("simple", mismatch.device)
            assertEquals("binary", mismatch.requested)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `handshake timeout sets Timeout failure`() = runTest {
        fakeController.connectionResults = listOf(ConnectionResult.SocketEstablished)
        viewModel.state.test {
            awaitItem()
            requestControlPanelConnect()
            viewModel.connectToDevice(testDevice)
            awaitItem() // connecting
            advanceTimeBy(3_000)
            runCurrent()
            val failed = awaitItem()
            assertFalse(failed.isConnected)
            assertEquals(HandshakeFailure.Timeout, failed.handshakeFailure)
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
    fun `BLE link Error sets BleLinkFailed`() = runTest {
        fakeController.connectionResults = listOf(ConnectionResult.Error("BLE connection failed"))
        viewModel.state.test {
            awaitItem()
            viewModel.requestApplicationConnection(
                ApplicationId.CONTROL_PANEL,
                BluetoothProtocolMode.ADVANCED,
                BluetoothTransportType.BLE,
            )
            viewModel.connectToDevice(testDevice)
            awaitItem() // connecting
            val failed = awaitItem()
            assertFalse(failed.isConnected)
            assertEquals(BluetoothTransportType.BLE, fakeController.lastConnectTransport)
            val failure = failed.connectFailure
            assertTrue(
                "expected BleLinkFailed, got $failure",
                failure is BluetoothConnectFailure.BleLinkFailed,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WiFi SoftAP CONNECT uses wifi proto and SoftAp device`() = runTest {
        fakeController.connectionResults = listOf(ConnectionResult.SocketEstablished)
        launch {
            fakeController.emitMessage(
                EspMessage(app = "RC", type = ProtocolHandshake.ACK_TYPE, values = mapOf("app" to "RC")),
            )
        }
        viewModel.state.test {
            awaitItem()
            viewModel.requestApplicationConnection(
                applicationId = ApplicationId.RC_VEHICLE_PRO,
                protocolMode = BluetoothProtocolMode.ADVANCED,
                transport = BluetoothTransportType.WIFI,
                connectionMode = BluetoothConnectionMode.WIFI_BINARY,
            )
            viewModel.connectToWifiSoftAp()
            awaitItem() // connecting
            val connected = awaitItem()
            assertTrue(connected.isConnected)
            assertEquals(ApplicationId.RC_VEHICLE_PRO, connected.activeSession?.applicationId)
            assertEquals(BluetoothTransportType.WIFI, fakeController.lastConnectTransport)
            assertEquals(Esp32SoftApDevice.Default.address, fakeController.lastConnectDevice?.address)
            assertEquals("RC:CONNECT,proto,binary", fakeController.sentLines.first())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WiFi SoftAP link Error sets WifiSoftApLinkFailed`() = runTest {
        fakeController.connectionResults = listOf(ConnectionResult.Error("SoftAP unreachable"))
        viewModel.state.test {
            awaitItem()
            viewModel.requestApplicationConnection(
                applicationId = ApplicationId.RC_VEHICLE_PRO,
                protocolMode = BluetoothProtocolMode.ADVANCED,
                transport = BluetoothTransportType.WIFI,
                connectionMode = BluetoothConnectionMode.WIFI_BINARY,
            )
            viewModel.connectToWifiSoftAp()
            awaitItem() // connecting
            val failed = awaitItem()
            assertFalse(failed.isConnected)
            val failure = failed.connectFailure
            assertTrue(
                "expected WifiSoftApLinkFailed, got $failure",
                failure is BluetoothConnectFailure.WifiSoftApLinkFailed,
            )
            assertEquals(
                BluetoothConnectionMode.WIFI_BINARY,
                (failure as BluetoothConnectFailure.WifiSoftApLinkFailed).connectionMode,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Connection Error sets connectFailure and clears connecting state`() = runTest {
        fakeController.connectionResults = listOf(ConnectionResult.Error("Socket refused"))
        viewModel.state.test {
            awaitItem()
            requestControlPanelConnect()
            viewModel.connectToDevice(testDevice)
            awaitItem() // connecting
            val failed = awaitItem()
            assertFalse(failed.isConnected)
            assertFalse(failed.isConnecting)
            val failure = failed.connectFailure
            assertTrue(failure is BluetoothConnectFailure.ClassicLinkFailed)
            assertEquals("Socket refused", (failure as BluetoothConnectFailure.ClassicLinkFailed).technicalDetail)
            cancelAndIgnoreRemainingEvents()
        }
    }
    @Test
    fun `dismissError clears connectFailure`() = runTest {
        fakeController.connectionResults = listOf(ConnectionResult.Error("oops"))
        requestControlPanelConnect()
        viewModel.connectToDevice(testDevice)
        viewModel.dismissError()
        assertNull(viewModel.state.value.connectFailure)
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
    fun `init does not apply persisted stick knob or switch initials`() = runTest {
        fakeSettings.setSettings(
            UserSettings(
                leftStickMode = JoystickMode.Hold(JoystickMode.LEFT),
                leftKnobInitialValue = 0.75f,
                switchInitialStates = (0..5).associateWith { true }
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
            getApplicationConnectionMode = GetApplicationConnectionModeUseCase(fakeSettings),
            getApplicationBoard = GetApplicationBoardUseCase(fakeSettings),
            getSoftApPerformancePreset = GetSoftApPerformancePresetUseCase(fakeSettings),
            sessionCsvRepository = FakeSessionCsvRepository(),
            saveSettings = fakeSettings.toSaveSettingsUseCases(),
        )
        val collectJob = launch { coldStartViewModel.userSettings.collect { } }

        coldStartViewModel.userSettings
            .filterIsInstance<SettingsUiState.Success>()
            .first { it.settings.leftKnobInitialValue == 0.75f }

        val rc = coldStartViewModel.rcControlState.value
        assertEquals(0f, rc.leftStickPosition.first, 0.001f)
        assertEquals(0f, rc.leftStickPosition.second, 0.001f)
        assertEquals(0f, rc.leftKnobValue)
        assertFalse(rc.leftSwitches[0])

        collectJob.cancel()
    }

    @Test
    fun `onControlPanelEntered keeps live controls instead of persisted initials`() = runTest {
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
        assertEquals(0.9f, afterApply.leftStickPosition.first, 0.001f)
        assertEquals(0.9f, afterApply.leftStickPosition.second, 0.001f)
        assertEquals(0.1f, afterApply.leftKnobValue)
        assertTrue(afterApply.leftSwitches[0])

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
            getApplicationConnectionMode = GetApplicationConnectionModeUseCase(fakeSettings),
            getApplicationBoard = GetApplicationBoardUseCase(fakeSettings),
            getSoftApPerformancePreset = GetSoftApPerformancePresetUseCase(fakeSettings),
            sessionCsvRepository = FakeSessionCsvRepository(),
            saveSettings = fakeSettings.toSaveSettingsUseCases(),
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
    fun `continueLastSession navigates to modules hub when no active session`() = runTest {
        var navigated: String? = null
        viewModel.continueLastSession { navigated = it }
        assertEquals(Screen.Home.route, navigated)
    }

    @Test
    fun `openRecentProject navigates to application screen`() = runTest {
        var navigated: String? = null
        viewModel.openRecentProject(ApplicationId.CONTROL_PANEL) { navigated = it }
        runCurrent()
        assertEquals(ApplicationId.CONTROL_PANEL.mainRoute(), navigated)
        assertEquals(ApplicationId.CONTROL_PANEL, fakeSettings.lastApplicationFlow.first())
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

    @Test
    fun `RC Vehicle Pro SIMPLE session sends CTRL lines even if Control Panel is ADVANCED`() = runTest {
        fakeSettings.saveProtocolMode(ApplicationId.CONTROL_PANEL, BluetoothProtocolMode.ADVANCED)
        fakeSettings.saveProtocolMode(ApplicationId.RC_VEHICLE_PRO, BluetoothProtocolMode.SIMPLE)
        runCurrent()

        fakeController.connectionResults = listOf(ConnectionResult.SocketEstablished)
        viewModel.state.test {
            awaitItem()
            viewModel.requestApplicationConnection(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothProtocolMode.SIMPLE,
                BluetoothTransportType.CLASSIC,
            )
            launch {
                fakeController.emitMessage(
                    EspMessage(
                        app = "RC",
                        type = ProtocolHandshake.ACK_TYPE,
                        values = mapOf("app" to "RC"),
                    ),
                )
            }
            viewModel.connectToDevice(testDevice)
            awaitItem() // connecting
            val connected = awaitItem()
            assertTrue(connected.isConnected)
            assertEquals(ApplicationId.RC_VEHICLE_PRO, connected.activeSession?.applicationId)
            assertEquals(BluetoothProtocolMode.SIMPLE, connected.activeSession?.protocolMode)
            cancelAndIgnoreRemainingEvents()
        }

        fakeController.sentLines.clear()
        fakeController.sentData.clear()

        viewModel.onControlPanelEntered()
        runCurrent()
        assertTrue(
            "expected RC:CTRL lines, got ${fakeController.sentLines}",
            fakeController.sentLines.any { it.startsWith("RC:CTRL,") },
        )
        assertTrue(
            "ADVANCED binary must not be used for SIMPLE vehicle session: ${fakeController.sentData.size}",
            fakeController.sentData.isEmpty(),
        )

        val linesBeforeMove = fakeController.sentLines.size
        viewModel.onLeftStickChanged(0.4f, 0.6f)
        runCurrent()
        assertEquals(linesBeforeMove + 1, fakeController.sentLines.size)
        assertTrue(fakeController.sentLines.last().startsWith("RC:CTRL,"))
        assertTrue(fakeController.sentData.isEmpty())

        viewModel.stopSendingRcData()
    }

    @Test
    fun `ensureWifiSoftApConnected is no-op when already connected`() = runTest {
        fakeController.connectionResults = listOf(ConnectionResult.SocketEstablished)
        launch {
            fakeController.emitMessage(
                EspMessage(app = "RC", type = ProtocolHandshake.ACK_TYPE, values = mapOf("app" to "RC")),
            )
        }
        viewModel.state.test {
            awaitItem()
            viewModel.requestApplicationConnection(
                applicationId = ApplicationId.RC_VEHICLE_PRO,
                protocolMode = BluetoothProtocolMode.ADVANCED,
                transport = BluetoothTransportType.WIFI,
                connectionMode = BluetoothConnectionMode.WIFI_BINARY,
            )
            viewModel.connectToWifiSoftAp()
            awaitItem() // connecting
            val connected = awaitItem()
            assertTrue(connected.isConnected)
            cancelAndIgnoreRemainingEvents()
        }

        val connectCountBefore = fakeController.sentLines.count { it.startsWith("RC:CONNECT") }
        viewModel.ensureWifiSoftApConnected(ApplicationId.RC_VEHICLE_PRO)
        runCurrent()
        val connectCountAfter = fakeController.sentLines.count { it.startsWith("RC:CONNECT") }
        assertEquals(connectCountBefore, connectCountAfter)
    }

    @Test
    fun `CAM SoftAP Binary session sends binary frames not text CTRL`() = runTest {
        fakeSettings.saveProtocolMode(ApplicationId.CONTROL_PANEL, BluetoothProtocolMode.ADVANCED)
        fakeSettings.saveProtocolMode(ApplicationId.RC_VEHICLE_PRO, BluetoothProtocolMode.ADVANCED)
        fakeSettings.saveTransportType(ApplicationId.RC_VEHICLE_PRO, BluetoothTransportType.WIFI)
        fakeSettings.saveConnectionMode(ApplicationId.RC_VEHICLE_PRO, BluetoothConnectionMode.WIFI_BINARY)
        fakeSettings.saveBoard(ApplicationId.RC_VEHICLE_PRO, com.micsbol.telecon4esp32.domain.model.Esp32Board.CAM)
        runCurrent()

        fakeController.connectionResults = listOf(ConnectionResult.SocketEstablished)
        viewModel.state.test {
            awaitItem()
            viewModel.requestApplicationConnection(
                applicationId = ApplicationId.RC_VEHICLE_PRO,
                protocolMode = BluetoothProtocolMode.ADVANCED,
                transport = BluetoothTransportType.WIFI,
                connectionMode = BluetoothConnectionMode.WIFI_BINARY,
            )
            launch {
                fakeController.emitMessage(
                    EspMessage(
                        app = "RC",
                        type = ProtocolHandshake.ACK_TYPE,
                        values = mapOf("app" to "RC"),
                    ),
                )
            }
            viewModel.connectToWifiSoftAp()
            awaitItem() // connecting
            val connected = awaitItem()
            assertTrue(connected.isConnected)
            assertEquals(BluetoothProtocolMode.ADVANCED, connected.activeSession?.protocolMode)
            assertEquals(BluetoothTransportType.WIFI, connected.activeSession?.transport)
            assertEquals(
                BluetoothConnectionMode.WIFI_BINARY,
                connected.activeSession?.connectionMode,
            )
            assertEquals("RC:CONNECT,proto,binary", fakeController.sentLines.first())
            cancelAndIgnoreRemainingEvents()
        }

        fakeController.sentLines.clear()
        fakeController.sentData.clear()

        viewModel.onControlPanelEntered()
        advanceTimeBy(100)
        runCurrent()
        assertTrue(
            "expected binary RC frames, got lines=${fakeController.sentLines} data=${fakeController.sentData.size}",
            fakeController.sentData.isNotEmpty(),
        )
        assertTrue(
            "binary SoftAP must not send RC:CTRL text: ${fakeController.sentLines}",
            fakeController.sentLines.none { it.startsWith("RC:CTRL,") },
        )

        viewModel.stopSendingRcData()
    }

    @Test
    fun `DevKit WIFI_BINARY session sends binary frames not text CTRL`() = runTest {
        fakeSettings.saveProtocolMode(ApplicationId.CONTROL_PANEL, BluetoothProtocolMode.ADVANCED)
        fakeSettings.saveTransportType(ApplicationId.CONTROL_PANEL, BluetoothTransportType.WIFI)
        runCurrent()

        fakeController.connectionResults = listOf(ConnectionResult.SocketEstablished)
        viewModel.state.test {
            awaitItem()
            viewModel.requestApplicationConnection(
                applicationId = ApplicationId.CONTROL_PANEL,
                protocolMode = BluetoothProtocolMode.ADVANCED,
                transport = BluetoothTransportType.WIFI,
                connectionMode = BluetoothConnectionMode.WIFI_BINARY,
            )
            launch {
                fakeController.emitMessage(
                    EspMessage(
                        app = "RC",
                        type = ProtocolHandshake.ACK_TYPE,
                        values = mapOf("app" to "RC"),
                    ),
                )
            }
            viewModel.connectToWifiSoftAp()
            awaitItem() // connecting
            val connected = awaitItem()
            assertTrue(connected.isConnected)
            assertEquals(BluetoothProtocolMode.ADVANCED, connected.activeSession?.protocolMode)
            assertEquals(
                BluetoothConnectionMode.WIFI_BINARY,
                connected.activeSession?.connectionMode,
            )
            assertEquals("RC:CONNECT,proto,binary", fakeController.sentLines.first())
            assertEquals(
                Esp32SoftApDevice.forConnectionMode(
                    BluetoothConnectionMode.WIFI_BINARY,
                    ApplicationId.CONTROL_PANEL,
                ).name,
                fakeController.lastConnectDevice?.name,
            )
            cancelAndIgnoreRemainingEvents()
        }

        fakeController.sentLines.clear()
        fakeController.sentData.clear()

        viewModel.onControlPanelEntered()
        advanceTimeBy(100)
        runCurrent()
        assertTrue(
            "expected AA 55 binary frames, got lines=${fakeController.sentLines} data=${fakeController.sentData.size}",
            fakeController.sentData.isNotEmpty(),
        )
        assertTrue(
            "text CTRL must not go over WIFI_BINARY SoftAP: ${fakeController.sentLines}",
            fakeController.sentLines.none { it.startsWith("RC:CTRL,") },
        )
        val frame = fakeController.sentData.first()
        assertEquals(0xAA.toByte(), frame[0])
        assertEquals(0x55.toByte(), frame[1])

        viewModel.stopSendingRcData()
    }

    @Test
    fun `DevKit WIFI_SIMPLE CONNECT uses simple proto`() = runTest {
        fakeController.connectionResults = listOf(ConnectionResult.SocketEstablished)
        launch {
            fakeController.emitMessage(
                EspMessage(app = "RC", type = ProtocolHandshake.ACK_TYPE, values = mapOf("app" to "RC")),
            )
        }
        viewModel.state.test {
            awaitItem()
            viewModel.requestApplicationConnection(
                applicationId = ApplicationId.CONTROL_PANEL,
                protocolMode = BluetoothProtocolMode.SIMPLE,
                transport = BluetoothTransportType.WIFI,
                connectionMode = BluetoothConnectionMode.WIFI_SIMPLE,
            )
            viewModel.connectToWifiSoftAp()
            awaitItem()
            val connected = awaitItem()
            assertTrue(connected.isConnected)
            assertEquals("RC:CONNECT,proto,simple", fakeController.sentLines.first())
            assertEquals(
                "ESP32-TC-RC-WiFi-Simple",
                fakeController.lastConnectDevice?.name,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveTelemetryWidgetConfig updates analog label and channel`() = runTest {
        val indicatorJob = launch { viewModel.rcLeftIndicator.collect { } }
        val routingJob = launch { viewModel.rcChannelRouting.collect { } }
        viewModel.onControlPanelEntered()
        runCurrent()

        viewModel.saveTelemetryWidgetConfig(
            TelemetrySink.ANALOG_GAUGE,
            TelemetryChannel.CH_5,
            "  speed  ",
        )
        runCurrent()

        assertEquals("speed", viewModel.rcLeftIndicator.value.title)
        assertEquals(
            TelemetryChannel.CH_5,
            viewModel.rcChannelRouting.value.sourceFor(TelemetrySink.ANALOG_GAUGE),
        )
        val settings = fakeSettings.settingsFlow.first()
        assertEquals("speed", settings.analogIndicatorUnit)
        assertEquals(
            TelemetryChannel.CH_5,
            settings.channelRouting.sourceFor(TelemetrySink.ANALOG_GAUGE),
        )
        indicatorJob.cancel()
        routingJob.cancel()
    }

    @Test
    fun `saveTelemetryWidgetConfig updates numeric panel and battery labels`() = runTest {
        val leftJob = launch { viewModel.rcLeftSideTelemetry.collect { } }
        val rightJob = launch { viewModel.rcRightSideTelemetry.collect { } }
        val batteryJob = launch { viewModel.rcRightIndicator.collect { } }
        viewModel.onControlPanelEntered()
        runCurrent()

        viewModel.saveTelemetryWidgetConfig(
            TelemetrySink.PANEL_LEFT,
            TelemetryChannel.PANEL_RIGHT,
            "meters",
        )
        viewModel.saveTelemetryWidgetConfig(
            TelemetrySink.BATTERY_GAUGE,
            TelemetryChannel.CH_8,
            "battery",
        )
        runCurrent()

        assertEquals("meters", viewModel.rcLeftSideTelemetry.value.panelTitle)
        assertEquals(
            TelemetryChannel.PANEL_RIGHT,
            viewModel.rcChannelRouting.value.sourceFor(TelemetrySink.PANEL_LEFT),
        )
        assertEquals("battery", viewModel.rcRightIndicator.value.title)
        assertEquals(
            TelemetryChannel.CH_8,
            viewModel.rcChannelRouting.value.sourceFor(TelemetrySink.BATTERY_GAUGE),
        )
        leftJob.cancel()
        rightJob.cancel()
        batteryJob.cancel()
    }

    @Test
    fun `savePlotLabel calibration and channel update HUD plot settings`() = runTest {
        val labelsJob = launch { viewModel.rcPlotDisplayLabels.collect { } }
        val calibrationJob = launch { viewModel.rcPlotCalibrations.collect { } }
        val routingJob = launch { viewModel.rcChannelRouting.collect { } }
        viewModel.onControlPanelEntered()
        runCurrent()

        viewModel.savePlotLabel(0, "  current  ")
        viewModel.savePlotCalibration(
            0,
            PlotCalibration(offset = 1f, span = 10f, unit = "A"),
        )
        viewModel.saveChannelBinding(TelemetrySink.PLOT_0, TelemetryChannel.CH_8)
        runCurrent()

        assertEquals("current", viewModel.rcPlotDisplayLabels.value[0])
        assertEquals(
            PlotCalibration(offset = 1f, span = 10f, unit = "A"),
            viewModel.rcPlotCalibrations.value[0],
        )
        assertEquals(
            TelemetryChannel.CH_8,
            viewModel.rcChannelRouting.value.sourceFor(TelemetrySink.PLOT_0),
        )
        val settings = fakeSettings.settingsFlow.first()
        assertEquals("current", settings.plotLabels[0])
        assertEquals(
            PlotCalibration(offset = 1f, span = 10f, unit = "A"),
            settings.plotCalibrations[0],
        )
        assertEquals(
            TelemetryChannel.CH_8,
            settings.channelRouting.sourceFor(TelemetrySink.PLOT_0),
        )
        labelsJob.cancel()
        calibrationJob.cancel()
        routingJob.cancel()
    }

    @Test
    fun `saveStickMode persists left and right joystick modes`() = runTest {
        viewModel.saveStickMode(
            isRightStick = false,
            mode = JoystickMode.VerticalHold(JoystickMode.DOWN),
        )
        viewModel.saveStickMode(
            isRightStick = true,
            mode = JoystickMode.HorizontalSpring(JoystickMode.LEFT),
        )
        runCurrent()

        val settings = fakeSettings.settingsFlow.first()
        assertEquals(
            JoystickMode.VerticalHold(JoystickMode.DOWN),
            settings.leftStickMode,
        )
        assertEquals(
            JoystickMode.HorizontalSpring(JoystickMode.LEFT),
            settings.rightStickMode,
        )
        val rc = viewModel.rcControlState.value
        assertEquals(0f, rc.leftStickPosition.first, 0.001f)
        assertEquals(-1f, rc.leftStickPosition.second, 0.001f)
        assertEquals(-1f, rc.rightStickPosition.first, 0.001f)
        assertEquals(0f, rc.rightStickPosition.second, 0.001f)
    }

    @Test
    fun `resetLiveRcControlsToZero applies stick rest and zeros switches knobs`() = runTest {
        val collectJob = launch { viewModel.userSettings.collect { } }
        viewModel.userSettings.filterIsInstance<SettingsUiState.Success>().first()

        viewModel.saveStickMode(
            isRightStick = false,
            mode = JoystickMode.Hold(JoystickMode.LEFT),
        )
        viewModel.saveStickMode(
            isRightStick = true,
            mode = JoystickMode.VerticalHold(JoystickMode.UP),
        )
        runCurrent()

        viewModel.onLeftStickChanged(0.8f, -0.4f)
        viewModel.onRightStickChanged(-1f, 1f)
        viewModel.onLeftSwitchChanged(0, true)
        viewModel.onRightSwitchChanged(2, true)
        viewModel.onLeftKnobChanged(0.9f)
        viewModel.onRightKnobChanged(0.3f)

        viewModel.resetLiveRcControlsToZero()

        val rc = viewModel.rcControlState.value
        assertEquals(-1f, rc.leftStickPosition.first, 0.001f)
        assertEquals(0f, rc.leftStickPosition.second, 0.001f)
        assertEquals(0f, rc.rightStickPosition.first, 0.001f)
        assertEquals(1f, rc.rightStickPosition.second, 0.001f)
        assertFalse(rc.leftSwitches[0])
        assertFalse(rc.rightSwitches[2])
        assertEquals(0f, rc.leftKnobValue)
        assertEquals(0f, rc.rightKnobValue)
        collectJob.cancel()
    }

    @Test
    fun `toggleSessionRecording then save copies csv to downloads`() = runTest {
        viewModel.toggleSessionRecording()
        assertTrue(viewModel.sessionRecording.value.isRecording)

        viewModel.toggleSessionRecording()
        val stopped = viewModel.sessionRecording.value
        assertFalse(stopped.isRecording)
        assertNotNull(stopped.finishedFile)
        assertTrue(stopped.finishedFileName.endsWith(".csv"))
        assertTrue(stopped.finishedFile!!.readText().contains("timestamp_ms"))

        var saved = false
        viewModel.saveFinishedSessionCsvToDownloads { saved = it }
        assertTrue(saved)
        assertNull(viewModel.sessionRecording.value.finishedFile)
        assertTrue(viewModel.sessionRecording.value.savedLocation.isNotBlank())
        assertEquals(1, fakeSessionCsv.savedDownloads.size)
        assertTrue(fakeSessionCsv.savedDownloads.first().readText().contains("timestamp_ms"))
    }
}
