package com.gpsphotocamera.locationeditor.model

import com.google.android.gms.maps.model.LatLng
import java.util.Locale
import kotlin.math.abs

/**
 * Data class representing all manually editable location information for GPS Photo Camera.
 * Completely independent of device GPS hardware, allowing full manual control and overriding.
 */
data class LocationData(
    val heading: String = "Pune, Maharashtra, India",
    val country: String = "India",
    val state: String = "Maharashtra",
    val city: String = "Pune",
    val postalAddress: String = "40/34, Near Prime Furnishings, Bhonde Colony, Erandwane, Pune, Maharashtra 411004, India",
    val pinCode: String = "411004",
    val latitudeString: String = "18.511347",
    val longitudeString: String = "73.833891"
) {
    /**
     * Safely parse latitude into Double without throwing exceptions.
     */
    val latitude: Double?
        get() = latitudeString.trim().toDoubleOrNull()

    /**
     * Safely parse longitude into Double without throwing exceptions.
     */
    val longitude: Double?
        get() = longitudeString.trim().toDoubleOrNull()

    /**
     * Convert to Google Maps LatLng object if both coordinates are valid.
     */
    fun toLatLng(): LatLng? {
        val lat = latitude ?: return null
        val lon = longitude ?: return null
        return if (lat in -90.0..90.0 && lon in -180.0..180.0) {
            LatLng(lat, lon)
        } else {
            null
        }
    }

    /**
     * Format coordinates in decimal degrees format with cardinal indicators (e.g., "18.511347° N, 73.833891° E")
     */
    fun toFormattedCoordinates(precision: Int = 6): String {
        val lat = latitude ?: return "Invalid Latitude"
        val lon = longitude ?: return "Invalid Longitude"

        val latDir = if (lat >= 0) "N" else "S"
        val lonDir = if (lon >= 0) "E" else "W"

        val format = "%.${precision}f° %s, %.${precision}f° %s"
        return String.format(Locale.US, format, abs(lat), latDir, abs(lon), lonDir)
    }

    /**
     * Format into classic GPS Map Camera Degrees-Minutes-Seconds (DMS) string
     */
    fun toDmsCoordinates(): String {
        val lat = latitude ?: return "Invalid Lat"
        val lon = longitude ?: return "Invalid Lon"

        fun toDms(value: Double, isLat: Boolean): String {
            val direction = if (isLat) {
                if (value >= 0) "N" else "S"
            } else {
                if (value >= 0) "E" else "W"
            }
            val absVal = abs(value)
            val degrees = absVal.toInt()
            val minutesDecimal = (absVal - degrees) * 60.0
            val minutes = minutesDecimal.toInt()
            val seconds = (minutesDecimal - minutes) * 60.0
            return String.format(Locale.US, "%d°%d\'%.2f\" %s", degrees, minutes, seconds, direction)
        }

        return "${toDms(lat, true)}, ${toDms(lon, false)}"
    }

    companion object {
        /**
         * Reference sample from requirements: Pune, Maharashtra, India.
         */
        val DEFAULT_PUNE = LocationData(
            heading = "Pune, Maharashtra, India",
            country = "India",
            state = "Maharashtra",
            city = "Pune",
            postalAddress = "40/34, Near Prime Furnishings, Bhonde Colony, Erandwane, Pune, Maharashtra 411004, India",
            pinCode = "411004",
            latitudeString = "18.511347",
            longitudeString = "73.833891"
        )

        /**
         * Additional quick-test presets for global validation (testing negative coordinates, hemispheres).
         */
        val PRESET_NEW_YORK = LocationData(
            heading = "New York City, NY, USA",
            country = "United States",
            state = "New York",
            city = "New York",
            postalAddress = "Empire State Building, 20 W 34th St, New York, NY 10001, USA",
            pinCode = "10001",
            latitudeString = "40.748817",
            longitudeString = "-73.985428"
        )

        val PRESET_SYDNEY = LocationData(
            heading = "Sydney, NSW, Australia",
            country = "Australia",
            state = "New South Wales",
            city = "Sydney",
            postalAddress = "Bennelong Point, Sydney NSW 2000, Australia",
            pinCode = "2000",
            latitudeString = "-33.856784",
            longitudeString = "151.215297"
        )

        val PRESET_TOKYO = LocationData(
            heading = "Tokyo, Japan",
            country = "Japan",
            state = "Kanto",
            city = "Tokyo",
            postalAddress = "4 Chome-2-8 Shibakoen, Minato City, Tokyo 105-0011, Japan",
            pinCode = "105-0011",
            latitudeString = "35.658581",
            longitudeString = "139.745433"
        )
    }
}
