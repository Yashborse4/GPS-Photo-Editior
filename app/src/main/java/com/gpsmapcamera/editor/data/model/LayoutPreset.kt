package com.gpsmapcamera.editor.data.model

/**
 * Layout presets for the GPS Map Camera overlay.
 */
enum class LayoutPreset(val title: String, val description: String) {
    CLASSIC(
        title = "GPS Map Camera Classic",
        description = "Side-by-side satellite map thumbnail and multi-line detailed GPS metadata card at bottom"
    ),
    COMPACT(
        title = "Compact GPS Overlay",
        description = "Sleek, low-profile horizontal ribbon ideal for wide landscapes and panoramic photos"
    ),
    LARGE_CARD(
        title = "Large Location Card",
        description = "Modern elevated card with prominent map header, weather badge, and structured badges"
    ),
    MINIMAL(
        title = "Minimal Location Overlay",
        description = "Clean frameless typography with subtle drop-shadow directly over the photo"
    )
}
