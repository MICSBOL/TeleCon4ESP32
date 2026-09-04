package com.micsbol.telecon4esp32.ui.applications

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
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
)

/** Catalog shown in Modules / Codes hub (shipped apps only). */
fun defaultApplicationCatalog(): List<ApplicationCatalogItem> =
    allApplicationCatalogItems().filter { it.id.isShipped() }
