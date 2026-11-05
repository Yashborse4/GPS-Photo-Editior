package com.gpsphotocamera.locationeditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gpsphotocamera.locationeditor.ui.screen.LocationEditorScreen
import com.gpsphotocamera.locationeditor.ui.theme.GPSPhotoCameraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GPSPhotoCameraTheme {
                LocationEditorScreen()
            }
        }
    }
}
