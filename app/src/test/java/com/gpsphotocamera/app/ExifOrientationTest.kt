package com.gpsphotocamera.app

import androidx.exifinterface.media.ExifInterface
import com.gpsphotocamera.app.engine.ExifUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExifOrientationTest {

    @Test
    fun testConvertDecimalToDmsRational_positiveLatitude() {
        // 18.518667 -> 18 deg, 31 min, 7.2012 sec
        val result = ExifUtils.convertDecimalToDmsRational(18.518667)
        assertTrue(result.startsWith("18/1,31/1,"))
    }

    @Test
    fun testConvertDecimalToDmsRational_longitude() {
        // 73.833756 -> 73 deg, 50 min, 1.5216 sec
        val result = ExifUtils.convertDecimalToDmsRational(73.833756)
        assertTrue(result.startsWith("73/1,50/1,"))
    }

    @Test
    fun testOrientationConstants() {
        assertEquals(1, ExifInterface.ORIENTATION_NORMAL)
        assertEquals(3, ExifInterface.ORIENTATION_ROTATE_180)
        assertEquals(6, ExifInterface.ORIENTATION_ROTATE_90)
        assertEquals(8, ExifInterface.ORIENTATION_ROTATE_270)
    }
}
