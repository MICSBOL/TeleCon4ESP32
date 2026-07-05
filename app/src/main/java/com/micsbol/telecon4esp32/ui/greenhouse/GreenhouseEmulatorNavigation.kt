package com.micsbol.telecon4esp32.ui.greenhouse

import androidx.navigation.NavController
import com.micsbol.telecon4esp32.ui.navigation.Screen

/**
 * On low-RAM emulators, drop the main greenhouse route before opening a sub-screen so the
 * photo background, chart, and ViewModel are released before the next screen composes.
 */
internal object GreenhouseEmulatorNavigation {
    fun openSubScreen(navController: NavController, route: String) {
        if (GreenhouseEmulatorSupport.isEmulator()) {
            navController.navigate(route) {
                popUpTo(Screen.GreenhousePro.route) { inclusive = true }
            }
        } else {
            navController.navigate(route)
        }
    }

    fun backToGreenhouse(navController: NavController, subRoute: String) {
        if (GreenhouseEmulatorSupport.isEmulator()) {
            navController.navigate(Screen.GreenhousePro.route) {
                popUpTo(subRoute) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            navController.navigateUp()
        }
    }
}
