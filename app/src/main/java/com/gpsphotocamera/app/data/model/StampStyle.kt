package com.gpsphotocamera.app.data.model

/**
 * Visual styling options for the generated GPS overlay stamp.
 */
enum class StampStyle(val title: String, val description: String) {
    CLASSIC_BOTTOM(
        title = "Classic Banner",
        description = "Full-width bottom banner with map thumbnail, address, and coordinates"
    ),
    MODERN_CARD(
        title = "Floating Card",
        description = "Rounded translucent floating badge with map and compact metadata"
    ),
    COMPACT_MINIMAL(
        title = "Compact Minimal",
        description = "Minimalist watermark banner with key GPS & timestamp"
    )
}
