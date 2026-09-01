package com.micsbol.telecon4esp32.ui.navigation

import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.isApplicationCatalogVisible
import com.micsbol.telecon4esp32.ui.applications.ApplicationSettingsSection

/** Home when Catalog is hidden; Catalog once enough modules ship. */
fun modulesHubRoute(): String =
    if (isApplicationCatalogVisible()) Screen.Applications.route else Screen.Home.route

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Applications : Screen("applications")
    object Bluetooth : Screen("bluetooth")
    object ControlPanel : Screen("control_panel")
    object RcSettingsScreen : Screen("rc_settings")
    object ApplicationSettings : Screen("app_settings/{applicationId}?section={section}") {
        fun createRoute(
            applicationId: ApplicationId,
            section: ApplicationSettingsSection = ApplicationSettingsSection.CONNECTION,
        ): String = "app_settings/${applicationId.name}?section=${section.name}"
    }
    object RcVehiclePro : Screen("rc_vehicle_pro")
    object GreenhousePro : Screen("greenhouse_pro")
    object GreenhouseHelp : Screen("greenhouse_help")
    object GreenhouseSettings : Screen("greenhouse_settings")
    object GreenhouseCamera : Screen("greenhouse_camera")
    object SolarPro : Screen("solar_pro")
    object SolarHelp : Screen("solar_help")
    object SmartHomePro : Screen("smart_home_pro")
    object SmartHomeHelp : Screen("smart_home_help")
    object WaterTankPro : Screen("water_tank_pro")
    object WaterTankHelp : Screen("water_tank_help")
    object SmartDoorLockPro : Screen("smart_door_lock_pro")
    object SmartDoorLockHelp : Screen("smart_door_lock_help")
    object SmartLightingPro : Screen("smart_lighting_pro")
    object SmartLightingHelp : Screen("smart_lighting_help")
    object Upgrade : Screen("upgrade")
    object Tutorial : Screen("tutorial")
    object Codes : Screen("codes")
    object ApplicationCodes : Screen("application_codes/{applicationId}") {
        fun createRoute(applicationId: ApplicationId): String =
            "application_codes/${applicationId.name}"
    }
    object About : Screen("about")
    object PrivacyPolicy : Screen("privacy_policy")

    object ProPlaceholder : Screen("pro_placeholder/{applicationId}") {
        fun createRoute(applicationId: ApplicationId): String =
            "pro_placeholder/${applicationId.name}"
    }
}
