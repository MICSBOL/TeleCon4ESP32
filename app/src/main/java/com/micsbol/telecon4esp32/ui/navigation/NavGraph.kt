package com.micsbol.telecon4esp32.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.isApplicationCatalogVisible
import com.micsbol.telecon4esp32.domain.model.isShipped
import com.micsbol.telecon4esp32.ui.bluetooth.WrapApplicationBluetoothSession
import com.micsbol.telecon4esp32.ui.about.AboutScreen
import com.micsbol.telecon4esp32.ui.about.PrivacyPolicyScreen
import com.micsbol.telecon4esp32.ui.applications.ApplicationSettingsHostScreen
import com.micsbol.telecon4esp32.ui.applications.ApplicationSettingsSection
import com.micsbol.telecon4esp32.ui.applications.ApplicationsScreen
import com.micsbol.telecon4esp32.ui.applications.usesImmersiveHudChrome
import com.micsbol.telecon4esp32.ui.bluetooth.BluetoothScreen
import com.micsbol.telecon4esp32.ui.bluetooth.BluetoothViewModel
import com.micsbol.telecon4esp32.ui.bluetooth.rememberEnsureBluetoothReady
import com.micsbol.telecon4esp32.ui.codes.CodesHubScreen
import com.micsbol.telecon4esp32.ui.codes.CodesScreen
import com.micsbol.telecon4esp32.ui.components.LocalHudGlassDialog
import com.micsbol.telecon4esp32.ui.components.LocalHudSystemBarsHidden
import com.micsbol.telecon4esp32.ui.control_panel.ControlPanelCameraStreamViewModel
import com.micsbol.telecon4esp32.ui.control_panel.ControlPanelScreen
import com.micsbol.telecon4esp32.ui.control_panel.HideHudSystemBars
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

            CyberHomeScreen(
                navController = navController,
                isConnecting = state.isConnecting,
                activeSession = state.activeSession,
                lastApplicationId = lastApplicationId,
                errorMessage = state.errorMessage,
                connectFailure = state.connectFailure,
                handshakeFailure = state.handshakeFailure,
                onDismissError = bluetoothViewModel::dismissError,
                onOpenApplications = { navController.navigate(Screen.Applications.route) },
            )
        }
        composable(Screen.Bluetooth.route) {
            val state by bluetoothViewModel.state.collectAsState()
            val ensureBluetoothReady = rememberEnsureBluetoothReady()

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

            LaunchedEffect(Unit) {
                ensureBluetoothReady {
                    bluetoothViewModel.startScan()
                }
            }

            val lastApplicationId by bluetoothViewModel.lastApplicationId.collectAsState()
            val immersiveHud = lastApplicationId?.usesImmersiveHudChrome() == true
            if (immersiveHud) {
                HideHudSystemBars()
            }
            CompositionLocalProvider(
                LocalHudSystemBarsHidden provides immersiveHud,
                LocalHudGlassDialog provides immersiveHud,
            ) {
                BluetoothScreen(
                    state = state,
                    onNavigateBack = { navController.navigateUp() },
                    onStartScan = {
                        ensureBluetoothReady {
                            bluetoothViewModel.startScan()
                        }
                    },
                    onStopScan = bluetoothViewModel::stopScan,
                    onDismissError = bluetoothViewModel::dismissError,
                    onDeviceClick = { device ->
                        ensureBluetoothReady {
                            bluetoothViewModel.connectToDevice(device)
                        }
                    }
                )
            }
        }
        composable(Screen.Applications.route) {
            if (!isApplicationCatalogVisible()) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Applications.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } else {
                ApplicationsScreen(navController = navController)
            }
        }
        composable(Screen.ControlPanel.route) {
            ControlPanelScreen(
                bluetoothViewModel = bluetoothViewModel,
                navController = navController,
                cameraStreamViewModel = hiltViewModel<ControlPanelCameraStreamViewModel>(),
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
        composable(Screen.Upgrade.route) {
            UpgradeScreen(navController = navController)
        }
        composable(
            route = Screen.ApplicationSettings.route,
            arguments = listOf(
                navArgument("applicationId") { type = NavType.StringType },
                navArgument("section") {
                    type = NavType.StringType
                    defaultValue = ApplicationSettingsSection.CONNECTION.name
                    nullable = true
                },
            ),
        ) { backStackEntry ->
            val applicationId = backStackEntry.arguments
                ?.getString("applicationId")
                ?.let { runCatching { ApplicationId.valueOf(it) }.getOrNull() }
                ?.takeIf { it.isShipped() }
                ?: return@composable
            val section = ApplicationSettingsSection.fromNav(
                backStackEntry.arguments?.getString("section"),
            )
            ApplicationSettingsHostScreen(
                navController = navController,
                applicationId = applicationId,
                settingsSection = section,
                settingsViewModel = settingsViewModel,
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