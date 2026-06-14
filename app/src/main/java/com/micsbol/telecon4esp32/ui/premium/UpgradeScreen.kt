package com.micsbol.telecon4esp32.ui.premium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.TeleCon4Esp32Scaffold
import com.micsbol.telecon4esp32.ui.components.EmitterStyledCard
import com.micsbol.telecon4esp32.ui.components.mutedTextColor

@Composable
fun UpgradeScreen(navController: NavController) {
    TeleCon4Esp32Scaffold(
        title = stringResource(R.string.upgrade_title),
        subtitle = stringResource(R.string.upgrade_subtitle),
        onNavigateBack = { navController.navigateUp() },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EmitterStyledCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.upgrade_benefits_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    UpgradeBenefitLine(stringResource(R.string.upgrade_benefit_ad_free))
                    UpgradeBenefitLine(stringResource(R.string.upgrade_benefit_rc_vehicle))
                    UpgradeBenefitLine(stringResource(R.string.upgrade_benefit_applications))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.upgrade_billing_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = mutedTextColor(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { /* Play Billing — next implementation step */ },
                modifier = Modifier.fillMaxWidth(0.92f),
                enabled = false,
            ) {
                Text(stringResource(R.string.upgrade_purchase_button))
            }
            TextButton(onClick = { /* restore purchases — next step */ }) {
                Text(stringResource(R.string.upgrade_restore_button))
            }
        }
    }
}

@Composable
private fun UpgradeBenefitLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}
