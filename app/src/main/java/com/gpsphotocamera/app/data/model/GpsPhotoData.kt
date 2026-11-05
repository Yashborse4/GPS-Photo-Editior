package com.gpsphotocamera.app.data.model

import java.util.Locale
import kotlin.math.abs

/**
 * Immutable data state containing all editable GPS, timestamp, location, and visual metadata
 * that will be rendered onto the photograph.
 *
 * All fields are editable independently by the user.
 */
data class GpsPhotoData(
    val originalImageUri: String = "",
    val latitude: Double = 18.520430,
    val longitude: Double = 73.856744,
    val locationHeading: String = "Pune, Maharashtra, India",
    val postalAddress: String = "Shivaji Nagar, Pune, Maharashtra 411005, India",
    val dateString: String = "05/11/2026",
    val timeString: String = "03:15:00 PM",
    val timezone: String = "GMT +05:30",
    val customNote: String = "Site Survey & Inspection",
    val stampStyle: StampStyle = StampStyle.CLASSIC_BOTTOM,
    val mapType: AppMapType = AppMapType.SATELLITE
) {
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
     * Combined date, time, and timezone line.
     * Example: "05/11/2026 03:15:00 PM GMT +05:30"
     */
    fun formattedTimestampWithZone(): String {
        return "$dateString $timeString $timezone"
    }
}
