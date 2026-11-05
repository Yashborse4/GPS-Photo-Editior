package com.gpsmapcamera.editor.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Complete immutable state representation for the GPS Photo Camera live preview
 * and high-resolution export engine.
 */
data class GpsOverlayState(
    // Location Data
    val locationTitle: String = "Pune, Maharashtra, India",
    val streetAddress: String = "Shivaji Nagar, FC Road, 411005",
    val latitude: Double = 18.520430,
    val longitude: Double = 73.856744,
    val altitudeMeters: Double = 560.2,
    val accuracyMeters: Float = 4.5f,
    val headingDegrees: Float = 145f, // Compass bearing
    val coordinatesFormat: CoordinatesFormat = CoordinatesFormat.DECIMAL,

    // Date & Time Data (Manual & Persistent)
    val customDate: String = "05/11/2026",
    val customTime: String = "03:15:20 PM",
    val customTimezone: String = "GMT +05:30",
    val dayOfWeek: String = "Thursday",
    val is24Hour: Boolean = false,
    val timestampEpochMillis: Long = System.currentTimeMillis(),

    // Notes & Metadata
    val note: String = "Site Survey #402 - Inspection Point",
    val showNote: Boolean = true,
    val weatherDescription: String = "31°C Sunny",
    val showWeather: Boolean = false,

    // Layout & Presets
    val preset: LayoutPreset = LayoutPreset.CLASSIC,

    // Panel Geometry & Positioning (Normalized 0.0f .. 1.0f coordinates relative to image)
    val normX: Float = 0.50f, // Center horizontally
    val normY: Float = 0.88f, // Near bottom
    val panelWidthPercent: Float = 0.94f, // 94% of photo width
    val panelHeightPaddingDp: Float = 12f,
    val cornerRadiusDp: Float = 12f,
    val scale: Float = 1.0f,

    // Panel Color & Alpha
    val backgroundOpacity: Float = 0.75f,
    val backgroundColorHex: Long = 0xFF000000, // Black
    val textColorHex: Long = 0xFFFFFFFF,       // White
    val accentColorHex: Long = 0xFFFF3B30,     // Vibrant Red / Orange Pin

    // Typography
    val fontFamily: FontFamilyOption = FontFamilyOption.DEFAULT,
    val fontSizeMultiplier: Float = 1.0f,
    val isBold: Boolean = false,
    val lineSpacingDp: Float = 3f,
    val textAlign: OverlayTextAlign = OverlayTextAlign.LEFT,

    // Map Thumbnail
    val showMap: Boolean = true,
    val mapType: MapType = MapType.SATELLITE,
    val mapZoom: Int = 16,
    val mapAspectRatio: MapAspectRatio = MapAspectRatio.SQUARE,
    val mapScale: Float = 1.0f,
    val showCompass: Boolean = true,
    val showMarker: Boolean = true
) {
    /**
     * Cardinal direction calculation from heading degrees.
     */
    val cardinalDirection: String
        get() {
            val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW", "N")
            val index = Math.round((headingDegrees % 360) / 45.0).toInt()
            return directions[index]
        }

    /**
     * Reset panel position and scaling to default bottom alignment.
     */
    fun resetPosition(): GpsOverlayState {
        return copy(
            normX = 0.50f,
            normY = when (preset) {
                LayoutPreset.CLASSIC -> 0.88f
                LayoutPreset.COMPACT -> 0.92f
                LayoutPreset.LARGE_CARD -> 0.84f
                LayoutPreset.MINIMAL -> 0.90f
            },
            scale = 1.0f
        )
    }

    /**
     * Apply default styling according to a selected layout preset.
     */
    fun applyPreset(newPreset: LayoutPreset): GpsOverlayState {
        return when (newPreset) {
            LayoutPreset.CLASSIC -> copy(
                preset = LayoutPreset.CLASSIC,
                backgroundOpacity = 0.75f,
                backgroundColorHex = 0xFF000000,
                textColorHex = 0xFFFFFFFF,
                cornerRadiusDp = 12f,
                panelWidthPercent = 0.94f,
                showMap = true,
                mapAspectRatio = MapAspectRatio.SQUARE,
                normY = 0.88f,
                normX = 0.50f,
                scale = 1.0f,
                fontSizeMultiplier = 1.0f,
                textAlign = OverlayTextAlign.LEFT
            )
            LayoutPreset.COMPACT -> copy(
                preset = LayoutPreset.COMPACT,
                backgroundOpacity = 0.80f,
                backgroundColorHex = 0xFF121212,
                textColorHex = 0xFFFFFFFF,
                cornerRadiusDp = 8f,
                panelWidthPercent = 0.96f,
                showMap = true,
                mapAspectRatio = MapAspectRatio.SQUARE,
                normY = 0.92f,
                normX = 0.50f,
                scale = 0.95f,
                fontSizeMultiplier = 0.90f,
                textAlign = OverlayTextAlign.LEFT
            )
            LayoutPreset.LARGE_CARD -> copy(
                preset = LayoutPreset.LARGE_CARD,
                backgroundOpacity = 0.85f,
                backgroundColorHex = 0xFF181C24,
                textColorHex = 0xFFFFFFFF,
                cornerRadiusDp = 18f,
                panelWidthPercent = 0.92f,
                showMap = true,
                mapAspectRatio = MapAspectRatio.LANDSCAPE,
                normY = 0.84f,
                normX = 0.50f,
                scale = 1.05f,
                fontSizeMultiplier = 1.05f,
                textAlign = OverlayTextAlign.LEFT
            )
            LayoutPreset.MINIMAL -> copy(
                preset = LayoutPreset.MINIMAL,
                backgroundOpacity = 0.35f,
                backgroundColorHex = 0x00000000,
                textColorHex = 0xFFFFFFFF,
                cornerRadiusDp = 6f,
                panelWidthPercent = 0.90f,
                showMap = false,
                normY = 0.90f,
                normX = 0.50f,
                scale = 0.95f,
                fontSizeMultiplier = 1.0f,
                textAlign = OverlayTextAlign.LEFT
            )
        }
    }

    companion object {
        fun createDefault(): GpsOverlayState {
            val now = Date()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            val tz = TimeZone.getDefault()
            val rawOffset = tz.rawOffset
            val hours = rawOffset / (1000 * 60 * 60)
            val minutes = Math.abs((rawOffset / (1000 * 60)) % 60)
            val tzFormatted = String.format(Locale.US, "GMT %+03d:%02d", hours, minutes)

            return GpsOverlayState(
                customDate = dateFormat.format(now),
                customTime = timeFormat.format(now),
                customTimezone = tzFormatted,
                dayOfWeek = dayFormat.format(now)
            )
        }
    }
}
