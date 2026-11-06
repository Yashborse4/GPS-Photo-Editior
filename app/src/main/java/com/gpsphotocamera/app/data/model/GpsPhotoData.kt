package com.gpsphotocamera.app.data.model

import com.gpsphotocamera.app.utils.AppDateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Immutable domain state model representing all GPS metadata, address fields,
 * styling options, layout configuration, and interactive panel coordinates.
 */
data class GpsPhotoData(
    // 1. Photo Uri & Target
    val originalImageUri: String = "",

    // 2. Location & Coordinate Information (Default calibrated to reference photograph)
    val latitude: Double = 18.518084,
    val longitude: Double = 73.835936,
    val altitudeMeters: Double = 560.2,
    val headingDegrees: Float = 145f,
    val locationHeading: String = "Pune, Maharashtra, India",
    val postalAddress: String = "870/7, Bhandarkar Rd, Near Hdfc Bank, Shreeman Society, Deccan Gymkhana, Pune, Maharashtra 411004, India",

    // 3. Date, Time & Timezone (Default calibrated to reference photograph)
    val dateString: String = "19/10/2026",
    val timeString: String = "12:18 PM",
    val timezone: String = "GMT +05:30",
    val dayOfWeek: String = "Monday",
    val is24Hour: Boolean = false,

    // 4. Notes & Annotations
    val customNote: String = "Captured by GPS Map Camera",
    val showNote: Boolean = true,

    // 5. Layout Presets & Styling
    val stampStyle: StampStyle = StampStyle.CLASSIC_BOTTOM,

    // 6. Interactive Positioning (Normalized coordinates 0.0f .. 1.0f)
    val normX: Float = 0.50f, // Center horizontally
    val normY: Float = 0.88f, // Near bottom anchor
    val scale: Float = 1.0f,  // Pinch-to-scale multiplier
    val panelWidthPercent: Float = 0.94f,
    val panelHeightPaddingDp: Float = 10f,
    val cornerRadiusDp: Float = 16f,

    // 7. Panel Background & Color (Calibrated to reference dark graphite)
    val backgroundOpacity: Float = 0.78f,
    val backgroundColorHex: Long = 0xFF202225,
    val textColorHex: Long = 0xFFFFFFFF,
    val accentColorHex: Long = 0xFFE53935, // Classic Google Maps Red pin

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
    val mapZoom: Int = 18,
    val mapAspectRatio: String = "SQUARE", // "SQUARE", "LANDSCAPE", "WIDE"
    val showCompass: Boolean = true,
    val showMarker: Boolean = true,
    val customMapImageUri: String? = null
) {
    /**
     * Location heading with Indian national flag emoji appended if not already present.
     */
    val headingWithFlag: String
        get() {
            val trimmed = locationHeading.trim()
            return when {
                trimmed.contains("🇮🇳") || trimmed.contains("🇺🇸") || trimmed.contains("🇬🇧") -> trimmed
                trimmed.endsWith("India", ignoreCase = true) || trimmed.contains("India", ignoreCase = true) -> "$trimmed 🇮🇳"
                else -> trimmed
            }
        }

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
     * Example: 18.518084° N
     */
    fun formattedLatitude(): String {
        val direction = if (latitude >= 0) "N" else "S"
        return String.format(Locale.US, "%.6f° %s", abs(latitude), direction)
    }

    /**
     * Formats longitude with cardinal direction (E / W).
     * Example: 73.835936° E
     */
    fun formattedLongitude(): String {
        val direction = if (longitude >= 0) "E" else "W"
        return String.format(Locale.US, "%.6f° %s", abs(longitude), direction)
    }

    /**
     * Combined coordinate representation.
     * Example: "Lat 18.518084° N  Long 73.835936° E"
     */
    fun formattedCoordinates(): String {
        return "Lat ${formattedLatitude()}  Long ${formattedLongitude()}"
    }

    /**
     * Exact reference coordinate representation without cardinal suffixes.
     * Example: "Lat 18.518084° Long 73.835936°"
     */
    fun formattedCoordinatesExact(): String {
        return String.format(Locale.US, "Lat %.6f° Long %.6f°", latitude, longitude)
    }

    /**
     * Formats altitude and compass heading line.
     * Example: "Altitude: 560.2m   Heading: 145° SE"
     */
    fun formattedAltitudeCompass(): String {
        return String.format(Locale.US, "Altitude: %.1fm   Heading: %d° %s", altitudeMeters, headingDegrees.toInt(), cardinalDirection)
    }

    /**
     * Full reference timestamp strictly matching GPS Map Camera format:
     * Example: "Monday, 19/10/2026 12:18 PM GMT +05:30"
     */
    fun formattedTimestampWithZone(): String {
        return AppDateTimeFormatter.formatFullReferenceTimestamp(dayOfWeek, dateString, timeString, timezone)
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
    fun applyPreset(preset: StampStyle): GpsPhotoData = applyPresetDefaults(preset)

    fun applyPresetDefaults(preset: StampStyle): GpsPhotoData {
        return when (preset) {
            StampStyle.CLASSIC_BOTTOM -> copy(
                stampStyle = preset,
                panelWidthPercent = 0.94f,
                backgroundOpacity = 0.72f,
                backgroundColorHex = 0xFF202225,
                textColorHex = 0xFFFFFFFF,
                accentColorHex = 0xFFE53935,
                cornerRadiusDp = 14f,
                lineSpacingDp = 3f,
                showMap = true,
                mapAspectRatio = "SQUARE",
                normY = 0.88f
            )
            StampStyle.COMPACT_MINIMAL -> copy(
                stampStyle = preset,
                panelWidthPercent = 0.96f,
                backgroundOpacity = 0.80f,
                backgroundColorHex = 0xFF14171F,
                textColorHex = 0xFFFFFFFF,
                cornerRadiusDp = 8f,
                lineSpacingDp = 2f,
                showMap = true,
                mapAspectRatio = "SQUARE",
                normY = 0.92f
            )
            StampStyle.MODERN_CARD -> copy(
                stampStyle = preset,
                panelWidthPercent = 0.92f,
                backgroundOpacity = 0.82f,
                backgroundColorHex = 0xFF181B22,
                textColorHex = 0xFFFFFFFF,
                cornerRadiusDp = 18f,
                lineSpacingDp = 4f,
                showMap = true,
                mapAspectRatio = "LANDSCAPE",
                normY = 0.85f
            )
            StampStyle.MINIMAL_WATERMARK -> copy(
                stampStyle = preset,
                panelWidthPercent = 0.80f,
                backgroundOpacity = 0.45f,
                backgroundColorHex = 0xFF000000,
                textColorHex = 0xFFFFFFFF,
                cornerRadiusDp = 24f,
                lineSpacingDp = 2f,
                showMap = false,
                textAlign = "CENTER",
                normY = 0.90f
            )
        }
    }
}
