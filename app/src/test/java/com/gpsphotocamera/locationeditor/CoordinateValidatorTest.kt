package com.gpsphotocamera.locationeditor

import com.gpsphotocamera.locationeditor.model.LocationData
import com.gpsphotocamera.locationeditor.validation.CoordinateValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive Unit Tests for CoordinateValidator and LocationData formatting.
 */
class CoordinateValidatorTest {

    // --- Latitude Validation Tests ---

    @Test
    fun `validateLatitude accepts valid positive coordinate`() {
        val (isValid, error) = CoordinateValidator.validateLatitude("18.511347")
        assertTrue(isValid)
        assertNull(error)
    }

    @Test
    fun `validateLatitude accepts valid negative coordinate`() {
        val (isValid, error) = CoordinateValidator.validateLatitude("-33.856784")
        assertTrue(isValid)
        assertNull(error)
    }

    @Test
    fun `validateLatitude accepts exact boundaries`() {
        assertTrue(CoordinateValidator.validateLatitude("-90.0").first)
        assertTrue(CoordinateValidator.validateLatitude("90.0").first)
        assertTrue(CoordinateValidator.validateLatitude("0.0").first)
        assertTrue(CoordinateValidator.validateLatitude("0").first)
    }

    @Test
    fun `validateLatitude rejects out of range coordinates`() {
        val (isValidOver, errorOver) = CoordinateValidator.validateLatitude("90.00001")
        assertFalse(isValidOver)
        assertNotNull(errorOver)

        val (isValidUnder, errorUnder) = CoordinateValidator.validateLatitude("-90.00001")
        assertFalse(isValidUnder)
        assertNotNull(errorUnder)
    }

    @Test
    fun `validateLatitude rejects empty and blank inputs`() {
        val (isValidEmpty, _) = CoordinateValidator.validateLatitude("")
        assertFalse(isValidEmpty)

        val (isValidBlank, _) = CoordinateValidator.validateLatitude("   ")
        assertFalse(isValidBlank)
    }

    @Test
    fun `validateLatitude rejects non-numerical strings`() {
        assertFalse(CoordinateValidator.validateLatitude("18.511abc").first)
        assertFalse(CoordinateValidator.validateLatitude("abc").first)
        assertFalse(CoordinateValidator.validateLatitude("18.511.347").first)
        assertFalse(CoordinateValidator.validateLatitude("--18.511").first)
    }

    // --- Longitude Validation Tests ---

    @Test
    fun `validateLongitude accepts valid positive coordinate`() {
        val (isValid, error) = CoordinateValidator.validateLongitude("73.833891")
        assertTrue(isValid)
        assertNull(error)
    }

    @Test
    fun `validateLongitude accepts valid negative coordinate`() {
        val (isValid, error) = CoordinateValidator.validateLongitude("-73.985428")
        assertTrue(isValid)
        assertNull(error)
    }

    @Test
    fun `validateLongitude accepts exact boundaries`() {
        assertTrue(CoordinateValidator.validateLongitude("-180.0").first)
        assertTrue(CoordinateValidator.validateLongitude("180.0").first)
        assertTrue(CoordinateValidator.validateLongitude("0.0").first)
    }

    @Test
    fun `validateLongitude rejects out of range coordinates`() {
        val (isValidOver, errorOver) = CoordinateValidator.validateLongitude("180.00001")
        assertFalse(isValidOver)
        assertNotNull(errorOver)

        val (isValidUnder, errorUnder) = CoordinateValidator.validateLongitude("-180.00001")
        assertFalse(isValidUnder)
        assertNotNull(errorUnder)
    }

    @Test
    fun `validateLongitude rejects non-numerical strings and empty inputs`() {
        assertFalse(CoordinateValidator.validateLongitude("").first)
        assertFalse(CoordinateValidator.validateLongitude("   ").first)
        assertFalse(CoordinateValidator.validateLongitude("73.xyz").first)
        assertFalse(CoordinateValidator.validateLongitude("73.83.89").first)
    }

    // --- Combined Coordinate Validation Tests ---

    @Test
    fun `validateCoordinates handles valid reference pair`() {
        val result = CoordinateValidator.validateCoordinates("18.511347", "73.833891")
        assertTrue(result.isValid)
        assertTrue(result.isLatitudeValid)
        assertTrue(result.isLongitudeValid)
        assertNull(result.latitudeError)
        assertNull(result.longitudeError)
    }

    @Test
    fun `validateCoordinates handles partial invalidity`() {
        // Lat valid, Lon invalid
        val res1 = CoordinateValidator.validateCoordinates("18.511347", "195.000")
        assertFalse(res1.isValid)
        assertTrue(res1.isLatitudeValid)
        assertFalse(res1.isLongitudeValid)

        // Lat invalid, Lon valid
        val res2 = CoordinateValidator.validateCoordinates("95.511347", "73.833891")
        assertFalse(res2.isValid)
        assertFalse(res2.isLatitudeValid)
        assertTrue(res2.isLongitudeValid)

        // Both invalid
        val res3 = CoordinateValidator.validateCoordinates("abc", "xyz")
        assertFalse(res3.isValid)
        assertFalse(res3.isLatitudeValid)
        assertFalse(res3.isLongitudeValid)
    }

    // --- Input Sanitization Tests ---

    @Test
    fun `sanitizeInput normalizes European comma decimals and whitespace`() {
        assertEquals("18.511347", CoordinateValidator.sanitizeInput("  18,511347  "))
        assertEquals("-73.985428", CoordinateValidator.sanitizeInput("-73,985428"))
    }

    // --- LocationData Formatting & DMS Tests ---

    @Test
    fun `LocationData formats coordinates with cardinal directions`() {
        val data = LocationData(
            latitudeString = "18.511347",
            longitudeString = "73.833891"
        )
        val formatted = data.toFormattedCoordinates()
        assertTrue(formatted.contains("N"))
        assertTrue(formatted.contains("E"))
        assertTrue(formatted.contains("18.511347"))
        assertTrue(formatted.contains("73.833891"))
    }

    @Test
    fun `LocationData handles negative coordinates in cardinal directions`() {
        val data = LocationData(
            latitudeString = "-33.856784",
            longitudeString = "-151.215297"
        )
        val formatted = data.toFormattedCoordinates()
        assertTrue(formatted.contains("S"))
        assertTrue(formatted.contains("W"))
    }

    @Test
    fun `LocationData converts to DMS accurately`() {
        val data = LocationData(
            latitudeString = "18.511347",
            longitudeString = "73.833891"
        )
        val dms = data.toDmsCoordinates()
        assertTrue(dms.contains("18°"))
        assertTrue(dms.contains("30'"))
        assertTrue(dms.contains("73°"))
        assertTrue(dms.contains("50'"))
    }
}
