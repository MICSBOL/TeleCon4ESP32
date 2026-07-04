package com.micsbol.telecon4esp32.ui.greenhouse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.protocolPrefix
import com.micsbol.telecon4esp32.ui.applications.applicationSettingsTitleRes
import com.micsbol.telecon4esp32.ui.greenhouse.components.GreenhouseCard
import com.micsbol.telecon4esp32.ui.greenhouse.components.GreenhouseSubScreenTopBar

@Composable
fun GreenhouseSettingsScreen(
    navController: NavController,
) {
    val edgeInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
    )
    val bottomInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)

    GreenhouseBackground(showPhoto = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(edgeInsets)
                .windowInsetsPadding(bottomInsets),
        ) {
            GreenhouseSubScreenTopBar(
                title = stringResource(applicationSettingsTitleRes(ApplicationId.GREENHOUSE)),
                subtitle = stringResource(R.string.greenhouse_settings_subtitle),
                onBackClick = { navController.navigateUp() },
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GreenhouseCard(elevated = false) {
                    Text(
                        text = stringResource(R.string.app_settings_protocol_section_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenhouseGlass.TextOnGlassPrimary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.app_settings_protocol_prefix_label,
                            ApplicationId.GREENHOUSE.protocolPrefix(),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = GreenhouseGlass.TextOnGlassSecondary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.app_settings_protocol_section_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = GreenhouseGlass.TextOnGlassSecondary,
                    )
                }
                GreenhouseCard(elevated = false) {
                    Text(
                        text = stringResource(R.string.app_settings_protocol_simple),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenhouseGlass.TextOnGlassPrimary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.app_settings_protocol_simple_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = GreenhouseGlass.TextOnGlassSecondary,
                    )
                }
            }
        }
    }
}
