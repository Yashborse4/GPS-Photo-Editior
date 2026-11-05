package com.gpsphotocamera.app.data.model

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Immutable data state containing all editable GPS, timestamp, location, visual styling,
 * panel geometries, and typography parameters.
 *
 * Designed for resolution-independent live Jetpack Compose previewing and native
 * full-resolution Android Canvas & Bitmap rendering.
 */
data class GpsPhotoData(
    // 1. Source Photograph
    val originalImageUri: String = "",

    // 2. GPS & Geographical Coordinates
    val latitude: Double = 18.520430,
    val longitude: Double = 73.856744,
    val altitudeMeters: Double = 560.2,
    val headingDegrees: Float = 145f,
    val locationHeading: String = "Pune, Maharashtra, India",
    val postalAddress: String = "Shivaji Nagar, Pune, Maharashtra 411005, India",

    // 3. Date, Time & Timezone
    val dateString: String = "05/11/2026",
    val timeString: String = "03:15:00 PM",
    val timezone: String = "GMT +05:30",
    val dayOfWeek: String = "Thursday",
    val is24Hour: Boolean = false,

    // 4. Notes & Annotations
    val customNote: String = "Site Survey & Inspection",
    val showNote: Boolean = true,

    // 5. Layout Presets & Styling
    val stampStyle: StampStyle = StampStyle.CLASSIC_BOTTOM,

    // 6. Interactive Positioning (Normalized coordinates 0.0f .. 1.0f)
    val normX: Float = 0.50f, // Center horizontally
    val normY: Float = 0.88f, // Near bottom anchor
    val scale: Float = 1.0f,  // Pinch-to-scale multiplier
    val panelWidthPercent: Float = 0.94f,
    val panelHeightPaddingDp: Float = 10f,
    val cornerRadiusDp: Float = 14f,

    // 7. Panel Background & Color
    val backgroundOpacity: Float = 0.72f,
    val backgroundColorHex: Long = 0xFF0D0F14,
    val textColorHex: Long = 0xFFFFFFFF,
    val accentColorHex: Long = 0xFFFF3B30, // Red pin / compass needle

    // 8. Typography Controls
    val fontSizeMultiplier: Float = 1.0f,
    val isBold: Boolean = false,
    val fontFamily: String = "DEFAULT", // "DEFAULT", "MONOSPACE", "SERIF"
    val lineSpacingDp: Float = 3f,
    val textAlign: String = "LEFT", // "LEFT", "CENTER", "RIGHT"

    // 9. Map Controls
    val mapType: AppMapType = AppMapType.SATELLITE,
    val showMap: Boolean = true,
    val mapThumbnailScale: Float = 1.0f,
    val mapZoom: Int = 16,
    val mapAspectRatio: String = "SQUARE", // "SQUARE", "LANDSCAPE", "WIDE"
    val showCompass: Boolean = true,
    val showMarker: Boolean = true
) {
    /**
     * Cardinal direction representation derived from heading bearing degrees.
     */
    val cardinalDirection: String
        get() {
            val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW", "N")
            val index = ((headingDegrees % 360) / 45.0).roundToInt()
            return directions[index.coerceIn(0, directions.lastIndex)]
        }

    /**
     * Formats latitude with cardinal direction (N / S).
     * Example: 18.520430° N
     */
    fun formattedLatitude(): String {
        val direction = if (latitude >= 0) "N" else "S"
        return String.format(Locale.US, "%.6f° %s", abs(latitude), direction)
    }

    /**
     * Formats longitude with cardinal direction (E / W).
     * Example: 73.856744° E
     */
    fun formattedLongitude(): String {
        val direction = if (longitude >= 0) "E" else "W"
        return String.format(Locale.US, "%.6f° %s", abs(longitude), direction)
    }

    /**
     * Combined coordinate representation.
     * Example: "Lat 18.520430° N  Long 73.856744° E"
     */
    fun formattedCoordinates(): String {
        return "Lat ${formattedLatitude()}  Long ${formattedLongitude()}"
    }

    /**
     * Formats altitude and compass heading line.
     * Example: "Altitude: 560.2m   Heading: 145° SE"
     */
    fun formattedAltitudeCompass(): String {
        return String.format(Locale.US, "Altitude: %.1fm   Heading: %d° %s", altitudeMeters, headingDegrees.toInt(), cardinalDirection)
    }

    /**
     * Combined date, time, and timezone line.
     * Example: "05/11/2026 03:15:00 PM GMT +05:30"
     */
    fun formattedTimestampWithZone(): String {
        return "$dateString $timeString $timezone"
    }

    /**
     * Resets the overlay panel position to default bottom-centered orientation.
     */
    fun resetPosition(): GpsPhotoData {
        return copy(
            normX = 0.50f,
            normY = when (stampStyle) {
                StampStyle.CLASSIC_BOTTOM -> 0.88f
                StampStyle.COMPACT_MINIMAL -> 0.92f
                StampStyle.MODERN_CARD -> 0.85f
                StampStyle.MINIMAL_WATERMARK -> 0.90f
            },
            scale = 1.0f
        )
    }

    /**
     * Applies default styling parameters calibrated for each layout preset.
     */
    fun applyPreset(style: StampStyle): GpsPhotoData {
        return when (style) {
            StampStyle.CLASSIC_BOTTOM -> copy(
                stampStyle = StampStyle.CLASSIC_BOTTOM,
                backgroundOpacity = 0.72f,
                backgroundColorHex = 0xFF0D0F14,
                textColorHex = 0xFFFFFFFF,
                accentColorHex = 0xFFFF3B30,
                cornerRadiusDp = 14f,
                panelWidthPercent = 0.94f,
                showMap = true,
                mapAspectRatio = "SQUARE",
                mapThumbnailScale = 1.0f,
                fontSizeMultiplier = 1.0f,
                normX = 0.50f,
                normY = 0.88f,
                scale = 1.0f,
                textAlign = "LEFT"
            )
            StampStyle.COMPACT_MINIMAL -> copy(
                stampStyle = StampStyle.COMPACT_MINIMAL,
                backgroundOpacity = 0.80f,
                backgroundColorHex = 0xFF12141A,
                textColorHex = 0xFFFFFFFF,
                accentColorHex = 0xFF38BDF8,
                cornerRadiusDp = 8f,
                panelWidthPercent = 0.96f,
                showMap = true,
                mapAspectRatio = "SQUARE",
                mapThumbnailScale = 0.85f,
                fontSizeMultiplier = 0.90f,
                normX = 0.50f,
                normY = 0.92f,
                scale = 0.95f,
                textAlign = "LEFT"
            )
            StampStyle.MODERN_CARD -> copy(
                stampStyle = StampStyle.MODERN_CARD,
                backgroundOpacity = 0.82f,
                backgroundColorHex = 0xFF1E222D,
                textColorHex = 0xFFFFFFFF,
                accentColorHex = 0xFF10B981,
                cornerRadiusDp = 18f,
                panelWidthPercent = 0.92f,
                showMap = true,
                mapAspectRatio = "LANDSCAPE",
                mapThumbnailScale = 1.15f,
                fontSizeMultiplier = 1.05f,
                normX = 0.50f,
                normY = 0.85f,
                scale = 1.0f,
                textAlign = "LEFT"
            )
            StampStyle.MINIMAL_WATERMARK -> copy(
                stampStyle = StampStyle.MINIMAL_WATERMARK,
                backgroundOpacity = 0.60f,
                backgroundColorHex = 0xFF000000,
                textColorHex = 0xFFF1F5F9,
                accentColorHex = 0xFFF59E0B,
                cornerRadiusDp = 24f,
                panelWidthPercent = 0.88f,
                showMap = false,
                fontSizeMultiplier = 0.90f,
                normX = 0.50f,
                normY = 0.90f,
                scale = 0.90f,
                textAlign = "CENTER"
            )
        }
    }
}
