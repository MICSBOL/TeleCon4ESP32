package com.micsbol.telecon4esp32.ui.navigation

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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.about.AboutScreen
import com.micsbol.telecon4esp32.ui.about.PrivacyPolicyScreen
import com.micsbol.telecon4esp32.ui.applications.ApplicationSettingsHostScreen
import com.micsbol.telecon4esp32.ui.applications.ApplicationsScreen
import com.micsbol.telecon4esp32.ui.applications.ProApplicationPlaceholderScreen
import com.micsbol.telecon4esp32.ui.greenhouse.GreenhouseScreen
import com.micsbol.telecon4esp32.ui.smarthome.SmartHomeScreen
import com.micsbol.telecon4esp32.ui.smartdoorlock.SmartDoorLockScreen
import com.micsbol.telecon4esp32.ui.smartlighting.SmartLightingScreen
import com.micsbol.telecon4esp32.ui.solarsystem.SolarSystemScreen
import com.micsbol.telecon4esp32.ui.watertank.WaterTankScreen
import com.micsbol.telecon4esp32.ui.bluetooth.BluetoothScreen
import com.micsbol.telecon4esp32.ui.bluetooth.BluetoothViewModel
import com.micsbol.telecon4esp32.ui.codes.CodesScreen
import com.micsbol.telecon4esp32.ui.control_panel.ControlPanelScreen
import com.micsbol.telecon4esp32.ui.entitlement.EntitlementViewModel
import com.micsbol.telecon4esp32.ui.entitlement.LocalEntitlement
import com.micsbol.telecon4esp32.ui.home.HomeScreen
import com.micsbol.telecon4esp32.ui.premium.UpgradeScreen
import com.micsbol.telecon4esp32.ui.rc_settings.SettingsViewModel
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProScreen
import com.micsbol.telecon4esp32.ui.splash.SplashScreen
import com.micsbol.telecon4esp32.ui.tutorial.TutorialScreen

@Composable
fun AppNavGraph(
    onComposeSplashReady: () -> Unit = {},
) {

    val navController = rememberNavController()
    val bluetoothViewModel = hiltViewModel<BluetoothViewModel>()
    val settingsViewModel = hiltViewModel<SettingsViewModel>()
    val entitlementViewModel = hiltViewModel<EntitlementViewModel>()
    val entitlement by entitlementViewModel.entitlement.collectAsState()

    CompositionLocalProvider(LocalEntitlement provides entitlement) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                navController = navController,
                onComposeSplashReady = onComposeSplashReady,
            )
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
                onNavigateBack = { navController.navigateUp() },
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
        composable(Screen.Applications.route) {
            ApplicationsScreen(navController = navController)
        }
        composable(Screen.ControlPanel.route) {
            ControlPanelScreen(
                bluetoothViewModel = bluetoothViewModel,
                navController = navController,
            )
        }
        composable(Screen.RcVehiclePro.route) {
            RcVehicleProScreen(
                navController = navController,
                bluetoothViewModel = bluetoothViewModel,
            )
        }
        composable(Screen.GreenhousePro.route) {
            GreenhouseScreen(navController = navController)
        }
        composable(Screen.SolarPro.route) {
            SolarSystemScreen(navController = navController)
        }
        composable(Screen.SmartHomePro.route) {
            SmartHomeScreen(navController = navController)
        }
        composable(Screen.WaterTankPro.route) {
            WaterTankScreen(navController = navController)
        }
        composable(Screen.SmartDoorLockPro.route) {
            SmartDoorLockScreen(navController = navController)
        }
        composable(Screen.SmartLightingPro.route) {
            SmartLightingScreen(navController = navController)
        }
        composable(Screen.CustomDashboardPro.route) {
            ProApplicationPlaceholderScreen(
                navController = navController,
                applicationId = ApplicationId.CUSTOM_DASHBOARD,
            )
        }
        composable(Screen.Upgrade.route) {
            UpgradeScreen(navController = navController)
        }
        composable(
            route = Screen.ApplicationSettings.route,
            arguments = listOf(
                navArgument("applicationId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val applicationId = backStackEntry.arguments
                ?.getString("applicationId")
                ?.let { runCatching { ApplicationId.valueOf(it) }.getOrNull() }
                ?: return@composable
            ApplicationSettingsHostScreen(
                navController = navController,
                applicationId = applicationId,
                settingsViewModel = settingsViewModel,
            )
        }
        composable(
            route = Screen.ProPlaceholder.route,
            arguments = listOf(
                navArgument("applicationId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val applicationId = backStackEntry.arguments
                ?.getString("applicationId")
                ?.let { runCatching { ApplicationId.valueOf(it) }.getOrNull() }
                ?: return@composable
            ProApplicationPlaceholderScreen(
                navController = navController,
                applicationId = applicationId,
            )
        }
        composable(Screen.Tutorial.route) {
            TutorialScreen(navController = navController)
        }
        composable(Screen.Codes.route){
            CodesScreen(navController = navController)
        }
        composable(Screen.About.route) {
            AboutScreen(navController = navController)
        }
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(navController = navController)
        }
    }
    }
}