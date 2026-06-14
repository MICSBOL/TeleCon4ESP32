package com.micsbol.telecon4esp32.ui.about

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.TeleCon4Esp32Scaffold
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PrivacyPolicyScreen(navController: NavController) {
    val policyUrl = if (Locale.getDefault().language == "es") {
        stringResource(R.string.privacy_policy_url_es)
    } else {
        stringResource(R.string.privacy_policy_url)
    }
    val rememberedPolicyUrl = remember(policyUrl) { policyUrl }

    TeleCon4Esp32Scaffold(
        title = stringResource(R.string.privacy_policy_title),
        onNavigateBack = { navController.navigateUp() },
    ) { paddingValues ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = false
                    loadUrl(rememberedPolicyUrl)
                }
            }
        )
    }
}
