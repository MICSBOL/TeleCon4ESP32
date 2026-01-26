package com.example.emitterapp.ui.bluetooth

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.emitterapp.domain.bluetooth.BluetoothDevice
import com.example.emitterapp.ui.theme.EmitterAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScreen(
    state: BluetoothUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit,
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect to a Device") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    if (state.isScanning) {
                        // 1. Show a progress indicator to signify scanning is active.
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp), // A good size for an app bar
                            color = MaterialTheme.colorScheme.onPrimary, // Match the icon color
                            strokeWidth = 3.dp // A slightly thicker line
                        )
                        // 2. Also show the button to stop the scan.
                        IconButton(onClick = onStopScan) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Stop Scan"
                            )
                        }
                    } else {
                        // When not scanning, just show the button to start.
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            BluetoothDeviceList(
                pairedDevices = state.pairedDevices,
                scannedDevices = state.scannedDevices,
                onClick = onDeviceClick,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (state.isScanning) {
            if (state.scannedDevices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun BluetoothDeviceList(
    pairedDevices: List<BluetoothDevice>,
    scannedDevices: List<BluetoothDevice>,
    onClick: (BluetoothDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        item {
            Text(
                text = "Paired Devices",
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
                text = "Scanned Devices",
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
    device: BluetoothDevice,
    onClick: (BluetoothDevice) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp) // Add spacing between cards
            .clickable { onClick(device) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Add a subtle shadow
    ) {
        Row(
            modifier = Modifier.padding(16.dp), // Internal padding for the content
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = "Bluetooth Device",
                // Use a prominent color like primary for the icon
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = device.name ?: "(No name)",
                style = MaterialTheme.typography.bodyLarge,
                // This color will automatically adapt to light/dark mode
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