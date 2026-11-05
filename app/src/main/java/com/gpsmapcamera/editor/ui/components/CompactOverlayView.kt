package com.gpsmapcamera.editor.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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

@Composable
fun CompactOverlayView(
    state: GpsOverlayState,
    mapBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    val bgColor = Color(state.backgroundColorHex).copy(alpha = state.backgroundOpacity)
    val textColor = Color(state.textColorHex)
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
            .padding(horizontal = 10.dp, vertical = (state.panelHeightPaddingDp * 0.7f).dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.showMap && mapBitmap != null) {
                val mapSize = (54.dp * state.mapScale).coerceAtLeast(40.dp)
                MapThumbnailView(
                    mapBitmap = mapBitmap,
                    aspectRatio = state.mapAspectRatio,
                    modifier = Modifier.width(mapSize)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${state.locationTitle} • ${state.customDate} ${state.customTime}",
                    color = textColor,
                    fontSize = (11.5.sp * state.fontSizeMultiplier),
                    fontWeight = FontWeight.Bold,
                    fontFamily = state.fontFamily.composeFontFamily,
                    textAlign = textAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "${CoordinatesFormatter.format(state.latitude, state.longitude, state.coordinatesFormat)} • Alt: ${state.altitudeMeters.toInt()}m",
                    color = textColor.copy(alpha = 0.85f),
                    fontSize = (10.sp * state.fontSizeMultiplier),
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
