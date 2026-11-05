package com.gpsphotocamera.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gpsphotocamera.app.ui.navigation.AppNavHost
import com.gpsphotocamera.app.ui.theme.GpsPhotoCameraTheme

/**
 * Main Activity hosting the Jetpack Compose navigation graph.
 * Supports edge-to-edge display and dynamic theming.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GpsPhotoCameraTheme {
                AppNavHost()
            }
        }
    }
}
