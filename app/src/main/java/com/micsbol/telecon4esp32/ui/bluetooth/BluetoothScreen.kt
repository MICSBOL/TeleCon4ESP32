package com.micsbol.telecon4esp32.ui.bluetooth

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothDevice
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteDevice
import com.micsbol.telecon4esp32.ui.ads.AdPolicy
import com.micsbol.telecon4esp32.ui.entitlement.LocalEntitlement
import com.micsbol.telecon4esp32.ui.components.AdBanner
import com.micsbol.telecon4esp32.ui.components.NeoCard
import com.micsbol.telecon4esp32.ui.components.NeoIconBadge
import com.micsbol.telecon4esp32.ui.components.NeoScaffold
import com.micsbol.telecon4esp32.ui.components.NeoSectionTitle
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.theme.Neo
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme

@Composable
fun BluetoothScreen(
    state: BluetoothUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceClick: (RemoteDevice) -> Unit,
    onNavigateBack: () -> Unit,
    onDismissError: () -> Unit = {},
) {
    val entitlement = LocalEntitlement.current

    BluetoothConnectionErrorDialog(
        handshakeFailure = state.handshakeFailure,
        connectFailure = state.connectFailure,
        errorMessage = state.errorMessage,
        onDismiss = onDismissError,
    )

    val showAdBanner = AdPolicy.hasBanner(Screen.Bluetooth.route, entitlement)

    NeoScaffold(
        title = stringResource(R.string.home_title),
        subtitle = stringResource(R.string.bluetooth_connect_to_a_device),
        onNavigateBack = onNavigateBack,
        actions = {
            if (state.isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(28.dp)
                        .padding(end = 4.dp),
                    color = Neo.Accent,
                    strokeWidth = 3.dp
                )
                IconButton(onClick = onStopScan) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.bluetooth_stop_scan),
                        tint = Neo.Accent,
                    )
                }
            } else {
                IconButton(onClick = onStartScan) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.bluetooth_start_scan),
                        tint = Neo.Accent,
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                BluetoothDeviceList(
                    pairedDevices = state.pairedDevices,
                    scannedDevices = state.scannedDevices,
                    onClick = onDeviceClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
                if (showAdBanner) {
                    AdBanner(modifier = Modifier.fillMaxWidth())
                }
            }

            if (state.isScanning && state.pairedDevices.isEmpty() && state.scannedDevices.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Neo.Accent
                )
            }

            if (state.isConnecting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    NeoCard(modifier = Modifier.padding(24.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Neo.Accent)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.home_bluetooth_status_connecting),
                                color = Neo.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BluetoothDeviceList(
    pairedDevices: List<RemoteDevice>,
    scannedDevices: List<RemoteDevice>,
    onClick: (RemoteDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            NeoSectionTitle(
                text = stringResource(R.string.bluetooth_paired_devices),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (pairedDevices.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.bluetooth_no_paired_devices),
                    color = Neo.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
        items(pairedDevices) { device ->
            DeviceListItem(device = device, onClick = onClick)
        }
        item {
            NeoSectionTitle(
                text = stringResource(R.string.bluetooth_scanned_devices),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (scannedDevices.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.bluetooth_no_scanned_devices),
                    color = Neo.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
        items(scannedDevices) { device ->
            DeviceListItem(device = device, onClick = onClick)
        }
    }
}

@Composable
fun DeviceListItem(
    device: RemoteDevice,
    onClick: (RemoteDevice) -> Unit
) {
    NeoCard(
        onClick = { onClick(device) },
        contentPadding = 12.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeoIconBadge(icon = Icons.Default.Bluetooth, size = 42.dp)
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    text = device.name ?: stringResource(R.string.bluetooth_unknown_device),
                    color = Neo.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = device.address,
                    color = Neo.TextSecondary,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BluetoothScreenPreview() {
    val fakePairedDevices = listOf(
        BluetoothDevice(name = "RC Car", address = "00:11:22:33:44:55"),
        BluetoothDevice(name = "Old RC Remote", address = "AA:BB:CC:DD:EE:FF")
    )
    val fakeScannedDevices = listOf(
        BluetoothDevice(name = "Neighbor's TV", address = "A1:B2:C3:D4:E5:F6"),
        BluetoothDevice(name = "Fitness Tracker", address = "1A:2B:3C:4D:5E:6F")
    )

    val fakeState = BluetoothUiState(
        pairedDevices = fakePairedDevices,
        scannedDevices = fakeScannedDevices
    )

    TeleCon4Esp32Theme {
        BluetoothScreen(
            state = fakeState,
            onStartScan = { },
            onStopScan = { },
            onDeviceClick = { },
            onNavigateBack = { },
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark Mode"
)
@Composable
fun BluetoothScreenPreview_Dark() {
    val fakePairedDevices = listOf(
        BluetoothDevice(name = "RC Car", address = "00:11:22:33:44:55"),
    )
    val fakeState = BluetoothUiState(
        pairedDevices = fakePairedDevices,
        scannedDevices = emptyList(),
        isScanning = true
    )

    TeleCon4Esp32Theme {
        BluetoothScreen(
            state = fakeState,
            onStartScan = { },
            onStopScan = { },
            onDeviceClick = { },
            onNavigateBack = { },
        )
    }
}
