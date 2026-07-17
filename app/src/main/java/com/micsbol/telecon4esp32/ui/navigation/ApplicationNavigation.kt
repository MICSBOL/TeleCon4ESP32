package com.micsbol.telecon4esp32.ui.navigation

import com.micsbol.telecon4esp32.domain.model.ApplicationId

fun ApplicationId.mainRoute(): String = when (this) {
    ApplicationId.CONTROL_PANEL -> Screen.ControlPanel.route
    ApplicationId.RC_VEHICLE_PRO -> Screen.RcVehiclePro.route
    ApplicationId.GREENHOUSE -> Screen.GreenhousePro.route
    ApplicationId.SOLAR_POWER -> Screen.SolarPro.route
    ApplicationId.SMART_HOME -> Screen.SmartHomePro.route
    ApplicationId.WATER_TANK -> Screen.WaterTankPro.route
    ApplicationId.SMART_DOOR_LOCK -> Screen.SmartDoorLockPro.route
    ApplicationId.SMART_LIGHTING -> Screen.SmartLightingPro.route
}

fun routeToApplicationId(route: String?): ApplicationId? = when (route) {
    Screen.ControlPanel.route -> ApplicationId.CONTROL_PANEL
    Screen.RcVehiclePro.route -> ApplicationId.RC_VEHICLE_PRO
    Screen.GreenhousePro.route,
    Screen.GreenhouseHelp.route,
    Screen.GreenhouseSettings.route,
    Screen.GreenhouseCamera.route,
    -> ApplicationId.GREENHOUSE
    Screen.SolarPro.route,
    Screen.SolarHelp.route,
    -> ApplicationId.SOLAR_POWER
    Screen.SmartHomePro.route,
    Screen.SmartHomeHelp.route,
    -> ApplicationId.SMART_HOME
    Screen.WaterTankPro.route,
    Screen.WaterTankHelp.route,
    -> ApplicationId.WATER_TANK
    Screen.SmartDoorLockPro.route,
    Screen.SmartDoorLockHelp.route,
    -> ApplicationId.SMART_DOOR_LOCK
    Screen.SmartLightingPro.route,
    Screen.SmartLightingHelp.route,
    -> ApplicationId.SMART_LIGHTING
    else -> null
}
