package com.example.emitterapp.ui.bluetooth

import android.content.res.Configuration
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.emitterapp.R
import com.example.emitterapp.domain.bluetooth.BluetoothDevice
import com.example.emitterapp.domain.bluetooth.RemoteDevice
import com.example.emitterapp.ui.theme.EmitterAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScreen(
    state: BluetoothUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceClick: (RemoteDevice) -> Unit,
    onDismissError: () -> Unit = {},
) {
    // ----- Error dialog -----
    if (state.errorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.bluetooth_connection_error)) },
            text  = { Text(state.errorMessage) },
            confirmButton = {
                TextButton(onClick = onDismissError) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bluetooth_connect_to_a_device)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    if (state.isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 3.dp
                        )
                        IconButton(onClick = onStopScan) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Stop Scan"
                            )
                        }
                    } else {
                        IconButton(onClick = onStartScan) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Start Scan"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                BluetoothDeviceList(
                    pairedDevices = state.pairedDevices,
                    scannedDevices = state.scannedDevices,
                    onClick = onDeviceClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ----- Scanning empty-list spinner -----
            if (state.isScanning && state.pairedDevices.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            // ----- Connecting overlay -----
            if (state.isConnecting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = false) {},   // block touches while connecting
                    contentAlignment = Alignment.Center
                ) {
                    Card(elevation = CardDefaults.cardElevation(8.dp)) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Connecting…", style = MaterialTheme.typography.bodyLarge)
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
    LazyColumn(modifier = modifier) {
        item {
            Text(
                text = stringResource(R.string.bluetooth_paired_devices),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )
        }
        items(pairedDevices) { device ->
            DeviceListItem(device = device, onClick = onClick)
        }
        item {
            Text(
                text = stringResource(R.string.bluetooth_scanned_devices),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick(device) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = "Bluetooth Device",
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = device.name ?: "(No name)",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp)
            )
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

    EmitterAppTheme {
        BluetoothScreen(
            state = fakeState,
            onStartScan = { },
            onStopScan = { },
            onDeviceClick = { },
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
        BluetoothDevice(name = "Old RC Remote", address = "AA:BB:CC:DD:EE:FF")
    )
    val fakeScannedDevices = listOf(
        BluetoothDevice(name = "Neighbor's TV", address = "A1:B2:C3:D4:E5:F6"),
        BluetoothDevice(name = "Fitness Tracker", address = "1A:2B:3C:4D:5E:6F")
    )

    val fakeState = BluetoothUiState(
        pairedDevices = fakePairedDevices,
        scannedDevices = fakeScannedDevices,
        isScanning = true
    )

    EmitterAppTheme {
        BluetoothScreen(
            state = fakeState,
            onStartScan = { },
            onStopScan = { },
            onDeviceClick = { },
        )
    }
}