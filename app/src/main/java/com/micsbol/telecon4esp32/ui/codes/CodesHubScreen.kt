package com.micsbol.telecon4esp32.ui.codes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.ads.InterstitialTrigger
import com.micsbol.telecon4esp32.ui.ads.rememberNavigateWithInterstitial
import com.micsbol.telecon4esp32.ui.applications.defaultApplicationCatalog
import com.micsbol.telecon4esp32.ui.components.NeoIconBadge
import com.micsbol.telecon4esp32.ui.components.NeoScaffold
import com.micsbol.telecon4esp32.ui.components.glassSurface
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.theme.Neo

@Composable
fun CodesHubScreen(navController: NavController) {
    val catalog = remember { defaultApplicationCatalog() }
    val language = remember { currentCodeAssetLanguage() }

    val navigateBackToHome = rememberNavigateWithInterstitial(
        trigger = InterstitialTrigger.CODES_EXIT,
        onNavigate = { navController.navigateUp() },
    )

    BackHandler(onBack = navigateBackToHome)

    NeoScaffold(
        title = stringResource(R.string.codes_and_documents_title),
        onNavigateBack = navigateBackToHome,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(catalog, key = { it.id.name }) { item ->
                val documentCount = remember(item.id, language) {
                    codeAssetsFor(item.id, language).size
                }
                CodesHubListItem(
                    icon = item.icon,
                    title = stringResource(item.titleRes),
                    subtitle = codesHubSubtitle(documentCount),
                    onClick = {
                        navController.navigate(Screen.ApplicationCodes.createRoute(item.id))
                    },
                )
            }
        }
    }
}

@Composable
private fun codesHubSubtitle(documentCount: Int): String {
    return if (documentCount > 0) {
        pluralStringResource(
            R.plurals.codes_hub_item_subtitle,
            documentCount,
            documentCount,
        )
    } else {
        stringResource(R.string.codes_hub_no_documents)
    }
}

@Composable
private fun CodesHubListItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NeoIconBadge(icon = icon, size = 44.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Neo.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = Neo.Positive,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Neo.TextSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}
