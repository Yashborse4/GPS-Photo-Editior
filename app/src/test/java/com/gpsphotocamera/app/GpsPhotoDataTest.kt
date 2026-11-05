package com.gpsphotocamera.app

import com.gpsphotocamera.app.data.model.AppMapType
import com.gpsphotocamera.app.data.model.GpsPhotoData
import com.gpsphotocamera.app.data.model.StampStyle
import com.gpsphotocamera.app.utils.AppDateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for GpsPhotoData formatting, cardinal coordinate calculation,
 * and DateTimeFormatter utility operations.
 */
class GpsPhotoDataTest {

    @Test
    fun testDefaultGpsPhotoDataValues() {
        val data = GpsPhotoData()
        assertEquals(18.520430, data.latitude, 0.000001)
        assertEquals(73.856744, data.longitude, 0.000001)
        assertEquals("Pune, Maharashtra, India", data.locationHeading)
        assertEquals(StampStyle.CLASSIC_BOTTOM, data.stampStyle)
        assertEquals(AppMapType.SATELLITE, data.mapType)
    }

    @Test
    fun testFormattedLatitudeNorthernHemisphere() {
        val data = GpsPhotoData(latitude = 18.518667)
        assertEquals("18.518667° N", data.formattedLatitude())
    }

    @Test
    fun testFormattedLatitudeSouthernHemisphere() {
        val data = GpsPhotoData(latitude = -33.868820)
        assertEquals("33.868820° S", data.formattedLatitude())
    }

    @Test
    fun testFormattedLongitudeEasternHemisphere() {
        val data = GpsPhotoData(longitude = 73.833756)
        assertEquals("73.833756° E", data.formattedLongitude())
    }

    @Test
    fun testFormattedLongitudeWesternHemisphere() {
        val data = GpsPhotoData(longitude = -122.084000)
        assertEquals("122.084000° W", data.formattedLongitude())
    }

    @Test
    fun testFormattedCoordinatesCombined() {
        val data = GpsPhotoData(latitude = 18.518667, longitude = 73.833756)
        assertEquals("Lat 18.518667° N  Long 73.833756° E", data.formattedCoordinates())
    }

    @Test
    fun testFormattedTimestampWithZone() {
        val data = GpsPhotoData(
            dateString = "03/11/2026",
            timeString = "02:45:00 PM",
            timezone = "GMT +05:30"
        )
        assertEquals("03/11/2026 02:45:00 PM GMT +05:30", data.formattedTimestampWithZone())
    }

    @Test
    fun testTimezoneFormatting() {
        val tzString = AppDateTimeFormatter.getCurrentTimezoneString()
        assertNotNull(tzString)
        assertTrue(tzString.startsWith("GMT "))
    }

    @Test
    fun testTimeFormatting() {
        val formattedTime = AppDateTimeFormatter.formatTime(14, 30)
        // 14:30 should format as 02:30:00 PM
        assertTrue(formattedTime.contains("02:30:00") && formattedTime.contains("PM"))
    }

    @Test
    fun testCommonTimezonesListNotEmpty() {
        val timezones = AppDateTimeFormatter.commonTimezones
        assertTrue(timezones.isNotEmpty())
        assertTrue(timezones.any { it.contains("GMT +05:30") })
        assertTrue(timezones.any { it.contains("GMT +00:00") })
    }

    @Test
    fun testOpenStreetMapTypeAndLayerConfig() {
        val mapType = AppMapType.OPEN_STREET_MAP
        assertEquals("OpenStreetMap", mapType.displayName)
        assertTrue(AppMapType.entries.contains(AppMapType.OPEN_STREET_MAP))
    }
}
