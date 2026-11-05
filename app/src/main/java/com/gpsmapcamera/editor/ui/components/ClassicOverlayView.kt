package com.gpsmapcamera.editor.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gpsmapcamera.editor.data.model.GpsOverlayState
import com.gpsmapcamera.editor.data.model.OverlayTextAlign
import com.gpsmapcamera.editor.domain.util.CoordinatesFormatter
import java.util.Locale

@Composable
fun ClassicOverlayView(
    state: GpsOverlayState,
    mapBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    val bgColor = Color(state.backgroundColorHex).copy(alpha = state.backgroundOpacity)
    val textColor = Color(state.textColorHex)
    val accentColor = Color(state.accentColorHex)
    val shape = RoundedCornerShape(state.cornerRadiusDp.dp)

    val textAlign = when (state.textAlign) {
        OverlayTextAlign.LEFT -> TextAlign.Start
        OverlayTextAlign.CENTER -> TextAlign.Center
        OverlayTextAlign.RIGHT -> TextAlign.End
    }

    Box(
        modifier = modifier
            .fillMaxWidth(state.panelWidthPercent)
            .clip(shape)
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = state.panelHeightPaddingDp.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Satellite Map Thumbnail
            if (state.showMap && mapBitmap != null) {
                val mapWidth = (88.dp * state.mapScale).coerceAtLeast(60.dp)
                MapThumbnailView(
                    mapBitmap = mapBitmap,
                    aspectRatio = state.mapAspectRatio,
                    modifier = Modifier.width(mapWidth)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            // Right: Multi-line Typography
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy((state.lineSpacingDp * 0.75f).dp)
            ) {
                // Line 1: Location Title (Bold)
                Text(
                    text = state.locationTitle,
                    color = textColor,
                    fontSize = (13.sp * state.fontSizeMultiplier),
                    fontWeight = FontWeight.Bold,
                    fontFamily = state.fontFamily.composeFontFamily,
                    textAlign = textAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                // Line 2: Street Address
                Text(
                    text = state.streetAddress,
                    color = textColor.copy(alpha = 0.90f),
                    fontSize = (10.5.sp * state.fontSizeMultiplier),
                    fontWeight = if (state.isBold) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = state.fontFamily.composeFontFamily,
                    textAlign = textAlign,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                // Line 3: Lat & Long Coordinates
                Text(
                    text = CoordinatesFormatter.format(state.latitude, state.longitude, state.coordinatesFormat),
                    color = textColor,
                    fontSize = (10.sp * state.fontSizeMultiplier),
                    fontWeight = FontWeight.Medium,
                    fontFamily = state.fontFamily.composeFontFamily,
                    textAlign = textAlign,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )

                // Line 4: Altitude & Compass Heading
                Text(
                    text = "Altitude: ${String.format(Locale.US, "%.1f", state.altitudeMeters)}m   Heading: ${state.headingDegrees.toInt()}° ${state.cardinalDirection}",
                    color = textColor.copy(alpha = 0.85f),
                    fontSize = (9.5.sp * state.fontSizeMultiplier),
                    fontFamily = state.fontFamily.composeFontFamily,
                    textAlign = textAlign,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )

                // Line 5: Date, Time & Timezone
                Text(
                    text = "${state.dayOfWeek}, ${state.customDate}  ${state.customTime}  ${state.customTimezone}",
                    color = textColor.copy(alpha = 0.85f),
                    fontSize = (9.5.sp * state.fontSizeMultiplier),
                    fontFamily = state.fontFamily.composeFontFamily,
                    textAlign = textAlign,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )

                // Line 6: Custom Note (if enabled)
                if (state.showNote && state.note.isNotBlank()) {
                    Text(
                        text = "Note: ${state.note}",
                        color = accentColor,
                        fontSize = (9.5.sp * state.fontSizeMultiplier),
                        fontWeight = FontWeight.Medium,
                        fontFamily = state.fontFamily.composeFontFamily,
                        textAlign = textAlign,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
