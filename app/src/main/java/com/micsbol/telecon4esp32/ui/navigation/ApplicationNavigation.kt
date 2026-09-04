package com.micsbol.telecon4esp32.ui.navigation

import com.micsbol.telecon4esp32.domain.model.ApplicationId

fun ApplicationId.mainRoute(): String = when (this) {
    ApplicationId.CONTROL_PANEL -> Screen.ControlPanel.route
    ApplicationId.RC_VEHICLE_PRO -> Screen.RcVehiclePro.route
}

fun routeToApplicationId(route: String?): ApplicationId? = when (route) {
    Screen.ControlPanel.route -> ApplicationId.CONTROL_PANEL
    Screen.RcVehiclePro.route -> ApplicationId.RC_VEHICLE_PRO
    else -> null
}
