package com.example.emitterapp.ui.rc_screen.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            // Create an AdView
            AdView(context).apply {
                // Set the ad size. Here, we're using an adaptive banner.
                setAdSize(AdSize.BANNER)

                // Use Google's sample Ad Unit ID for testing.
                // Replace this with your own Ad Unit ID from your AdMob account before publishing.
                adUnitId = "ca-app-pub-3940256099942544/6300978111"

                // Create an ad request and load the ad.
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}