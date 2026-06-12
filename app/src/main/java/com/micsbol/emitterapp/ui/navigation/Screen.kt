package com.micsbol.emitterapp.ui.navigation

import com.micsbol.emitterapp.domain.model.ApplicationId

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Applications : Screen("applications")
    object Bluetooth : Screen("bluetooth")
    object ControlPanel : Screen("control_panel")
    object RcSettingsScreen : Screen("rc_settings")
    object ApplicationSettings : Screen("app_settings/{applicationId}") {
        fun createRoute(applicationId: ApplicationId): String =
            "app_settings/${applicationId.name}"
    }
    object RcVehiclePro : Screen("rc_vehicle_pro")
    object GreenhousePro : Screen("greenhouse_pro")
    object SolarPro : Screen("solar_pro")
    object SmartHomePro : Screen("smart_home_pro")
    object CustomDashboardPro : Screen("custom_dashboard_pro")
    object Upgrade : Screen("upgrade")
    object Tutorial : Screen("tutorial")
    object Codes : Screen("codes")
    object About : Screen("about")
    object PrivacyPolicy : Screen("privacy_policy")

    object ProPlaceholder : Screen("pro_placeholder/{applicationId}") {
        fun createRoute(applicationId: ApplicationId): String =
            "pro_placeholder/${applicationId.name}"
    }
}
