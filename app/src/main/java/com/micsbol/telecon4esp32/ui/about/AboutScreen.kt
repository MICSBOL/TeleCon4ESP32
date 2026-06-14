package com.micsbol.telecon4esp32.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.BuildConfig
import java.util.Locale
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.util.hostedPdfUrl
import com.micsbol.telecon4esp32.ui.components.TeleCon4Esp32Scaffold
import com.micsbol.telecon4esp32.ui.components.EmitterSectionTitle
import com.micsbol.telecon4esp32.ui.components.EmitterStyledCard
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.components.mutedTextColor
import com.micsbol.telecon4esp32.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
    val privacyPolicyUrl = if (Locale.getDefault().language == "es") {
        stringResource(R.string.privacy_policy_url_es)
    } else {
        stringResource(R.string.privacy_policy_url)
    }
    val documentationHomeUrl = stringResource(R.string.documentation_base_url)
    val documentationPdfEnUrl = stringResource(R.string.documentation_pdf_en_url)
    val documentationPdfEsUrl = stringResource(R.string.documentation_pdf_es_url)
    val documentationRepositoryUrl = stringResource(R.string.documentation_repository_url)

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // No browser available.
        }
    }

    fun openPrivacyPolicyInBrowser() = openUrl(privacyPolicyUrl)

    TeleCon4Esp32Scaffold(
        title = stringResource(R.string.about_title),
        subtitle = stringResource(R.string.about_subtitle),
        onNavigateBack = { navController.navigateUp() },
        actions = {
            IconButton(onClick = { openPrivacyPolicyInBrowser() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = stringResource(R.string.about_open_in_browser),
                    tint = brandPrimary()
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.about_version_label, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodyMedium,
                color = mutedTextColor()
            )
            Spacer(modifier = Modifier.height(20.dp))
            EmitterSectionTitle(text = stringResource(R.string.about_legal_section_title))
            EmitterStyledCard(
                modifier = Modifier.clickable {
                    navController.navigate(Screen.PrivacyPolicy.route)
                }
            ) {
                AboutLinkRow(
                    title = stringResource(R.string.about_privacy_policy),
                    subtitle = stringResource(R.string.about_privacy_policy_hint),
                    icon = Icons.Default.Policy
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(
                onClick = { openPrivacyPolicyInBrowser() },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = brandPrimary(),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.about_open_in_browser),
                        color = brandPrimary()
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            EmitterSectionTitle(text = stringResource(R.string.about_documentation_section_title))
            EmitterStyledCard(
                modifier = Modifier.clickable { openUrl(documentationHomeUrl) }
            ) {
                AboutLinkRow(
                    title = stringResource(R.string.about_documentation_home),
                    subtitle = stringResource(R.string.about_documentation_home_hint),
                    icon = Icons.Default.Description
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            EmitterStyledCard(
                modifier = Modifier.clickable { openUrl(hostedPdfUrl(documentationPdfEnUrl)) }
            ) {
                AboutLinkRow(
                    title = stringResource(R.string.about_documentation_en),
                    subtitle = stringResource(R.string.about_documentation_en_hint),
                    icon = Icons.Default.Description
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            EmitterStyledCard(
                modifier = Modifier.clickable { openUrl(hostedPdfUrl(documentationPdfEsUrl)) }
            ) {
                AboutLinkRow(
                    title = stringResource(R.string.about_documentation_es),
                    subtitle = stringResource(R.string.about_documentation_es_hint),
                    icon = Icons.Default.Description
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            EmitterStyledCard(
                modifier = Modifier.clickable { openUrl(documentationRepositoryUrl) }
            ) {
                AboutLinkRow(
                    title = stringResource(R.string.about_documentation_repository),
                    subtitle = stringResource(R.string.about_documentation_repository_hint),
                    icon = Icons.Default.Source
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            EmitterSectionTitle(text = stringResource(R.string.about_data_section_title))
            EmitterStyledCard {
                Text(
                    text = stringResource(R.string.about_data_section_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedTextColor()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.about_contact_label, stringResource(R.string.about_contact_email)),
                style = MaterialTheme.typography.bodySmall,
                color = mutedTextColor()
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AboutLinkRow(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Default.Policy
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = brandPrimary(),
            modifier = Modifier.size(28.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = mutedTextColor()
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = mutedTextColor(),
            modifier = Modifier.size(24.dp)
        )
    }
}
