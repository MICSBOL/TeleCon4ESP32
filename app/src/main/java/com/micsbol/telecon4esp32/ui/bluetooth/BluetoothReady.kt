package com.micsbol.telecon4esp32.ui.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothRuntimePermissions

/**
 * Requests Nearby devices (and fine location on Android 11 and below) and
 * Bluetooth enable, then runs [onReady]. Does not require Location to be on.
 */
@Composable
fun rememberEnsureBluetoothReady(): (() -> Unit) -> Unit {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java)?.adapter
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val enabled = result.resultCode == Activity.RESULT_OK ||
            bluetoothAdapter?.isEnabled == true
        if (enabled) {
            val next = pending
            pending = null
            next?.invoke()
        } else {
            pending = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { perms ->
        val granted = BluetoothRuntimePermissions.required().all { perms[it] == true } ||
            BluetoothRuntimePermissions.allGranted(context)
        if (!granted) {
            pending = null
            return@rememberLauncherForActivityResult
        }
        if (bluetoothAdapter?.isEnabled == false && activity != null) {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } else {
            val next = pending
            pending = null
            next?.invoke()
        }
    }

    val ensureReady by rememberUpdatedState<( () -> Unit) -> Unit>(
        { onReady: () -> Unit ->
            if (!BluetoothRuntimePermissions.allGranted(context)) {
                pending = onReady
                permissionLauncher.launch(BluetoothRuntimePermissions.required())
            } else if (bluetoothAdapter?.isEnabled == false && activity != null) {
                pending = onReady
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } else {
                onReady()
            }
        },
    )

    return { onReady -> ensureReady(onReady) }
}
