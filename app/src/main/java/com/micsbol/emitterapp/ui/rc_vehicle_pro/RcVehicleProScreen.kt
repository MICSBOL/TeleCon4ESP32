package com.micsbol.emitterapp.ui.rc_vehicle_pro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
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
import com.micsbol.emitterapp.ui.applications.ApplicationSettingsIconButton
import com.micsbol.emitterapp.ui.components.EmitterAppScaffold
import com.micsbol.emitterapp.ui.components.brandPrimary
import com.micsbol.emitterapp.ui.components.mutedTextColor

@Composable
fun RcVehicleProScreen(
    navController: NavController,
) {
    EmitterAppScaffold(
        title = stringResource(R.string.app_rc_vehicle_title),
        subtitle = stringResource(R.string.app_rc_vehicle_subtitle),
        onNavigateBack = { navController.navigateUp() },
        actions = {
            ApplicationSettingsIconButton(
                applicationId = ApplicationId.RC_VEHICLE_PRO,
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
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                tint = brandPrimary(),
                modifier = Modifier.height(64.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.rc_vehicle_pro_placeholder_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.rc_vehicle_pro_placeholder_body),
                style = MaterialTheme.typography.bodyMedium,
                color = mutedTextColor(),
                textAlign = TextAlign.Center,
            )
        }
    }
}
