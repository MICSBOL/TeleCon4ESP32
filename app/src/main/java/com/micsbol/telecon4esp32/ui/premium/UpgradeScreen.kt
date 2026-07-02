package com.micsbol.telecon4esp32.ui.premium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.NeoCard
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.components.NeoScaffold
import com.micsbol.telecon4esp32.ui.components.NeoSectionTitle
import com.micsbol.telecon4esp32.ui.theme.Neo

@Composable
fun UpgradeScreen(navController: NavController) {
    NeoScaffold(
        title = stringResource(R.string.upgrade_title),
        subtitle = stringResource(R.string.upgrade_subtitle),
        onNavigateBack = { navController.navigateUp() },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NeoCard(modifier = Modifier.fillMaxWidth()) {
                NeoSectionTitle(text = stringResource(R.string.upgrade_benefits_title))
                Spacer(modifier = Modifier.height(12.dp))
                UpgradeBenefitLine(stringResource(R.string.upgrade_benefit_ad_free))
                UpgradeBenefitLine(stringResource(R.string.upgrade_benefit_rc_vehicle))
                UpgradeBenefitLine(stringResource(R.string.upgrade_benefit_applications))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.upgrade_billing_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = Neo.TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            NeoPillButton(
                text = stringResource(R.string.upgrade_purchase_button),
                onClick = { /* Play Billing — next implementation step */ },
                modifier = Modifier.fillMaxWidth(0.92f),
                enabled = false,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { /* restore purchases — next step */ }) {
                Text(
                    text = stringResource(R.string.upgrade_restore_button),
                    color = Neo.Accent,
                )
            }
        }
    }
}

@Composable
private fun UpgradeBenefitLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = Neo.TextPrimary,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}
