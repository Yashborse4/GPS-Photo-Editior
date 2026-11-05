package com.gpsphotocamera.locationeditor.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gpsphotocamera.locationeditor.model.LocationData
import com.gpsphotocamera.locationeditor.ui.theme.DarkBorder
import com.gpsphotocamera.locationeditor.ui.theme.GpsRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reusable demonstration component illustrating how the captured satellite map Bitmap
 * and manually edited location metadata are stamped onto a photograph.
 *
 * Visually reproduces the layout of the reference GPS Map Camera application:
 * - High-contrast semi-transparent bottom overlay badge
 * - Left: Captured satellite map thumbnail with the red location pin
 * - Right: Heading, complete address, coordinates (decimal + DMS), and date-time stamp
 */
@Composable
fun PhotoStampSampleOverlay(
    thumbnailBitmap: Bitmap?,
    locationData: LocationData,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a 'GMT' Z", Locale.US)
    val currentDateStr = dateFormat.format(Date())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Simulated photograph background (ambient gradient)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF2C3E50),
                                    Color(0xFF1A252F),
                                    Color(0xFF0F1419)
                                )
                            )
                        )
                ) {
                    Text(
                        text = "Camera Viewfinder Simulation",
                        color = Color(0x66FFFFFF),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                // GPS Map Camera Stamp Badge (Anchored to bottom of photograph)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color(0xCC000000)) // 80% opacity dark overlay
                        .border(
                            width = 1.dp,
                            color = Color(0x33FFFFFF),
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                        )
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Satellite Map Thumbnail
                        if (thumbnailBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                            ) {
                                Image(
                                    bitmap = thumbnailBitmap.asImageBitmap(),
                                    contentDescription = "GPS Satellite Stamp",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF222222))
                                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Map\nPending",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // 2. Location Metadata Text Block
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Location Heading
                            Text(
                                text = locationData.heading,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )

                            // Complete Address
                            Text(
                                text = locationData.postalAddress,
                                color = Color(0xFFDDDDDD),
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                maxLines = 2
                            )

                            // Decimal Coordinates
                            Text(
                                text = "Lat ${locationData.latitudeString}°  Long ${locationData.longitudeString}°",
                                color = GpsRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            // DMS Coordinates
                            Text(
                                text = locationData.toDmsCoordinates(),
                                color = Color(0xFFAAAAAA),
                                fontSize = 9.sp
                            )

                            // Timestamp
                            Text(
                                text = currentDateStr,
                                color = Color(0xFF888888),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
