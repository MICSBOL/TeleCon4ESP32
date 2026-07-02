package com.micsbol.telecon4esp32.ui.applications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.applications.ApplicationSettingsIconButton
import com.micsbol.telecon4esp32.ui.components.NeoScaffold
import com.micsbol.telecon4esp32.ui.theme.Neo

@Composable
fun ProApplicationPlaceholderScreen(
    navController: NavController,
    applicationId: ApplicationId,
) {
    val catalogItem = defaultApplicationCatalog().first { it.id == applicationId }

    NeoScaffold(
        title = stringResource(catalogItem.titleRes),
        subtitle = stringResource(catalogItem.subtitleRes),
        onNavigateBack = { navController.navigateUp() },
        actions = {
            ApplicationSettingsIconButton(
                applicationId = applicationId,
                navController = navController,
            )
        },
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
                text = stringResource(R.string.pro_app_placeholder_title),
                color = Neo.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.pro_app_placeholder_body),
                color = Neo.TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
