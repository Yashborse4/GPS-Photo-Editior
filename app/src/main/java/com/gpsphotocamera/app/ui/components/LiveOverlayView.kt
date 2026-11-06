package com.gpsphotocamera.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gpsphotocamera.app.data.model.GpsPhotoData

/**
 * Compose live overlay component rendering the 3-component GPS metadata stamp:
 * 1. Satellite map thumbnail on bottom-left with Google watermark
 * 2. Translucent black information panel on bottom-right with heading, address, coords, date/time, note
 * 3. Small GPS Map Camera branding badge floating atop the right of the information panel
 */
@Composable
fun LiveOverlayView(
    data: GpsPhotoData,
    mapBitmap: Bitmap? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Floating Top-Right Branding Badge
        Surface(
            color = Color(0xDE222428),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            modifier = Modifier.padding(end = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0288D1)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF18202C))
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "GPS Map Camera",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom Row: Map Thumbnail + Information Panel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Map Thumbnail
            if (data.showMap) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E2E22))
                ) {
                    if (mapBitmap != null) {
                        Image(
                            bitmap = mapBitmap.asImageBitmap(),
                            contentDescription = "Map Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(
                        text = "Google",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    )
                }
            }

            // Information Card
            Surface(
                color = Color(data.backgroundColorHex.toInt()).copy(alpha = data.backgroundOpacity),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Heading with Flag
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = data.locationHeading,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (data.locationHeading.contains("India", ignoreCase = true) && !data.locationHeading.contains("🇮🇳")) {
                            Text(
                                text = " 🇮🇳",
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Postal Address
                    if (data.postalAddress.isNotBlank()) {
                        Text(
                            text = data.postalAddress,
                            color = Color.White,
                            fontSize = 11.sp,
                            maxLines = 2
                        )
                    }

                    // Coordinates
                    Text(
                        text = data.formattedCoordinatesExact(),
                        color = Color.White,
                        fontSize = 11.sp
                    )

                    // Date & Time
                    Text(
                        text = data.formattedTimestampWithZone(),
                        color = Color.White,
                        fontSize = 11.sp
                    )

                    // Note
                    if (data.showNote) {
                        Text(
                            text = "Note : " + data.customNote.ifBlank { "Captured by GPS Map Camera" },
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
