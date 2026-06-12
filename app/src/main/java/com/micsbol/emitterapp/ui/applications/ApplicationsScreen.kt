package com.micsbol.emitterapp.ui.applications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.micsbol.emitterapp.R
import com.micsbol.emitterapp.domain.model.Entitlement
import com.micsbol.emitterapp.domain.model.has
import com.micsbol.emitterapp.domain.model.isFree
import com.micsbol.emitterapp.domain.model.premiumFeature
import com.micsbol.emitterapp.ui.components.EmitterAppScaffold
import com.micsbol.emitterapp.ui.entitlement.LocalEntitlement
import com.micsbol.emitterapp.ui.navigation.Screen

@Composable
fun ApplicationsScreen(navController: NavController) {
    val entitlement = LocalEntitlement.current
    val catalog = remember { defaultApplicationCatalog() }
    var comingSoonAppName by remember { mutableStateOf<String?>(null) }

    EmitterAppScaffold(
        title = stringResource(R.string.applications_title),
        subtitle = stringResource(R.string.applications_subtitle),
        onNavigateBack = { navController.navigateUp() },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(catalog, key = { it.id.name }) { item ->
                val title = stringResource(item.titleRes)
                ApplicationListItemCard(
                    icon = item.icon,
                    title = title,
                    subtitle = stringResource(item.subtitleRes),
                    badge = applicationBadge(item, entitlement),
                    onClick = {
                        handleApplicationClick(
                            item = item,
                            entitlement = entitlement,
                            navController = navController,
                            onComingSoon = { comingSoonAppName = title },
                        )
                    },
                    settingsContentDescription = stringResource(
                        R.string.applications_settings_content_description,
                        stringResource(applicationSettingsTitleRes(item.id)),
                    ),
                    onSettingsClick = {
                        navController.navigateToApplicationSettings(item.id)
                    },
                )
            }
        }
    }

    if (comingSoonAppName != null) {
        AlertDialog(
            onDismissRequest = { comingSoonAppName = null },
            title = { Text(stringResource(R.string.applications_coming_soon_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.applications_coming_soon_message,
                        comingSoonAppName.orEmpty(),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { comingSoonAppName = null }) {
                    Text(stringResource(R.string.codes_dialog_ok))
                }
            },
        )
    }
}

private fun handleApplicationClick(
    item: ApplicationCatalogItem,
    entitlement: Entitlement,
    navController: NavController,
    onComingSoon: () -> Unit,
) {
    when {
        item.id.isFree() -> navController.navigate(item.route)
        item.comingSoon -> onComingSoon()
        else -> {
            val feature = item.id.premiumFeature()
            if (feature != null && entitlement.has(feature)) {
                navController.navigate(item.route)
            } else {
                navController.navigate(Screen.Upgrade.route)
            }
        }
    }
}

@Composable
private fun applicationBadge(
    item: ApplicationCatalogItem,
    entitlement: Entitlement,
): String {
    return when {
        item.id.isFree() -> stringResource(R.string.applications_badge_free)
        item.comingSoon -> stringResource(R.string.applications_badge_coming_soon)
        else -> {
            val feature = item.id.premiumFeature()
            if (feature != null && entitlement.has(feature)) {
                stringResource(R.string.applications_badge_pro_unlocked)
            } else {
                stringResource(R.string.applications_badge_pro)
            }
        }
    }
}
