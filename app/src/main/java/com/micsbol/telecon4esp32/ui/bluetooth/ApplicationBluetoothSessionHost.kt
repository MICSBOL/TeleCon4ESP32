package com.micsbol.telecon4esp32.ui.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.applications.titleRes
import com.micsbol.telecon4esp32.ui.components.LiveControlBluetoothDisconnectedBannerOverlay
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
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    val bluetoothAdapter = bluetoothManager?.adapter
    val state by bluetoothViewModel.state.collectAsState()
    val transport by remember(applicationId) {
        bluetoothViewModel.observeTransportType(applicationId)
    }.collectAsState(initial = BluetoothTransportType.CLASSIC)
    var pendingConnect by remember { mutableStateOf(false) }

    LaunchedEffect(applicationId) {
        bluetoothViewModel.markRecentApplication(applicationId)
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (pendingConnect) {
            pendingConnect = false
            openApplicationBluetooth(
                navController = navController,
                bluetoothViewModel = bluetoothViewModel,
                applicationId = applicationId,
                protocolMode = protocolMode,
                transport = transport,
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { perms ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scanGranted = perms[Manifest.permission.BLUETOOTH_SCAN] == true
            val connectGranted = perms[Manifest.permission.BLUETOOTH_CONNECT] == true
            if (scanGranted && connectGranted) {
                if (bluetoothAdapter?.isEnabled == false && activity != null) {
                    pendingConnect = true
                    enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                } else {
                    openApplicationBluetooth(
                        navController = navController,
                        bluetoothViewModel = bluetoothViewModel,
                        applicationId = applicationId,
                        protocolMode = protocolMode,
                        transport = transport,
                    )
                }
            }
        }
    }

    fun ensureBluetoothReadyThenConnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scanGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN,
            ) == PackageManager.PERMISSION_GRANTED
            val connectGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED
            if (!scanGranted || !connectGranted) {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                    ),
                )
                return
            }
        }
        if (bluetoothAdapter?.isEnabled == false && activity != null) {
            pendingConnect = true
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }
        openApplicationBluetooth(
            navController = navController,
            bluetoothViewModel = bluetoothViewModel,
            applicationId = applicationId,
            protocolMode = protocolMode,
            transport = transport,
        )
    }

    fun ensureReadyThenConnect() {
        if (transport == BluetoothTransportType.WIFI) {
            openApplicationWifiSoftAp(
                bluetoothViewModel = bluetoothViewModel,
                applicationId = applicationId,
                protocolMode = protocolMode,
            )
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

    BluetoothConnectionErrorDialog(
        handshakeFailure = state.handshakeFailure,
        connectFailure = state.connectFailure,
        errorMessage = state.errorMessage,
        onDismiss = bluetoothViewModel::dismissError,
    )

    if (hasConflict && conflictSession != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.bluetooth_session_conflict_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.bluetooth_session_conflict_body,
                        stringResource(conflictSession.applicationId.titleRes()),
                        stringResource(applicationId.titleRes()),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { bluetoothViewModel.disconnectFromDevice() }) {
                    Text(stringResource(R.string.bluetooth_session_disconnect_other))
                }
            },
            dismissButton = {
                TextButton(onClick = { navController?.navigateUp() }) {
                    Text(stringResource(R.string.codes_pdf_cancel))
                }
            },
        )
    }

    val sessionUi = ApplicationBluetoothSessionUi(
        isConnected = isConnectedForApp,
        isConnecting = state.isConnecting,
        onConnect = {
            if (navController != null && !hasConflict) {
                ensureReadyThenConnect()
            }
        },
    )

    val currentSessionUi by rememberUpdatedState(sessionUi)

    CompositionLocalProvider(
        LocalApplicationBluetoothSession provides currentSessionUi,
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            content()

            if (navController != null &&
                transport != BluetoothTransportType.WIFI &&
                !isConnectedForApp &&
                !state.isConnecting &&
                !hasConflict
            ) {
                LiveControlBluetoothDisconnectedBannerOverlay(
                    visible = true,
                    onClick = { ensureReadyThenConnect() },
                )
            }
        }
    }
}

private fun openApplicationWifiSoftAp(
    bluetoothViewModel: BluetoothViewModel,
    applicationId: ApplicationId,
    protocolMode: BluetoothProtocolMode,
) {
    bluetoothViewModel.requestApplicationConnection(
        applicationId,
        protocolMode,
        BluetoothTransportType.WIFI,
    )
    // Stay on the app screen — SoftAP connect does not open the BT picker.
    bluetoothViewModel.connectToWifiSoftAp()
}

private fun openApplicationBluetooth(
    navController: NavController?,
    bluetoothViewModel: BluetoothViewModel,
    applicationId: ApplicationId,
    protocolMode: BluetoothProtocolMode,
    transport: BluetoothTransportType,
) {
    bluetoothViewModel.requestApplicationConnection(applicationId, protocolMode, transport)
    bluetoothViewModel.preparePostConnectPopBack()
    navController?.navigate(Screen.Bluetooth.route)
}
