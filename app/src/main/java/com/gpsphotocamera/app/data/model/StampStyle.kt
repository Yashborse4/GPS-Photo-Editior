package com.gpsphotocamera.app.data.model

/**
 * Visual layout presets for the GPS Photo Camera live overlay and high-res export.
 */
enum class StampStyle(val title: String, val description: String) {
    CLASSIC_BOTTOM(
        title = "GPS Map Camera Classic",
        description = "Dark translucent card, satellite thumbnail, compass, and multi-line metadata reproducing reference photo"
    ),
    COMPACT_MINIMAL(
        title = "Compact GPS Overlay",
        description = "Sleek low-profile bottom overlay with essential coordinates and timestamp"
    ),
    MODERN_CARD(
        title = "Large Location Card",
        description = "Spacious floating card with wide satellite map preview and comprehensive metadata"
    ),
    MINIMAL_WATERMARK(
        title = "Minimal Location Overlay",
        description = "Clean unobtrusive floating watermark pill for maximum photo visibility"
    );

    companion object {
        val CLASSIC = CLASSIC_BOTTOM
        val COMPACT = COMPACT_MINIMAL
        val LARGE_CARD = MODERN_CARD
        val MINIMAL = MINIMAL_WATERMARK
    }
}
