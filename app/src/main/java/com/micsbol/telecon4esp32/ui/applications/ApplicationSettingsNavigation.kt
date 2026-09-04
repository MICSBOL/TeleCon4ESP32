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

enum class ApplicationSettingsSection {
    CONNECTION,
    ;

    companion object {
        fun fromNav(value: String?): ApplicationSettingsSection =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: CONNECTION
    }
}

fun NavController.navigateToApplicationSettings(
    applicationId: ApplicationId,
    section: ApplicationSettingsSection = ApplicationSettingsSection.CONNECTION,
) {
    navigate(Screen.ApplicationSettings.createRoute(applicationId, section))
}

fun applicationSettingsTitleRes(applicationId: ApplicationId): Int = when (applicationId) {
    ApplicationId.CONTROL_PANEL -> R.string.app_control_panel_settings_title
    ApplicationId.RC_VEHICLE_PRO -> R.string.app_rc_vehicle_settings_title
}

fun controlPanelSettingsTitleRes(section: ApplicationSettingsSection): Int = when (section) {
    ApplicationSettingsSection.CONNECTION -> R.string.control_panel_connection_settings_title
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
