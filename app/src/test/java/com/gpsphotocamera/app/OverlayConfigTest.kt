package com.gpsphotocamera.app

import com.gpsphotocamera.app.engine.LocationData
import com.gpsphotocamera.app.engine.OverlayConfig
import com.gpsphotocamera.app.engine.PanelPosition
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayConfigTest {

    @Test
    fun testReferencePreset_calibratedProportions() {
        val config = OverlayConfig.createReferencePreset()

        // Reference photo dimensions: 1086 x 1448
        // Panel: 691 / 1086 ~= 0.63628
        assertEquals(0.63628f, config.panel.panelWidthPercent, 0.001f)
        // Map: 283 / 1086 ~= 0.26059
        assertEquals(0.26059f, config.map.widthPercent, 0.001f)
        // Margin: 45 / 1086 ~= 0.04144
        assertEquals(0.04144f, config.panel.horizontalMarginPercent, 0.001f)
        // Gap: 22 / 1086 ~= 0.02026
        assertEquals(0.02026f, config.panel.gapBetweenMapAndPanelPercent, 0.001f)
        // Bottom margin: 22 / 1448 ~= 0.01520
        assertEquals(0.01520f, config.panel.bottomMarginPercent, 0.001f)
        // Panel position
        assertEquals(PanelPosition.BOTTOM, config.panel.position)
    }

    @Test
    fun testReferencePreset_locationAndCoordinates() {
        val config = OverlayConfig.createReferencePreset()

        assertEquals("Pune, Maharashtra, India", config.location.heading)
        assertEquals("🇮🇳", config.location.flagEmoji)
        assertEquals("Pune, Maharashtra, India 🇮🇳", config.location.displayHeading)

        val coords = config.location.formattedCoordinates()
        assertEquals("Lat 18.518667° Long 73.833756°", coords)
    }

    @Test
    fun testCustomCoordinates_decimalPrecision() {
        val loc = LocationData(
            latitude = 12.9715987,
            longitude = 77.5945627,
            decimalPrecision = 4
        )
        val formatted = loc.formattedCoordinates()
        assertEquals("Lat 12.9716° Long 77.5946°", formatted)
    }

    @Test
    fun testReferencePreset_dateTime() {
        val config = OverlayConfig.createReferencePreset()
        val dtStr = config.dateTime.formattedString()
        assertEquals("Tuesday, 03/11/2026 02:45 PM GMT +05:30", dtStr)
    }

    @Test
    fun testReferencePreset_note() {
        val config = OverlayConfig.createReferencePreset()
        assertEquals("Note : Captured by GPS Map Camera", config.note.formattedString())
    }

    @Test
    fun testPanelOpacityAdjustment() {
        val panel = OverlayConfig.createReferencePreset().panel
        val updated = panel.withOpacity(0.5f)

        assertEquals(0.5f, updated.opacity, 0.02f)
    }
}
