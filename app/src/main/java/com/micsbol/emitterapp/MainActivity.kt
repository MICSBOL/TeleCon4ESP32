package com.micsbol.emitterapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.micsbol.emitterapp.domain.bluetooth.RemoteController
import com.micsbol.emitterapp.ui.navigation.AppNavGraph
import com.micsbol.emitterapp.ui.theme.EmitterAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var remoteController: RemoteController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EmitterAppTheme {
                AppNavGraph()
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
