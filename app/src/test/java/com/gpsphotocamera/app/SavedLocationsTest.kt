package com.gpsphotocamera.app

import com.gpsphotocamera.app.data.model.AppMapType
import com.gpsphotocamera.app.data.model.SavedLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for SavedLocation model, coordinate formatting, and bookmark management.
 */
class SavedLocationsTest {

    @Test
    fun testSavedLocationFormattedCoordinatesNorthernEastern() {
        val loc = SavedLocation(
            title = "Pune Site",
            heading = "Pune, Maharashtra, India",
            address = "Shivaji Nagar",
            latitude = 18.520430,
            longitude = 73.856744
        )
        assertEquals("18.520430° N, 73.856744° E", loc.formattedCoordinates)
        assertEquals(AppMapType.OPEN_STREET_MAP, loc.mapType)
        assertNotNull(loc.id)
    }

    @Test
    fun testSavedLocationFormattedCoordinatesSouthernWestern() {
        val loc = SavedLocation(
            title = "Sydney & Rio",
            heading = "Rio de Janeiro, Brazil",
            address = "Copacabana",
            latitude = -22.906800,
            longitude = -43.172900
        )
        assertEquals("22.906800° S, 43.172900° W", loc.formattedCoordinates)
    }

    @Test
    fun testSavedLocationTimestampAndNote() {
        val customNote = "Foundation concrete pouring milestone"
        val loc = SavedLocation(
            title = "Project Alpha",
            heading = "Mumbai",
            address = "Colaba",
            latitude = 18.922000,
            longitude = 72.834700,
            note = customNote
        )
        assertEquals(customNote, loc.note)
        assertTrue(loc.timestamp > 0)
    }
}
