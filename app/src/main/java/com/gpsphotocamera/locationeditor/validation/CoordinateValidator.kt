package com.gpsphotocamera.locationeditor.validation

import com.gpsphotocamera.locationeditor.model.CoordinateValidationResult

/**
 * Robust coordinate validation engine for manual GPS entry.
 * Validates range, syntax, numerical parsing, edge cases, and returns human-friendly feedback.
 */
object CoordinateValidator {

    private const val MIN_LATITUDE = -90.0
    private const val MAX_LATITUDE = 90.0
    private const val MIN_LONGITUDE = -180.0
    private const val MAX_LONGITUDE = 180.0

    /**
     * Validate an individual latitude string.
     */
    fun validateLatitude(latitudeStr: String): Pair<Boolean, String?> {
        val trimmed = latitudeStr.trim()
        if (trimmed.isEmpty()) {
            return Pair(false, "Latitude cannot be empty.")
        }

        val lat = trimmed.toDoubleOrNull()
            ?: return Pair(false, "Invalid format. Enter a valid decimal number.")

        if (lat.isNaN() || lat.isInfinite()) {
            return Pair(false, "Latitude must be a finite numerical value.")
        }

        if (lat < MIN_LATITUDE || lat > MAX_LATITUDE) {
            return Pair(false, "Latitude must be between -90.0 and +90.0 degrees.")
        }

        return Pair(true, null)
    }

    /**
     * Validate an individual longitude string.
     */
    fun validateLongitude(longitudeStr: String): Pair<Boolean, String?> {
        val trimmed = longitudeStr.trim()
        if (trimmed.isEmpty()) {
            return Pair(false, "Longitude cannot be empty.")
        }

        val lon = trimmed.toDoubleOrNull()
            ?: return Pair(false, "Invalid format. Enter a valid decimal number.")

        if (lon.isNaN() || lon.isInfinite()) {
            return Pair(false, "Longitude must be a finite numerical value.")
        }

        if (lon < MIN_LONGITUDE || lon > MAX_LONGITUDE) {
            return Pair(false, "Longitude must be between -180.0 and +180.0 degrees.")
        }

        return Pair(true, null)
    }

    /**
     * Full validation of both latitude and longitude fields.
     */
    fun validateCoordinates(latitudeStr: String, longitudeStr: String): CoordinateValidationResult {
        val (latValid, latError) = validateLatitude(latitudeStr)
        val (lonValid, lonError) = validateLongitude(longitudeStr)

        return CoordinateValidationResult(
            isLatitudeValid = latValid,
            latitudeError = latError,
            isLongitudeValid = lonValid,
            longitudeError = lonError
        )
    }

    /**
     * Normalize coordinate string (strip extra spaces, normalize commas to decimal points if entered).
     */
    fun sanitizeInput(input: String): String {
        return input.trim().replace(',', '.')
    }
}
