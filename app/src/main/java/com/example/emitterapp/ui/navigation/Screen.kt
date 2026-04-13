package com.example.emitterapp.ui.navigation

import com.example.emitterapp.domain.model.RcUiStyle

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Bluetooth : Screen("bluetooth")
    object RcScreen : Screen("rc_screen")
    object RcScreenLedStyle : Screen("rc_screen_led_style")
    object TestBluetooth : Screen("test_bluetooth")
    object RcSettingsScreen : Screen("rc_settings")
    object Tutorial : Screen("tutorial")
    object Codes : Screen("codes")
}

/** Maps a domain style enum to its navigation route. Kept here so domain stays route-agnostic. */
fun RcUiStyle.toRoute(): String = when (this) {
    RcUiStyle.SCREEN_3D  -> Screen.RcScreen.route
    RcUiStyle.SCREEN_LED -> Screen.RcScreenLedStyle.route
}
