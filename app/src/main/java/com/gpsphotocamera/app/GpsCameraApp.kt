package com.gpsphotocamera.app

import android.app.Application

/**
 * Main Application class for GPS Photo Camera.
 * Initializes application-level singletons, repositories, and context.
 */
class GpsCameraApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: GpsCameraApp
            private set
    }
}
