package com.micsbol.telecon4esp32.ui.bluetooth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.supportsCamVideoControl
import com.micsbol.telecon4esp32.ui.applications.titleRes
import com.micsbol.telecon4esp32.ui.components.DisconnectedBannerInsets
import com.micsbol.telecon4esp32.ui.components.LiveControlBluetoothDisconnectedBannerOverlay
import com.micsbol.telecon4esp32.ui.components.LocalDisconnectedBannerInsets
import com.micsbol.telecon4esp32.ui.components.LocalHudGlassDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialogBody
import com.micsbol.telecon4esp32.ui.components.NeoDialogTitle
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.components.NeoSecondaryButton
import com.micsbol.telecon4esp32.ui.navigation.Screen

/**
 * Wraps an application screen with contextual Bluetooth / SoftAP session UX:
 * disconnected banner, session-conflict warning, and connect navigation.
 */
@Composable
fun ApplicationBluetoothSessionHost(
    applicationId: ApplicationId,
    protocolMode: BluetoothProtocolMode,
    bluetoothViewModel: BluetoothViewModel,
    navController: NavController?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state by bluetoothViewModel.state.collectAsState()
    val connectionMode by remember(applicationId) {
        bluetoothViewModel.observeLiveConnectionMode(applicationId)
    }.collectAsState(initial = BluetoothConnectionMode.CLASSIC_SIMPLE)
    val transport = connectionMode.transport
    val board by remember(applicationId) {
        bluetoothViewModel.observeBoard(applicationId)
    }.collectAsState(initial = Esp32Board.defaultFor(applicationId))
    val ensureBluetoothReady = rememberEnsureBluetoothReady()

    LaunchedEffect(applicationId) {
        bluetoothViewModel.markRecentApplication(applicationId)
    }

    val connectOverBluetooth by rememberUpdatedState {
        openApplicationBluetooth(
            navController = navController,
            bluetoothViewModel = bluetoothViewModel,
            applicationId = applicationId,
            connectionMode = connectionMode,
        )
    }

    fun ensureBluetoothReadyThenConnect() {
        ensureBluetoothReady { connectOverBluetooth() }
    }

    val softApConnect = rememberSoftApConnectAction(
        onConnect = {
            val connectionMode = when {
                applicationId.supportsCamVideoControl() && board == Esp32Board.CAM ->
                    if (protocolMode == BluetoothProtocolMode.ADVANCED) {
                        BluetoothConnectionMode.WIFI_BINARY
                    } else {
                        BluetoothConnectionMode.WIFI_CAM_STARTER
                    }
                else ->
                    BluetoothConnectionMode.WIFI_BINARY
            }
            openApplicationWifiSoftAp(
                bluetoothViewModel = bluetoothViewModel,
                applicationId = applicationId,
                protocolMode = connectionMode.protocolMode,
                connectionMode = connectionMode,
            )
        },
    )

    fun ensureReadyThenConnect() {
        if (transport == BluetoothTransportType.WIFI) {
            // SoftAP: API 29+ requests local-only Wi‑Fi in-app; older APIs open system Wi‑Fi.
            bluetoothViewModel.dismissError()
            softApConnect()
            return
        }
        ensureBluetoothReadyThenConnect()
    }

    LaunchedEffect(navController) {
        bluetoothViewModel.navigateToScreen.collect { route ->
            if (route == BluetoothViewModel.POP_BACK_ON_CONNECT) {
                navController?.navigateUp()
            } else {
                navController?.navigate(route)
            }
        }
    }

    val isConnectedForApp =
        state.isConnected && state.activeSession?.applicationId == applicationId
    val hasConflict =
        state.isConnected &&
            state.activeSession != null &&
            state.activeSession?.applicationId != applicationId
    val conflictSession = state.activeSession
    val hudGlassDialog = applicationId == ApplicationId.RC_VEHICLE_PRO

    CompositionLocalProvider(LocalHudGlassDialog provides hudGlassDialog) {
    BluetoothConnectionErrorDialog(
        handshakeFailure = state.handshakeFailure,
        connectFailure = state.connectFailure,
        errorMessage = state.errorMessage,
        onDismiss = bluetoothViewModel::dismissError,
    )

    if (hasConflict && conflictSession != null) {
        NeoDialog(
            onDismissRequest = { navController?.navigateUp() },
            wrapContentHeight = true,
            title = {
                NeoDialogTitle(text = stringResource(R.string.bluetooth_session_conflict_title))
            },
            subtitle = {
                NeoDialogBody(
                    text = stringResource(
                        R.string.bluetooth_session_conflict_body,
                        stringResource(conflictSession.applicationId.titleRes()),
                        stringResource(applicationId.titleRes()),
                    ),
                )
            },
            actions = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NeoSecondaryButton(
                        text = stringResource(R.string.codes_pdf_cancel),
                        onClick = { navController?.navigateUp() },
                        compact = true,
                    )
                    NeoPillButton(
                        text = stringResource(R.string.bluetooth_session_disconnect_other),
                        onClick = { bluetoothViewModel.disconnectFromDevice() },
                        compact = true,
                    )
                }
            },
        )
    }

    val sessionUi = ApplicationBluetoothSessionUi(
        isConnected = isConnectedForApp,
        isConnecting = state.isConnecting,
        transport = transport,
        onConnect = {
            if (navController != null && !hasConflict) {
                ensureReadyThenConnect()
            }
        },
    )

    val currentSessionUi by rememberUpdatedState(sessionUi)
    val disconnectedBannerInsets = remember { DisconnectedBannerInsets() }

    CompositionLocalProvider(
        LocalApplicationBluetoothSession provides currentSessionUi,
        LocalDisconnectedBannerInsets provides disconnectedBannerInsets,
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            content()

            if (navController != null &&
                !isConnectedForApp &&
                !state.isConnecting &&
                !hasConflict &&
                !disconnectedBannerInsets.suppressHostOverlay
            ) {
                LiveControlBluetoothDisconnectedBannerOverlay(
                    visible = true,
                    onClick = { ensureReadyThenConnect() },
                )
            }
        }
    }
    }
}

private fun openApplicationWifiSoftAp(
    bluetoothViewModel: BluetoothViewModel,
    applicationId: ApplicationId,
    protocolMode: BluetoothProtocolMode,
    connectionMode: BluetoothConnectionMode,
) {
    bluetoothViewModel.requestApplicationConnection(
        applicationId = applicationId,
        protocolMode = protocolMode,
        transport = BluetoothTransportType.WIFI,
        connectionMode = connectionMode,
    )
    // Stay on the app screen — SoftAP connect does not open the BT picker.
    bluetoothViewModel.connectToWifiSoftAp()
}

private fun openApplicationBluetooth(
    navController: NavController?,
    bluetoothViewModel: BluetoothViewModel,
    applicationId: ApplicationId,
    connectionMode: BluetoothConnectionMode,
) {
    bluetoothViewModel.requestApplicationConnection(
        applicationId = applicationId,
        protocolMode = connectionMode.protocolMode,
        transport = connectionMode.transport,
        connectionMode = connectionMode,
    )
    bluetoothViewModel.preparePostConnectPopBack()
    navController?.navigate(Screen.Bluetooth.route)
}
