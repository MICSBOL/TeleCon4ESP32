package com.micsbol.telecon4esp32

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteController
import com.micsbol.telecon4esp32.ui.ads.InterstitialAdManager
import com.micsbol.telecon4esp32.ui.ads.LocalInterstitialAdManager
import com.micsbol.telecon4esp32.ui.ads.LocalRewardedAdManager
import com.micsbol.telecon4esp32.ui.ads.RewardedAdManager
import com.micsbol.telecon4esp32.ui.navigation.AppNavGraph
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var remoteController: RemoteController

    @Inject
    lateinit var interstitialAdManager: InterstitialAdManager

    @Inject
    lateinit var rewardedAdManager: RewardedAdManager

    private var keepSystemSplash = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSystemSplash }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TeleCon4Esp32Theme {
                CompositionLocalProvider(
                    LocalInterstitialAdManager provides interstitialAdManager,
                    LocalRewardedAdManager provides rewardedAdManager,
                ) {
                    AppNavGraph(
                        onComposeSplashReady = {
                            keepSystemSplash = false
                            window.decorView.post { window.decorView.invalidate() }
                        },
                    )
                }
            }
        }

        // Force window refresh on MIUI: some builds defer Compose rendering until
        // invalidation or interaction. Scheduling these ensures first frame draws.
        window.decorView.post {
            window.decorView.invalidate()
        }
    }

    override fun onStop() {
        super.onStop()
        // Scans should not keep running while app UI is no longer visible.
        remoteController.stopDiscovery()
    }

    override fun onDestroy() {
        // Release socket/resources when app is really finishing (Back/Recents close).
        if (isFinishing) {
            remoteController.release()
        }
        super.onDestroy()
    }
}
