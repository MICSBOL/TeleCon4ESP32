package com.micsbol.telecon4esp32.ui.applications

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.rc_settings.DisplayLabelDraft
import com.micsbol.telecon4esp32.ui.rc_settings.RcSettingsScreen
import com.micsbol.telecon4esp32.ui.rc_settings.SettingsViewModel

@Composable
fun ApplicationSettingsHostScreen(
    navController: NavController,
    applicationId: ApplicationId,
    settingsViewModel: SettingsViewModel,
    onDisplayLabelsApplied: (DisplayLabelDraft) -> Unit = {},
) {
    val applicationSettingsViewModel: ApplicationSettingsViewModel = hiltViewModel()

    when (applicationId) {
        ApplicationId.CONTROL_PANEL -> {
            RcSettingsScreen(
                navController = navController,
                viewModel = settingsViewModel,
                applicationId = applicationId,
                applicationSettingsViewModel = applicationSettingsViewModel,
                onDisplayLabelsApplied = onDisplayLabelsApplied,
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
