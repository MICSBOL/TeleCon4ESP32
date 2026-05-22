package com.micsbol.emitterapp.ui.navigation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.micsbol.emitterapp.ui.bluetooth.BluetoothScreen
import com.micsbol.emitterapp.ui.bluetooth.BluetoothViewModel
import com.micsbol.emitterapp.ui.codes.CodesScreen
import com.micsbol.emitterapp.ui.home.HomeScreen
import com.micsbol.emitterapp.ui.rc_screen.RcScreen
import com.micsbol.emitterapp.ui.rc_settings.RcSettingsScreen
import com.micsbol.emitterapp.ui.rc_settings.SettingsViewModel
import com.micsbol.emitterapp.ui.splash.SplashScreen
import com.micsbol.emitterapp.ui.tutorial.TutorialScreen

@Composable
fun AppNavGraph() {

    val navController = rememberNavController()
    val bluetoothViewModel = hiltViewModel<BluetoothViewModel>()
    val settingsViewModel = hiltViewModel<SettingsViewModel>()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }
        composable(Screen.Home.route) {
            val state by bluetoothViewModel.state.collectAsState()
            val lastDeviceName by bluetoothViewModel.lastDeviceName.collectAsState()
            val context = LocalContext.current
            val activity = context as? ComponentActivity
            val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
            val bluetoothAdapter = bluetoothManager?.adapter

            val enableBluetoothLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {}

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { perms ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val scanGranted = perms[Manifest.permission.BLUETOOTH_SCAN] == true
                    val connectGranted = perms[Manifest.permission.BLUETOOTH_CONNECT] == true
                    if (scanGranted && connectGranted && bluetoothAdapter?.isEnabled == false) {
                        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    }
                }
            }

            fun ensureBluetoothReadyBeforeAction(onReady: () -> Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val scanGranted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED

                    val connectGranted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!scanGranted || !connectGranted) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT
                            )
                        )
                        return
                    }
                }

                if (bluetoothAdapter?.isEnabled == false && activity != null) {
                    enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    return
                }

                onReady()
            }

            LaunchedEffect(Unit) {
                bluetoothViewModel.navigateToScreen.collect { route ->
                    navController.navigate(route)
                }
            }

            HomeScreen(
                navController = navController,
                isConnecting = state.isConnecting,
                isBluetoothConnected = state.isConnected,
                errorMessage = state.errorMessage,
                lastDeviceName = lastDeviceName,
                onDismissError = bluetoothViewModel::dismissError,
                onStartClick = {
                    ensureBluetoothReadyBeforeAction {
                        bluetoothViewModel.quickConnect()
                    }
                }
            )
        }
        composable(Screen.Bluetooth.route) {
            val state by bluetoothViewModel.state.collectAsState()
            val context = LocalContext.current
            val activity = context as? ComponentActivity
            val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
            val bluetoothAdapter = bluetoothManager?.adapter

            val enableBluetoothLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                // No-op: user can tap scan/connect again after enabling Bluetooth.
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { perms ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val scanGranted = perms[Manifest.permission.BLUETOOTH_SCAN] == true
                    val connectGranted = perms[Manifest.permission.BLUETOOTH_CONNECT] == true
                    if (scanGranted && connectGranted && bluetoothAdapter?.isEnabled == false) {
                        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    }
                }
            }

            fun ensureBluetoothReadyBeforeAction(onReady: () -> Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val scanGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED
                    val connectGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!scanGranted || !connectGranted) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT
                            )
                        )
                        return
                    }
                }

                if (bluetoothAdapter?.isEnabled == false && activity != null) {
                    enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    return
                }

                onReady()
            }

            LaunchedEffect(key1 = true) {
                bluetoothViewModel.navigateToScreen.collect { route ->
                    navController.navigate(route)
                }
            }

            BluetoothScreen(
                state = state,
                onStartScan = {
                    ensureBluetoothReadyBeforeAction {
                        bluetoothViewModel.startScan()
                    }
                },
                onStopScan = bluetoothViewModel::stopScan,
                onDismissError = bluetoothViewModel::dismissError,
                onDeviceClick = { device ->
                    ensureBluetoothReadyBeforeAction {
                        bluetoothViewModel.connectToDevice(device)
                    }
                }
            )
        }
        composable(Screen.RcScreen.route) {
            RcScreen(bluetoothViewModel = bluetoothViewModel, navController = navController)
        }
        composable(Screen.RcSettingsScreen.route) {
            RcSettingsScreen(navController = navController, viewModel = settingsViewModel)
        }
        composable(Screen.Tutorial.route) {
            TutorialScreen(navController = navController)
        }
        composable(Screen.Codes.route){
            CodesScreen()
        }
    }
}