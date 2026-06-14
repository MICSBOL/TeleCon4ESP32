package com.micsbol.telecon4esp32.ui.applications

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.navigation.Screen

fun NavController.navigateToApplicationSettings(applicationId: ApplicationId) {
    navigate(Screen.ApplicationSettings.createRoute(applicationId))
}

fun applicationSettingsTitleRes(applicationId: ApplicationId): Int = when (applicationId) {
    ApplicationId.CONTROL_PANEL -> R.string.app_control_panel_settings_title
    ApplicationId.RC_VEHICLE_PRO -> R.string.app_rc_vehicle_settings_title
    ApplicationId.GREENHOUSE -> R.string.app_greenhouse_settings_title
    ApplicationId.SOLAR_POWER -> R.string.app_solar_settings_title
    ApplicationId.SMART_HOME -> R.string.app_smart_home_settings_title
    ApplicationId.CUSTOM_DASHBOARD -> R.string.app_custom_dashboard_settings_title
}

@Composable
fun ApplicationSettingsIconButton(
    applicationId: ApplicationId,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    IconButton(
        modifier = modifier,
        onClick = { navController.navigateToApplicationSettings(applicationId) },
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = stringResource(
                R.string.applications_settings_content_description,
                stringResource(applicationSettingsTitleRes(applicationId)),
            ),
            tint = brandPrimary(),
        )
    }
}
