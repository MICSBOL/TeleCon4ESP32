package com.micsbol.telecon4esp32.ui.navigation

import com.micsbol.telecon4esp32.domain.model.ApplicationId

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
    object GreenhouseHelp : Screen("greenhouse_help")
    object GreenhouseSettings : Screen("greenhouse_settings")
    object GreenhouseCamera : Screen("greenhouse_camera")
    object SolarPro : Screen("solar_pro")
    object SmartHomePro : Screen("smart_home_pro")
    object WaterTankPro : Screen("water_tank_pro")
    object SmartDoorLockPro : Screen("smart_door_lock_pro")
    object SmartLightingPro : Screen("smart_lighting_pro")
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
