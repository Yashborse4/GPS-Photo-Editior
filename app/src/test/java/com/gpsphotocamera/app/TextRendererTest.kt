package com.gpsphotocamera.app

import com.gpsphotocamera.app.engine.TextRenderer
import org.junit.Assert.assertEquals
import org.junit.Test

class TextRendererTest {

    @Test
    fun testComputeScaledFontSize_baseResolution() {
        val baseWidth = 1086
        val headingRatio = 0.04236f // 46px on 1086px
        val scaled = TextRenderer.computeScaledFontSize(baseWidth, headingRatio, 1.0f)
        assertEquals(46f, scaled, 0.5f)
    }

    @Test
    fun testComputeScaledFontSize_highResolution4K() {
        val width4K = 3840
        val headingRatio = 0.04236f
        val scaled4K = TextRenderer.computeScaledFontSize(width4K, headingRatio, 1.0f)
        assertEquals(3840 * 0.04236f, scaled4K, 0.01f)
    }

    @Test
    fun testComputeScaledFontSize_withMultiplier() {
        val width = 1086
        val bodyRatio = 0.02670f // 29px
        val scaled = TextRenderer.computeScaledFontSize(width, bodyRatio, 1.2f)
        assertEquals(29f * 1.2f, scaled, 0.5f)
    }
}
