package com.gpsphotocamera.app.data.model

/**
 * Map imagery styles supported for both the preview and generated overlay stamp.
 * Features OpenStreetMap (standard street map), satellite imagery, terrain, and hybrid views.
 */
enum class AppMapType(val displayName: String) {
    OPEN_STREET_MAP("OpenStreetMap"),
    SATELLITE("Satellite"),
    NORMAL("Standard"),
    TERRAIN("Terrain"),
    HYBRID("Hybrid")
}
