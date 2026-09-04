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
    object Upgrade : Screen("upgrade")
    object Tutorial : Screen("tutorial")
    object Codes : Screen("codes")
    object ApplicationCodes : Screen("application_codes/{applicationId}") {
        fun createRoute(applicationId: ApplicationId): String =
            "application_codes/${applicationId.name}"
    }
    object About : Screen("about")
    object PrivacyPolicy : Screen("privacy_policy")
}
