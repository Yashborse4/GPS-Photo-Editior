package com.gpsphotocamera.app.data.model

/**
 * Map imagery styles supported for both the preview and generated overlay stamp.
 */
enum class AppMapType(val displayName: String) {
    SATELLITE("Satellite"),
    NORMAL("Standard"),
    TERRAIN("Terrain"),
    HYBRID("Hybrid")
}
