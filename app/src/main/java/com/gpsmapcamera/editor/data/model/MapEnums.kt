package com.gpsmapcamera.editor.data.model

enum class MapType(val label: String) {
    SATELLITE("Satellite"),
    HYBRID("Hybrid"),
    TERRAIN("Terrain"),
    ROADMAP("Roadmap")
}

enum class MapAspectRatio(val label: String, val ratio: Float) {
    SQUARE("1:1 Square", 1.0f),
    LANDSCAPE("4:3 Standard", 4f / 3f),
    WIDE("16:9 Wide", 16f / 9f),
    CIRCLE("Circle", 1.0f)
}

enum class CoordinatesFormat(val label: String) {
    DECIMAL("Decimal Degrees (DD)"),
    DMS("Degrees, Minutes, Seconds (DMS)"),
    COMPACT("Compact (Lat/Long)")
}
