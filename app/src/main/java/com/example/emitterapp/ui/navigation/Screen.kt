package com.example.emitterapp.ui.navigation

sealed class Screen(val route: String) {
    object Splash: Screen("splash")
    object Home: Screen("home")
    object Bluetooth: Screen("bluetooth")
    object RcScreen: Screen("rc_screen")
    object TestBluetooth: Screen("test_bluetooth")
    object RcStettingScreen: Screen("rc_settings")
}