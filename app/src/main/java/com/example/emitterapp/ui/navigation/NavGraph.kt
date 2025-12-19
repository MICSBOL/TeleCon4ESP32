package com.example.emitterapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.emitterapp.ui.bluetooth.BluetoothScreen
import com.example.emitterapp.ui.bluetooth.BluetoothViewModel
import com.example.emitterapp.ui.home.HomeScreen
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
            BluetoothScreen(
                state = state,
                onStartScan = viewModel::startScan,
                onStopScan = viewModel::stopScan
            )
        }
    }
}