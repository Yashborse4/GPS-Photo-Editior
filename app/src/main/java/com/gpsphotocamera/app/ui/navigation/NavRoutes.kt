package com.gpsphotocamera.app.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Editor : Screen("editor?photoUri={photoUri}") {
        fun createRoute(photoUri: String): String {
            val encoded = Uri.encode(photoUri)
            return "editor?photoUri=$encoded"
        }
    }
    data object Preview : Screen("preview")
}
