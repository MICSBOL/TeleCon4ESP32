package com.micsbol.telecon4esp32.ui.navigation

import com.micsbol.telecon4esp32.domain.model.PremiumFeature

/**
 * Maps navigation routes to the pro application that owns them.
 *
 * Sub-routes (help, settings, camera) stay inside the same feature family so
 * one-use session grants are not cleared when opening in-app screens.
 */
object ProAppRoutes {

    private val routeToFeature = mapOf(
        Screen.RcVehiclePro.route to PremiumFeature.RC_VEHICLE_PRO,
        Screen.GreenhousePro.route to PremiumFeature.GREENHOUSE,
        Screen.GreenhouseHelp.route to PremiumFeature.GREENHOUSE,
        Screen.GreenhouseSettings.route to PremiumFeature.GREENHOUSE,
        Screen.GreenhouseCamera.route to PremiumFeature.GREENHOUSE,
        Screen.SolarPro.route to PremiumFeature.SOLAR_POWER,
        Screen.SolarHelp.route to PremiumFeature.SOLAR_POWER,
        Screen.SmartHomePro.route to PremiumFeature.SMART_HOME,
        Screen.SmartHomeHelp.route to PremiumFeature.SMART_HOME,
        Screen.WaterTankPro.route to PremiumFeature.WATER_TANK,
        Screen.WaterTankHelp.route to PremiumFeature.WATER_TANK,
        Screen.SmartDoorLockPro.route to PremiumFeature.SMART_DOOR_LOCK,
        Screen.SmartDoorLockHelp.route to PremiumFeature.SMART_DOOR_LOCK,
        Screen.SmartLightingPro.route to PremiumFeature.SMART_LIGHTING,
        Screen.SmartLightingHelp.route to PremiumFeature.SMART_LIGHTING,
    )

    fun featureForRoute(route: String?): PremiumFeature? = routeToFeature[route]
}
