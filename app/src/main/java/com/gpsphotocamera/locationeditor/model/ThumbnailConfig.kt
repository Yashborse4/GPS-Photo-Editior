package com.gpsphotocamera.locationeditor.model

/**
 * Thumbnail size and styling options for satellite map snapshots.
 */
enum class ThumbnailAspectRatio(val displayName: String, val ratioWidth: Float, val ratioHeight: Float) {
    SQUARE_1_1("Square (1:1)", 1.0f, 1.0f),
    STANDARD_4_3("Standard (4:3)", 4.0f, 3.0f),
    WIDE_16_9("Wide (16:9)", 16.0f, 9.0f)
}

enum class ThumbnailShape(val displayName: String) {
    ROUNDED_RECT("Rounded Rectangle"),
    SHARP_RECT("Sharp Rectangle"),
    CIRCLE("Circle Badge")
}

data class ThumbnailConfig(
    val id: String,
    val name: String,
    val widthPx: Int,
    val heightPx: Int,
    val aspectRatio: ThumbnailAspectRatio,
    val shape: ThumbnailShape = ThumbnailShape.ROUNDED_RECT,
    val cornerRadiusDp: Float = 12f,
    val borderWidthDp: Float = 2f
) {
    companion object {
        val SQUARE_DEFAULT = ThumbnailConfig(
            id = "square_default",
            name = "Standard Stamp (360x360)",
            widthPx = 360,
            heightPx = 360,
            aspectRatio = ThumbnailAspectRatio.SQUARE_1_1,
            shape = ThumbnailShape.ROUNDED_RECT,
            cornerRadiusDp = 12f
        )

        val SQUARE_HIGH_RES = ThumbnailConfig(
            id = "square_hd",
            name = "HD Stamp (600x600)",
            widthPx = 600,
            heightPx = 600,
            aspectRatio = ThumbnailAspectRatio.SQUARE_1_1,
            shape = ThumbnailShape.ROUNDED_RECT,
            cornerRadiusDp = 16f
        )

        val LANDSCAPE_4_3 = ThumbnailConfig(
            id = "landscape_4_3",
            name = "Landscape 4:3 (480x360)",
            widthPx = 480,
            heightPx = 360,
            aspectRatio = ThumbnailAspectRatio.STANDARD_4_3,
            shape = ThumbnailShape.ROUNDED_RECT,
            cornerRadiusDp = 12f
        )

        val CIRCLE_BADGE = ThumbnailConfig(
            id = "circle_badge",
            name = "Circle Badge (320x320)",
            widthPx = 320,
            heightPx = 320,
            aspectRatio = ThumbnailAspectRatio.SQUARE_1_1,
            shape = ThumbnailShape.CIRCLE,
            cornerRadiusDp = 160f
        )

        val ALL_PRESETS = listOf(SQUARE_DEFAULT, SQUARE_HIGH_RES, LANDSCAPE_4_3, CIRCLE_BADGE)
    }
}
