package com.gpsmapcamera.editor.domain.util

import com.gpsmapcamera.editor.data.model.CoordinatesFormat
import java.util.Locale

object CoordinatesFormatter {

    /**
     * Formats latitude and longitude according to the given format.
     */
    fun format(lat: Double, lng: Double, format: CoordinatesFormat): String {
        return when (format) {
            CoordinatesFormat.DECIMAL -> formatDecimal(lat, lng)
            CoordinatesFormat.DMS -> formatDms(lat, lng)
            CoordinatesFormat.COMPACT -> formatCompact(lat, lng)
        }
    }

    /**
     * Formats as Decimal Degrees: e.g. "Lat 18.520430°  Long 73.856744°"
     */
    fun formatDecimal(lat: Double, lng: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lngDir = if (lng >= 0) "E" else "W"
        return String.format(
            Locale.US,
            "Lat %.6f° %s   Long %.6f° %s",
            Math.abs(lat), latDir,
            Math.abs(lng), lngDir
        )
    }

    /**
     * Formats single latitude in decimal degrees.
     */
    fun formatLatDecimal(lat: Double): String {
        val dir = if (lat >= 0) "N" else "S"
        return String.format(Locale.US, "%.6f° %s", Math.abs(lat), dir)
    }

    /**
     * Formats single longitude in decimal degrees.
     */
    fun formatLngDecimal(lng: Double): String {
        val dir = if (lng >= 0) "E" else "W"
        return String.format(Locale.US, "%.6f° %s", Math.abs(lng), dir)
    }

    /**
     * Formats as Degrees Minutes Seconds: e.g. 18° 31' 13.5" N   73° 51' 24.3" E
     */
    fun formatDms(lat: Double, lng: Double): String {
        val latDms = toDms(lat, isLatitude = true)
        val lngDms = toDms(lng, isLatitude = false)
        return "$latDms   $lngDms"
    }

    /**
     * Compact format: "18.520430, 73.856744"
     */
    fun formatCompact(lat: Double, lng: Double): String {
        return String.format(Locale.US, "%.6f, %.6f", lat, lng)
    }

    private fun toDms(coordinate: Double, isLatitude: Boolean): String {
        val direction = if (isLatitude) {
            if (coordinate >= 0) "N" else "S"
        } else {
            if (coordinate >= 0) "E" else "W"
        }
        val abs = Math.abs(coordinate)
        val degrees = abs.toInt()
        val minutesDouble = (abs - degrees) * 60.0
        val minutes = minutesDouble.toInt()
        val seconds = (minutesDouble - minutes) * 60.0

        return String.format(Locale.US, "%d° %02d' %04.1f\" %s", degrees, minutes, seconds, direction)
    }

    /**
     * Validate coordinate boundaries.
     */
    fun isValidLatitude(lat: Double): Boolean = lat in -90.0..90.0

    fun isValidLongitude(lng: Double): Boolean = lng in -180.0..180.0

    /**
     * Parses a string representing decimal degrees (e.g. "18.520430", "-73.856744").
     */
    fun parseCoordinate(input: String): Double? {
        val clean = input.trim().replace(",", ".").replace("°", "")
        return clean.toDoubleOrNull()
    }
}
