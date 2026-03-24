package com.example.emitterapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.emitterapp.ui.navigation.AppNavGraph
import com.example.emitterapp.ui.theme.EmitterAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
}
