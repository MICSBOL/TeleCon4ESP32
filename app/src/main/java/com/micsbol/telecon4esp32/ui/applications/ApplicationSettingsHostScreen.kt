package com.micsbol.telecon4esp32.ui.applications

import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.control_panel.LockScreenOrientation
import com.micsbol.telecon4esp32.ui.rc_settings.RcSettingsScreen
import com.micsbol.telecon4esp32.ui.rc_settings.SettingsViewModel

@Composable
fun ApplicationSettingsHostScreen(
    navController: NavController,
    applicationId: ApplicationId,
    settingsViewModel: SettingsViewModel,
    settingsSection: ApplicationSettingsSection = ApplicationSettingsSection.CONNECTION,
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT)
    val applicationSettingsViewModel: ApplicationSettingsViewModel = hiltViewModel()

    when (applicationId) {
        ApplicationId.CONTROL_PANEL -> {
            RcSettingsScreen(
                navController = navController,
                viewModel = settingsViewModel,
                applicationId = applicationId,
                settingsSection = settingsSection,
                applicationSettingsViewModel = applicationSettingsViewModel,
            )
        }
        else -> {
            ApplicationProtocolSettingsScreen(
                navController = navController,
                applicationId = applicationId,
                viewModel = applicationSettingsViewModel,
            )
        }
    }
}
