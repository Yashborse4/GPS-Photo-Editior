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
 * exact reference formatting, and DateTimeFormatter utility operations.
 */
class GpsPhotoDataTest {

    @Test
    fun testDefaultGpsPhotoDataValues() {
        val data = GpsPhotoData()
        assertEquals(18.518084, data.latitude, 0.000001)
        assertEquals(73.835936, data.longitude, 0.000001)
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
    fun testFormattedCoordinatesExact() {
        val data = GpsPhotoData(latitude = 18.518084, longitude = 73.835936)
        assertEquals("Lat 18.518084° Long 73.835936°", data.formattedCoordinatesExact())
    }

    @Test
    fun testHeadingWithFlag() {
        val data = GpsPhotoData(locationHeading = "Pune, Maharashtra, India")
        assertEquals("Pune, Maharashtra, India 🇮🇳", data.headingWithFlag)
    }

    @Test
    fun testFormattedTimestampWithZone() {
        val data = GpsPhotoData(
            dateString = "19/10/2026",
            timeString = "12:18 PM",
            timezone = "GMT +05:30"
        )
        assertEquals("Monday, 19/10/2026 12:18 PM GMT +05:30", data.formattedTimestampWithZone())
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
        assertTrue(formattedTime.contains("02:30") && formattedTime.contains("PM"))
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

    @Test
    fun testDayOfWeekAutoCalculation() {
        assertEquals("Monday", AppDateTimeFormatter.calculateDayOfWeek("19/10/2026"))
        assertEquals("Tuesday", AppDateTimeFormatter.calculateDayOfWeek("20/10/2026"))
        assertEquals("Friday", AppDateTimeFormatter.calculateDayOfWeek("25/12/2026"))
        assertEquals("Wednesday", AppDateTimeFormatter.calculateDayOfWeek("2026-01-07"))
    }

    @Test
    fun testCustomDayOfWeekOverride() {
        val data = GpsPhotoData(
            dayOfWeek = "Special Tuesday",
            dateString = "19/10/2026",
            timeString = "12:18 PM",
            timezone = "GMT +05:30"
        )
        assertEquals("Special Tuesday, 19/10/2026 12:18 PM GMT +05:30", data.formattedTimestampWithZone())
    }

    @Test
    fun testExifDateTimeFormatting() {
        val exifDateTime = AppDateTimeFormatter.formatToExifDateTime("19/10/2026", "12:18 PM")
        assertEquals("2026:10:19 12:18:00", exifDateTime)
    }

    @Test
    fun testTimezoneOffsetExtraction() {
        assertEquals("+05:30", AppDateTimeFormatter.extractTimezoneOffset("GMT +05:30 (IST)"))
        assertEquals("-04:00", AppDateTimeFormatter.extractTimezoneOffset("GMT -04:00 (EDT)"))
        assertEquals("+00:00", AppDateTimeFormatter.extractTimezoneOffset("GMT +00:00 (UTC)"))
    }

    @Test
    fun testGpsDateStampAndTimeStamp() {
        assertEquals("2026:10:19", AppDateTimeFormatter.formatToGpsDateStamp("19/10/2026"))
        // 12:18 PM IST is 06:48 AM UTC
        val utcTime = AppDateTimeFormatter.formatToGpsTimeStamp("19/10/2026", "12:18 PM", "GMT +05:30")
        assertEquals("06:48:00", utcTime)
    }

    @Test
    fun testParseToEpochMillis() {
        val millis = AppDateTimeFormatter.parseToEpochMillis("19/10/2026", "12:18 PM", "GMT +05:30")
        assertTrue(millis > 0)
        // Verify roundtrip through UTC formatting
        val utcStamp = AppDateTimeFormatter.formatToGpsTimeStamp("19/10/2026", "12:18 PM", "GMT +05:30")
        assertEquals("06:48:00", utcStamp)
    }
}
