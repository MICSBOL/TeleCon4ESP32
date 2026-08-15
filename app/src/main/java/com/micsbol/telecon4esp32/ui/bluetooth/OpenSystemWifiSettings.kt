package com.micsbol.telecon4esp32.ui.bluetooth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.micsbol.telecon4esp32.R

/** Opens the system Wi‑Fi settings panel so the user can join the ESP32 SoftAP. */
fun openSystemWifiSettings(context: Context) {
    val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
    if (context !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(
            context,
            context.getString(R.string.app_settings_open_wifi_settings_failed),
            Toast.LENGTH_SHORT,
        ).show()
    }
}

/** True when TeleCon can request SoftAP via [android.net.wifi.WifiNetworkSpecifier]. */
fun supportsInAppSoftApJoin(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

/**
 * Opens system Wi‑Fi settings on click, then runs [onConnect] when the host resumes
 * (after the user joins SoftAP / returns). Used on API &lt; 29 or as a manual fallback.
 * Connection errors should only surface from [onConnect], not before Wi‑Fi settings.
 */
@Composable
fun rememberOpenWifiSettingsThenConnect(
    onConnect: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingConnectAfterWifi by remember { mutableStateOf(false) }
    val currentOnConnect by rememberUpdatedState(onConnect)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingConnectAfterWifi) {
                pendingConnectAfterWifi = false
                currentOnConnect()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return remember(context) {
        {
            pendingConnectAfterWifi = true
            openSystemWifiSettings(context)
        }
    }
}

/**
 * SoftAP connect entry: on API 29+ joins in-app (system SoftAP panel, local-only network);
 * older APIs open Wi‑Fi settings first, then [onConnect] on resume.
 */
@Composable
fun rememberSoftApConnectAction(
    onConnect: () -> Unit,
): () -> Unit {
    val openWifiSettingsThenConnect = rememberOpenWifiSettingsThenConnect(onConnect)
    val currentOnConnect by rememberUpdatedState(onConnect)
    return remember(openWifiSettingsThenConnect) {
        {
            if (supportsInAppSoftApJoin()) {
                currentOnConnect()
            } else {
                openWifiSettingsThenConnect()
            }
        }
    }
}
