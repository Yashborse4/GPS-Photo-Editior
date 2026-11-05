package com.gpsphotocamera.app.data.model

import java.util.Locale
import java.util.UUID
import kotlin.math.abs

/**
 * Entity representing a user-saved or favorite GPS location.
 * Allows instant re-application of coordinates, address, and map settings to future photographs.
 */
data class SavedLocation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val heading: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String = "GMT +05:30",
    val mapType: AppMapType = AppMapType.OPEN_STREET_MAP,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Human-readable formatted coordinates with hemisphere indicators.
     */
    val formattedCoordinates: String
        get() {
            val latDir = if (latitude >= 0) "N" else "S"
            val lngDir = if (longitude >= 0) "E" else "W"
            return String.format(
                Locale.US,
                "%.6f° %s, %.6f° %s",
                abs(latitude),
                latDir,
                abs(longitude),
                lngDir
            )
        }
}
