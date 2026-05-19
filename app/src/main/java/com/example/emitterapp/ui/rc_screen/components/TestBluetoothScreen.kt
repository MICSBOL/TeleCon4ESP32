package com.example.emitterapp.ui.rc_screen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.emitterapp.R
import com.example.emitterapp.ui.components.EmitterAppScaffold
import com.example.emitterapp.ui.components.EmitterFilledButton
import com.example.emitterapp.ui.components.brandPrimary

@Composable
fun TestBluetoothScreen(
    onSendTestPacket: () -> Unit,
    onDisconnect: () -> Unit
) {
    EmitterAppScaffold(
        title = stringResource(R.string.home_title),
        subtitle = stringResource(R.string.bluetooth_connect_to_a_device)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Messages",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = onDisconnect) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = brandPrimary()
                    )
                }
            }
            EmitterFilledButton(
                text = "Send Test Packet",
                onClick = onSendTestPacket,
                modifier = Modifier.fillMaxWidth(0.85f)
            )
        }
    }
}
