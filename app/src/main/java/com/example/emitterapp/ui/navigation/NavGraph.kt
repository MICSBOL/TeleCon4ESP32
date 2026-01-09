package com.example.emitterapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.emitterapp.ui.bluetooth.BluetoothScreen
import com.example.emitterapp.ui.bluetooth.BluetoothViewModel
import com.example.emitterapp.ui.home.HomeScreen
import com.example.emitterapp.ui.rc_screen.RcScreen
import com.example.emitterapp.ui.rc_screen.components.TestBluetoothScreen
import com.example.emitterapp.ui.splash.SplashScreen

@Composable
fun AppNavGraph() {

    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Bluetooth.route) {
            val viewModel = hiltViewModel<BluetoothViewModel>()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(key1 = true) {
                viewModel.navigateToScreen.collect { route ->
                    navController.navigate(route)
                }
            }

            BluetoothScreen(
                state = state,
                onStartScan = viewModel::startScan,
                onStopScan = viewModel::stopScan,
                onStartServer = viewModel::waitForIncomingConnections,
                onDeviceClick = viewModel::connectToDevice,
                navController = navController
            )
        }
        composable(Screen.TestBluetooth.route) {
            val viewModel = hiltViewModel<BluetoothViewModel>()
            TestBluetoothScreen(
                onDisconnect = viewModel::disconnectFromDevice,
                onSendTestPacket = viewModel::sendTextRcPacket
            )
        }
        composable(Screen.RcScreen.route) {
            val viewModel = hiltViewModel<BluetoothViewModel>()
            RcScreen(bluetoothViewModel = viewModel)
        }
    }
}