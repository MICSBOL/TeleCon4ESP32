package com.micsbol.emitterapp.ui.applications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.micsbol.emitterapp.R
import com.micsbol.emitterapp.domain.model.ApplicationId
import com.micsbol.emitterapp.ui.components.EmitterAppScaffold
import com.micsbol.emitterapp.ui.components.mutedTextColor
import com.micsbol.emitterapp.ui.rc_settings.RcSettingsScreen
import com.micsbol.emitterapp.ui.rc_settings.SettingsViewModel

@Composable
fun ApplicationSettingsHostScreen(
    navController: NavController,
    applicationId: ApplicationId,
    settingsViewModel: SettingsViewModel,
) {
    when (applicationId) {
        ApplicationId.CONTROL_PANEL -> {
            RcSettingsScreen(
                navController = navController,
                viewModel = settingsViewModel,
                applicationId = applicationId,
            )
        }
        else -> {
            ApplicationSettingsPlaceholderScreen(
                navController = navController,
                applicationId = applicationId,
            )
        }
    }
}

@Composable
fun ApplicationSettingsPlaceholderScreen(
    navController: NavController,
    applicationId: ApplicationId,
) {
    val catalogItem = defaultApplicationCatalog().first { it.id == applicationId }

    EmitterAppScaffold(
        title = stringResource(applicationSettingsTitleRes(applicationId)),
        subtitle = stringResource(catalogItem.titleRes),
        onNavigateBack = { navController.navigateUp() },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.app_settings_placeholder_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.app_settings_placeholder_body),
                style = MaterialTheme.typography.bodyMedium,
                color = mutedTextColor(),
                textAlign = TextAlign.Center,
            )
        }
    }
}
