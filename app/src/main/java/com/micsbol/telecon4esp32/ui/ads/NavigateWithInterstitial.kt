package com.micsbol.telecon4esp32.ui.ads

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberNavigateWithInterstitial(
    trigger: InterstitialTrigger,
    onNavigate: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val adManager = LocalInterstitialAdManager.current

    return remember(trigger, onNavigate, activity, adManager) {
        {
            if (activity == null || adManager == null) {
                onNavigate()
            } else {
                adManager.tryShow(activity, trigger, onNavigate)
            }
        }
    }
}
