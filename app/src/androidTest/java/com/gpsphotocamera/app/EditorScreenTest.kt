package com.gpsphotocamera.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gpsphotocamera.app.data.model.GpsPhotoData
import com.gpsphotocamera.app.data.model.StampStyle
import com.gpsphotocamera.app.ui.components.LiveOverlayView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testLiveOverlayDisplaysHeadingAndCoordinates() {
        val data = GpsPhotoData(
            locationHeading = "Pune, Maharashtra, India",
            postalAddress = "FC Road, Shivajinagar, 411005",
            latitude = 18.520430,
            longitude = 73.856744,
            stampStyle = StampStyle.CLASSIC_BOTTOM
        )

        composeTestRule.setContent {
            LiveOverlayView(data = data, mapBitmap = null)
        }

        composeTestRule.onNodeWithText("Pune, Maharashtra, India").assertExists()
        composeTestRule.onNodeWithText("FC Road, Shivajinagar, 411005").assertExists()
        composeTestRule.onNodeWithText("GPS Map Camera").assertExists()
    }
}
