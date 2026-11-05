package com.gpsphotocamera.app

import com.gpsphotocamera.app.data.model.AppMapType
import com.gpsphotocamera.app.data.model.GpsPhotoData
import com.gpsphotocamera.app.data.model.StampStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests covering visual customization presets, normalized coordinate logic,
 * compass bearing calculations, and boundary edge cases.
 */
class VisualCustomizationTest {

    @Test
    fun testClassicPresetValuesMatchReference() {
        val data = GpsPhotoData().applyPreset(StampStyle.CLASSIC_BOTTOM)

        assertEquals(StampStyle.CLASSIC_BOTTOM, data.stampStyle)
        assertEquals(0.72f, data.backgroundOpacity, 0.01f)
        assertEquals(0.94f, data.panelWidthPercent, 0.01f)
        assertEquals(14f, data.cornerRadiusDp, 0.1f)
        assertEquals(0.50f, data.normX, 0.01f)
        assertEquals(0.88f, data.normY, 0.01f)
        assertEquals(1.0f, data.scale, 0.01f)
        assertTrue(data.showMap)
        assertEquals("SQUARE", data.mapAspectRatio)
    }

    @Test
    fun testCompactPresetValues() {
        val data = GpsPhotoData().applyPreset(StampStyle.COMPACT_MINIMAL)

        assertEquals(StampStyle.COMPACT_MINIMAL, data.stampStyle)
        assertEquals(0.80f, data.backgroundOpacity, 0.01f)
        assertEquals(0.96f, data.panelWidthPercent, 0.01f)
        assertEquals(8f, data.cornerRadiusDp, 0.1f)
        assertEquals(0.92f, data.normY, 0.01f)
    }

    @Test
    fun testModernCardPresetValues() {
        val data = GpsPhotoData().applyPreset(StampStyle.MODERN_CARD)

        assertEquals(StampStyle.MODERN_CARD, data.stampStyle)
        assertEquals(0.82f, data.backgroundOpacity, 0.01f)
        assertEquals(0.92f, data.panelWidthPercent, 0.01f)
        assertEquals(18f, data.cornerRadiusDp, 0.1f)
        assertEquals("LANDSCAPE", data.mapAspectRatio)
    }

    @Test
    fun testMinimalWatermarkPresetValues() {
        val data = GpsPhotoData().applyPreset(StampStyle.MINIMAL_WATERMARK)

        assertEquals(StampStyle.MINIMAL_WATERMARK, data.stampStyle)
        assertFalse(data.showMap)
        assertEquals("CENTER", data.textAlign)
    }

    @Test
    fun testResetPositionRestoresDefaultAnchor() {
        val altered = GpsPhotoData(normX = 0.20f, normY = 0.35f, scale = 1.6f)
        val reset = altered.resetPosition()

        assertEquals(0.50f, reset.normX, 0.001f)
        assertEquals(0.88f, reset.normY, 0.001f)
        assertEquals(1.0f, reset.scale, 0.001f)
    }

    @Test
    fun testCardinalDirectionsCalculations() {
        assertEquals("N", GpsPhotoData(headingDegrees = 0f).cardinalDirection)
        assertEquals("NE", GpsPhotoData(headingDegrees = 45f).cardinalDirection)
        assertEquals("E", GpsPhotoData(headingDegrees = 90f).cardinalDirection)
        assertEquals("SE", GpsPhotoData(headingDegrees = 145f).cardinalDirection)
        assertEquals("S", GpsPhotoData(headingDegrees = 180f).cardinalDirection)
        assertEquals("SW", GpsPhotoData(headingDegrees = 225f).cardinalDirection)
        assertEquals("W", GpsPhotoData(headingDegrees = 270f).cardinalDirection)
        assertEquals("NW", GpsPhotoData(headingDegrees = 315f).cardinalDirection)
        assertEquals("N", GpsPhotoData(headingDegrees = 360f).cardinalDirection)
    }

    @Test
    fun testAltitudeCompassFormatting() {
        val data = GpsPhotoData(altitudeMeters = 560.2, headingDegrees = 145f)
        val formatted = data.formattedAltitudeCompass()
        assertEquals("Altitude: 560.2m   Heading: 145° SE", formatted)
    }

    @Test
    fun testNegativeCoordinatesHandling() {
        // Sydney, Australia (-33.8688, 151.2093)
        val sydney = GpsPhotoData(latitude = -33.868820, longitude = 151.209296)
        assertEquals("33.868820° S", sydney.formattedLatitude())
        assertEquals("151.209296° E", sydney.formattedLongitude())

        // Santiago, Chile (-33.448890, -70.669265)
        val santiago = GpsPhotoData(latitude = -33.448890, longitude = -70.669265)
        assertEquals("33.448890° S", santiago.formattedLatitude())
        assertEquals("70.669265° W", santiago.formattedLongitude())
        assertEquals("Lat 33.448890° S  Long 70.669265° W", santiago.formattedCoordinates())
    }

    @Test
    fun testLongAddressAndCustomNotes() {
        val longAddress = "Plot 104, Ground Floor, Cyber City Tech Park, DLF Phase 2, Sector 24, Gurugram, Haryana 122002, India"
        val longNote = "Structural integrity inspection of reinforced concrete beam pillar #14B - Approved for phase 2 construction"
        val data = GpsPhotoData(postalAddress = longAddress, customNote = longNote)

        assertEquals(longAddress, data.postalAddress)
        assertEquals(longNote, data.customNote)
        assertTrue(data.showNote)
    }
}
