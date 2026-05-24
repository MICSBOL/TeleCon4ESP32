package com.micsbol.emitterapp.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Bluetooth : Screen("bluetooth")
    object RcScreen : Screen("rc_screen")
    object RcSettingsScreen : Screen("rc_settings")
    object Tutorial : Screen("tutorial")
    object Codes : Screen("codes")
    object About : Screen("about")
    object PrivacyPolicy : Screen("privacy_policy")
}
