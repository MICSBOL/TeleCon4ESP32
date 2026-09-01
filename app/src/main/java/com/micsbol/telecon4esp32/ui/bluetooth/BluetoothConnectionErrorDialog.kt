package com.micsbol.telecon4esp32.ui.bluetooth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectFailure
import com.micsbol.telecon4esp32.domain.bluetooth.HandshakeFailure
import com.micsbol.telecon4esp32.ui.components.NeoDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialogBody
import com.micsbol.telecon4esp32.ui.components.NeoDialogTitle
import com.micsbol.telecon4esp32.ui.components.NeoPillButton

/**
 * Frosted-glass error dialog for link failures and protocol/app handshake mismatches.
 */
@Composable
fun BluetoothConnectionErrorDialog(
    handshakeFailure: HandshakeFailure?,
    connectFailure: BluetoothConnectFailure? = null,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
) {
    val content = bluetoothConnectionErrorContent(
        handshakeFailure = handshakeFailure,
        connectFailure = connectFailure,
        errorMessage = errorMessage,
    ) ?: return

    NeoDialog(
        onDismissRequest = onDismiss,
        wrapContentHeight = true,
        title = {
            NeoDialogTitle(text = content.title)
        },
        subtitle = {
            NeoDialogBody(text = content.body)
        },
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NeoPillButton(
                    text = stringResource(R.string.codes_dialog_ok),
                    onClick = onDismiss,
                    compact = true,
                )
            }
        },
    )
}

private data class BluetoothConnectionErrorContent(
    val title: String,
    val body: String,
)

@Composable
private fun bluetoothConnectionErrorContent(
    handshakeFailure: HandshakeFailure?,
    connectFailure: BluetoothConnectFailure?,
    errorMessage: String?,
): BluetoothConnectionErrorContent? = when {
    handshakeFailure != null -> BluetoothConnectionErrorContent(
        title = handshakeFailureTitle(handshakeFailure),
        body = handshakeFailureMessage(handshakeFailure),
    )
    connectFailure != null -> BluetoothConnectionErrorContent(
        title = connectFailureTitle(connectFailure),
        body = connectFailureMessage(connectFailure),
    )
    errorMessage == "missing_session_context" -> BluetoothConnectionErrorContent(
        title = stringResource(R.string.bluetooth_connection_failed_title),
        body = stringResource(R.string.bluetooth_missing_session_context),
    )
    errorMessage != null -> BluetoothConnectionErrorContent(
        title = stringResource(R.string.bluetooth_connection_failed_title),
        body = stringResource(R.string.bluetooth_connection_failed_body, errorMessage),
    )
    else -> null
}
