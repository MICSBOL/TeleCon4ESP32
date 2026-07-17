package com.micsbol.telecon4esp32.ui.bluetooth

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.model.ApplicationId

@Composable
fun WrapApplicationBluetoothSession(
    applicationId: ApplicationId,
    protocolMode: BluetoothProtocolMode,
    bluetoothViewModel: BluetoothViewModel,
    navController: NavHostController,
    content: @Composable () -> Unit,
) {
    ApplicationBluetoothSessionHost(
        applicationId = applicationId,
        protocolMode = protocolMode,
        bluetoothViewModel = bluetoothViewModel,
        navController = navController,
    ) {
        content()
    }
}
