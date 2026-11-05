package com.gpsmapcamera.editor.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
fun LargeCardOverlayView(
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
            .border(1.dp, Color.White.copy(alpha = 0.15f), shape)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.showMap && mapBitmap != null) {
                    val mapWidth = (96.dp * state.mapScale).coerceAtLeast(65.dp)
                    MapThumbnailView(
                        mapBitmap = mapBitmap,
                        aspectRatio = state.mapAspectRatio,
                        modifier = Modifier.width(mapWidth)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.locationTitle,
                        color = textColor,
                        fontSize = (15.sp * state.fontSizeMultiplier),
                        fontWeight = FontWeight.Bold,
                        fontFamily = state.fontFamily.composeFontFamily,
                        textAlign = textAlign,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = state.streetAddress,
                        color = textColor.copy(alpha = 0.85f),
                        fontSize = (11.sp * state.fontSizeMultiplier),
                        fontFamily = state.fontFamily.composeFontFamily,
                        textAlign = textAlign,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Divider line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(Color.White.copy(alpha = 0.2f))
            )

            // Metadata Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = CoordinatesFormatter.format(state.latitude, state.longitude, state.coordinatesFormat),
                    color = textColor,
                    fontSize = (10.sp * state.fontSizeMultiplier),
                    fontWeight = FontWeight.Medium,
                    fontFamily = state.fontFamily.composeFontFamily
                )
                Text(
                    text = "Alt: ${String.format(Locale.US, "%.1f", state.altitudeMeters)}m",
                    color = textColor.copy(alpha = 0.85f),
                    fontSize = (10.sp * state.fontSizeMultiplier),
                    fontFamily = state.fontFamily.composeFontFamily
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${state.dayOfWeek}, ${state.customDate} ${state.customTime}",
                    color = textColor.copy(alpha = 0.85f),
                    fontSize = (9.5.sp * state.fontSizeMultiplier),
                    fontFamily = state.fontFamily.composeFontFamily
                )
                Text(
                    text = state.customTimezone,
                    color = accentColor,
                    fontSize = (9.5.sp * state.fontSizeMultiplier),
                    fontWeight = FontWeight.Bold,
                    fontFamily = state.fontFamily.composeFontFamily
                )
            }

            if (state.showNote && state.note.isNotBlank()) {
                Text(
                    text = "Note: ${state.note}",
                    color = accentColor,
                    fontSize = (10.sp * state.fontSizeMultiplier),
                    fontWeight = FontWeight.Medium,
                    fontFamily = state.fontFamily.composeFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
