package com.gpsphotocamera.locationeditor

import android.app.Application

/**
 * Main application class for GPS Photo Camera.
 * Initializes application-level singletons, dependency injection,
 * or global configurations if required.
 */
class GpsPhotoCameraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialization hooks for logging, crashlytics, or analytics can be added here
    }
}
