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
    )

    fun featureForRoute(route: String?): PremiumFeature? = routeToFeature[route]
}
