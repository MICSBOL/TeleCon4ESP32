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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.bluetooth.WrapApplicationBluetoothSession
import com.micsbol.telecon4esp32.ui.about.AboutScreen
import com.micsbol.telecon4esp32.ui.about.PrivacyPolicyScreen
import com.micsbol.telecon4esp32.ui.applications.ApplicationSettingsHostScreen
import com.micsbol.telecon4esp32.ui.applications.ApplicationsScreen
import com.micsbol.telecon4esp32.ui.applications.ProApplicationPlaceholderScreen
import com.micsbol.telecon4esp32.ui.greenhouse.GreenhouseCameraScreen
import com.micsbol.telecon4esp32.ui.greenhouse.GreenhouseHelpScreen
import com.micsbol.telecon4esp32.ui.greenhouse.GreenhouseScreen
import com.micsbol.telecon4esp32.ui.greenhouse.GreenhouseViewModel
import com.micsbol.telecon4esp32.ui.greenhouse.GreenhouseSettingsScreen
import com.micsbol.telecon4esp32.ui.smarthome.SmartHomeHelpScreen
import com.micsbol.telecon4esp32.ui.smarthome.SmartHomeScreen
import com.micsbol.telecon4esp32.ui.smartdoorlock.SmartDoorLockHelpScreen
import com.micsbol.telecon4esp32.ui.smartdoorlock.SmartDoorLockScreen
import com.micsbol.telecon4esp32.ui.smartlighting.SmartLightingHelpScreen
import com.micsbol.telecon4esp32.ui.smartlighting.SmartLightingScreen
import com.micsbol.telecon4esp32.ui.solarsystem.SolarHelpScreen
import com.micsbol.telecon4esp32.ui.solarsystem.SolarSystemScreen
import com.micsbol.telecon4esp32.ui.watertank.WaterTankHelpScreen
import com.micsbol.telecon4esp32.ui.watertank.WaterTankScreen
import com.micsbol.telecon4esp32.ui.bluetooth.BluetoothScreen
import com.micsbol.telecon4esp32.ui.bluetooth.BluetoothViewModel
import com.micsbol.telecon4esp32.ui.codes.CodesHubScreen
import com.micsbol.telecon4esp32.ui.codes.CodesScreen
import com.micsbol.telecon4esp32.ui.control_panel.ControlPanelScreen
import com.micsbol.telecon4esp32.ui.entitlement.EntitlementViewModel
import com.micsbol.telecon4esp32.ui.entitlement.LocalEntitlement
import com.micsbol.telecon4esp32.ui.cyber.screens.CyberHomeScreen
import com.micsbol.telecon4esp32.ui.premium.UpgradeScreen
import com.micsbol.telecon4esp32.ui.rc_settings.SettingsViewModel
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProScreen
import com.micsbol.telecon4esp32.ui.splash.SplashScreen
import com.micsbol.telecon4esp32.ui.tutorial.TutorialScreen
import com.micsbol.telecon4esp32.ui.wallet.LocalWallet
import com.micsbol.telecon4esp32.ui.wallet.WalletViewModel

@Composable
fun AppNavGraph(
    onComposeSplashReady: () -> Unit = {},
) {

    val navController = rememberNavController()
    val bluetoothViewModel = hiltViewModel<BluetoothViewModel>()
    val settingsViewModel = hiltViewModel<SettingsViewModel>()
    val entitlementViewModel = hiltViewModel<EntitlementViewModel>()
    val walletViewModel = hiltViewModel<WalletViewModel>()
    val entitlement by entitlementViewModel.entitlement.collectAsState()
    val wallet by walletViewModel.wallet.collectAsState()
    var activeProFeature by remember { mutableStateOf<com.micsbol.telecon4esp32.domain.model.PremiumFeature?>(null) }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            val route = entry.destination.route
            val newFeature = ProAppRoutes.featureForRoute(route)
            if (activeProFeature != null && activeProFeature != newFeature) {
                walletViewModel.onProAppFeatureChanged(activeProFeature)
            }
            activeProFeature = newFeature
        }
    }

    CompositionLocalProvider(
        LocalEntitlement provides entitlement,
        LocalWallet provides wallet,
    ) {
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
            val lastApplicationId by bluetoothViewModel.lastApplicationId.collectAsState()
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

            CyberHomeScreen(
                navController = navController,
                isConnecting = state.isConnecting,
                activeSession = state.activeSession,
                lastApplicationId = lastApplicationId,
                lastDeviceName = state.activeSession?.deviceName ?: lastDeviceName,
                errorMessage = state.errorMessage,
                connectFailure = state.connectFailure,
                handshakeFailure = state.handshakeFailure,
                onDismissError = bluetoothViewModel::dismissError,
                onOpenApplications = { navController.navigate(Screen.Applications.route) },
                onContinueSession = {
                    bluetoothViewModel.continueLastSession { route ->
                        navController.navigate(route)
                    }
                },
                onOpenRecentProject = { applicationId ->
                    ensureBluetoothReadyBeforeAction {
                        bluetoothViewModel.openRecentProject(applicationId) { route ->
                            navController.navigate(route)
                        }
                    }
                },
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
                    if (route == BluetoothViewModel.POP_BACK_ON_CONNECT) {
                        navController.navigateUp()
                    } else {
                        navController.navigate(route) {
                            popUpTo(Screen.Bluetooth.route) { inclusive = true }
                        }
                    }
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
            val protocolMode by remember {
                bluetoothViewModel.observeProtocolMode(ApplicationId.RC_VEHICLE_PRO)
            }.collectAsState(initial = BluetoothProtocolMode.defaultFor(ApplicationId.RC_VEHICLE_PRO))
            WrapApplicationBluetoothSession(
                applicationId = ApplicationId.RC_VEHICLE_PRO,
                protocolMode = protocolMode,
                bluetoothViewModel = bluetoothViewModel,
                navController = navController,
            ) {
                RcVehicleProScreen(
                    navController = navController,
                    bluetoothViewModel = bluetoothViewModel,
                )
            }
        }
        composable(Screen.GreenhousePro.route) {
            val greenhouseViewModel = hiltViewModel<GreenhouseViewModel>()
            val protocolMode by greenhouseViewModel.protocolMode.collectAsState()
            WrapApplicationBluetoothSession(
                applicationId = ApplicationId.GREENHOUSE,
                protocolMode = protocolMode,
                bluetoothViewModel = bluetoothViewModel,
                navController = navController,
            ) {
                GreenhouseScreen(
                    navController = navController,
                    viewModel = greenhouseViewModel,
                )
            }
        }
        composable(Screen.GreenhouseHelp.route) {
            GreenhouseHelpScreen(navController = navController)
        }
        composable(Screen.GreenhouseSettings.route) {
            GreenhouseSettingsScreen(navController = navController)
        }
        composable(Screen.GreenhouseCamera.route) {
            GreenhouseCameraScreen(navController = navController)
        }
        composable(Screen.SolarPro.route) {
            val solarViewModel = hiltViewModel<com.micsbol.telecon4esp32.ui.solarsystem.SolarSystemViewModel>()
            val protocolMode by solarViewModel.protocolMode.collectAsState()
            WrapApplicationBluetoothSession(
                applicationId = ApplicationId.SOLAR_POWER,
                protocolMode = protocolMode,
                bluetoothViewModel = bluetoothViewModel,
                navController = navController,
            ) {
                SolarSystemScreen(navController = navController, viewModel = solarViewModel)
            }
        }
        composable(Screen.SolarHelp.route) {
            SolarHelpScreen(navController = navController)
        }
        composable(Screen.SmartHomePro.route) {
            val smartHomeViewModel = hiltViewModel<com.micsbol.telecon4esp32.ui.smarthome.SmartHomeViewModel>()
            val protocolMode by smartHomeViewModel.protocolMode.collectAsState()
            WrapApplicationBluetoothSession(
                applicationId = ApplicationId.SMART_HOME,
                protocolMode = protocolMode,
                bluetoothViewModel = bluetoothViewModel,
                navController = navController,
            ) {
                SmartHomeScreen(navController = navController, viewModel = smartHomeViewModel)
            }
        }
        composable(Screen.SmartHomeHelp.route) {
            SmartHomeHelpScreen(navController = navController)
        }
        composable(Screen.WaterTankPro.route) {
            val waterTankViewModel = hiltViewModel<com.micsbol.telecon4esp32.ui.watertank.WaterTankViewModel>()
            val protocolMode by waterTankViewModel.protocolMode.collectAsState()
            WrapApplicationBluetoothSession(
                applicationId = ApplicationId.WATER_TANK,
                protocolMode = protocolMode,
                bluetoothViewModel = bluetoothViewModel,
                navController = navController,
            ) {
                WaterTankScreen(navController = navController, viewModel = waterTankViewModel)
            }
        }
        composable(Screen.WaterTankHelp.route) {
            WaterTankHelpScreen(navController = navController)
        }
        composable(Screen.SmartDoorLockPro.route) {
            val doorLockViewModel = hiltViewModel<com.micsbol.telecon4esp32.ui.smartdoorlock.SmartDoorLockViewModel>()
            val protocolMode by doorLockViewModel.protocolMode.collectAsState()
            WrapApplicationBluetoothSession(
                applicationId = ApplicationId.SMART_DOOR_LOCK,
                protocolMode = protocolMode,
                bluetoothViewModel = bluetoothViewModel,
                navController = navController,
            ) {
                SmartDoorLockScreen(navController = navController, viewModel = doorLockViewModel)
            }
        }
        composable(Screen.SmartDoorLockHelp.route) {
            SmartDoorLockHelpScreen(navController = navController)
        }
        composable(Screen.SmartLightingPro.route) {
            val lightingViewModel = hiltViewModel<com.micsbol.telecon4esp32.ui.smartlighting.SmartLightingViewModel>()
            val protocolMode by lightingViewModel.protocolMode.collectAsState()
            WrapApplicationBluetoothSession(
                applicationId = ApplicationId.SMART_LIGHTING,
                protocolMode = protocolMode,
                bluetoothViewModel = bluetoothViewModel,
                navController = navController,
            ) {
                SmartLightingScreen(navController = navController, viewModel = lightingViewModel)
            }
        }
        composable(Screen.SmartLightingHelp.route) {
            SmartLightingHelpScreen(navController = navController)
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
                onDisplayLabelsApplied = { draft ->
                    if (applicationId == ApplicationId.CONTROL_PANEL) {
                        bluetoothViewModel.applyDisplayLabelSettings(draft)
                    }
                },
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
        composable(Screen.Codes.route) {
            CodesHubScreen(navController = navController)
        }
        composable(
            route = Screen.ApplicationCodes.route,
            arguments = listOf(
                navArgument("applicationId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val applicationId = backStackEntry.arguments
                ?.getString("applicationId")
                ?.let { runCatching { ApplicationId.valueOf(it) }.getOrNull() }
                ?: return@composable
            CodesScreen(
                navController = navController,
                applicationId = applicationId,
            )
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