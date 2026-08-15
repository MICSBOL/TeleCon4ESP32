package com.micsbol.telecon4esp32.ui.applications

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.isShipped
import com.micsbol.telecon4esp32.ui.navigation.Screen

data class ApplicationCatalogItem(
    val id: ApplicationId,
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: ImageVector,
    val route: String,
    val comingSoon: Boolean = false,
)

/** Full catalog definition; restore deferred apps by flipping [ApplicationId.isShipped]. */
fun allApplicationCatalogItems(): List<ApplicationCatalogItem> = listOf(
    ApplicationCatalogItem(
        id = ApplicationId.CONTROL_PANEL,
        titleRes = com.micsbol.telecon4esp32.R.string.app_control_panel_title,
        subtitleRes = com.micsbol.telecon4esp32.R.string.app_control_panel_subtitle,
        icon = Icons.Default.Tune,
        route = Screen.ControlPanel.route,
    ),
    ApplicationCatalogItem(
        id = ApplicationId.RC_VEHICLE_PRO,
        titleRes = com.micsbol.telecon4esp32.R.string.app_rc_vehicle_title,
        subtitleRes = com.micsbol.telecon4esp32.R.string.app_rc_vehicle_subtitle,
        icon = Icons.Default.SportsEsports,
        route = Screen.RcVehiclePro.route,
    ),
    ApplicationCatalogItem(
        id = ApplicationId.GREENHOUSE,
        titleRes = com.micsbol.telecon4esp32.R.string.app_greenhouse_title,
        subtitleRes = com.micsbol.telecon4esp32.R.string.app_greenhouse_subtitle,
        icon = Icons.Default.Eco,
        route = Screen.GreenhousePro.route,
    ),
    ApplicationCatalogItem(
        id = ApplicationId.SOLAR_POWER,
        titleRes = com.micsbol.telecon4esp32.R.string.app_solar_title,
        subtitleRes = com.micsbol.telecon4esp32.R.string.app_solar_subtitle,
        icon = Icons.Default.SolarPower,
        route = Screen.SolarPro.route,
    ),
    ApplicationCatalogItem(
        id = ApplicationId.SMART_HOME,
        titleRes = com.micsbol.telecon4esp32.R.string.app_smart_home_title,
        subtitleRes = com.micsbol.telecon4esp32.R.string.app_smart_home_subtitle,
        icon = Icons.Default.Home,
        route = Screen.SmartHomePro.route,
    ),
    ApplicationCatalogItem(
        id = ApplicationId.WATER_TANK,
        titleRes = com.micsbol.telecon4esp32.R.string.app_water_tank_title,
        subtitleRes = com.micsbol.telecon4esp32.R.string.app_water_tank_subtitle,
        icon = Icons.Default.WaterDrop,
        route = Screen.WaterTankPro.route,
    ),
    ApplicationCatalogItem(
        id = ApplicationId.SMART_DOOR_LOCK,
        titleRes = com.micsbol.telecon4esp32.R.string.app_smart_door_lock_title,
        subtitleRes = com.micsbol.telecon4esp32.R.string.app_smart_door_lock_subtitle,
        icon = Icons.Default.Lock,
        route = Screen.SmartDoorLockPro.route,
    ),
    ApplicationCatalogItem(
        id = ApplicationId.SMART_LIGHTING,
        titleRes = com.micsbol.telecon4esp32.R.string.app_smart_lighting_title,
        subtitleRes = com.micsbol.telecon4esp32.R.string.app_smart_lighting_subtitle,
        icon = Icons.Default.Lightbulb,
        route = Screen.SmartLightingPro.route,
    ),
)

/** Catalog shown in Modules / Codes hub (shipped apps only). */
fun defaultApplicationCatalog(): List<ApplicationCatalogItem> =
    allApplicationCatalogItems().filter { it.id.isShipped() }
