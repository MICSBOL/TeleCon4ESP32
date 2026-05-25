package com.micsbol.emitterapp.ui.bluetooth

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.micsbol.emitterapp.R
import com.micsbol.emitterapp.domain.bluetooth.BluetoothDevice
import com.micsbol.emitterapp.domain.bluetooth.RemoteDevice
import com.micsbol.emitterapp.ui.ads.AdPolicy
import com.micsbol.emitterapp.ui.components.EmitterAppScaffold
import com.micsbol.emitterapp.ui.navigation.Screen
import com.micsbol.emitterapp.ui.components.EmitterIconContainer
import com.micsbol.emitterapp.ui.components.EmitterSectionTitle
import com.micsbol.emitterapp.ui.components.EmitterStyledCard
import com.micsbol.emitterapp.ui.components.brandPrimary
import com.micsbol.emitterapp.ui.components.mutedTextColor
import com.micsbol.emitterapp.ui.theme.EmitterAppTheme

@Composable
fun BluetoothScreen(
    state: BluetoothUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceClick: (RemoteDevice) -> Unit,
    onNavigateBack: () -> Unit,
    onDismissError: () -> Unit = {},
) {
    if (state.errorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.bluetooth_connection_error)) },
            text = { Text(state.errorMessage) },
            confirmButton = {
                TextButton(onClick = onDismissError) { Text(stringResource(R.string.codes_dialog_ok)) }
            }
        )
    }

    EmitterAppScaffold(
        title = stringResource(R.string.home_title),
        subtitle = stringResource(R.string.bluetooth_connect_to_a_device),
        showAdBanner = AdPolicy.hasBanner(Screen.Bluetooth.route),
        onNavigateBack = onNavigateBack,
        actions = {
            if (state.isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(28.dp)
                        .padding(end = 4.dp),
                    color = brandPrimary(),
                    strokeWidth = 3.dp
                )
                IconButton(onClick = onStopScan) {
                    EmitterIconContainer(
                        icon = Icons.Default.Close,
                        contentDescription = stringResource(R.string.bluetooth_stop_scan)
                    )
                }
            } else {
                IconButton(onClick = onStartScan) {
                    EmitterIconContainer(
                        icon = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.bluetooth_start_scan)
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                BluetoothDeviceList(
                    pairedDevices = state.pairedDevices,
                    scannedDevices = state.scannedDevices,
                    onClick = onDeviceClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.isScanning && state.pairedDevices.isEmpty() && state.scannedDevices.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = brandPrimary()
                )
            }

            if (state.isConnecting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    EmitterStyledCard(modifier = Modifier.padding(24.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = brandPrimary())
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.home_bluetooth_status_connecting),
                                style = MaterialTheme.typography.bodyLarge
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
            EmitterSectionTitle(
                text = stringResource(R.string.bluetooth_paired_devices),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (pairedDevices.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.bluetooth_no_paired_devices),
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedTextColor(),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
        items(pairedDevices) { device ->
            DeviceListItem(device = device, onClick = onClick)
        }
        item {
            EmitterSectionTitle(
                text = stringResource(R.string.bluetooth_scanned_devices),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (scannedDevices.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.bluetooth_no_scanned_devices),
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedTextColor(),
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
    EmitterStyledCard(
        modifier = Modifier.clickable { onClick(device) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmitterIconContainer(
                icon = Icons.Default.Bluetooth,
                contentDescription = null
            )
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    text = device.name ?: stringResource(R.string.bluetooth_unknown_device),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedTextColor()
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

    EmitterAppTheme {
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

    EmitterAppTheme {
        BluetoothScreen(
            state = fakeState,
            onStartScan = { },
            onStopScan = { },
            onDeviceClick = { },
            onNavigateBack = { },
        )
    }
}
