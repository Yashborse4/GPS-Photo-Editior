package com.gpsphotocamera.locationeditor.model

/**
 * Encapsulates validation results for latitude and longitude fields.
 */
data class CoordinateValidationResult(
    val isLatitudeValid: Boolean,
    val latitudeError: String? = null,
    val isLongitudeValid: Boolean,
    val longitudeError: String? = null
) {
    val isValid: Boolean
        get() = isLatitudeValid && isLongitudeValid

    companion object {
        val VALID = CoordinateValidationResult(
            isLatitudeValid = true,
            latitudeError = null,
            isLongitudeValid = true,
            longitudeError = null
        )
    }
}
