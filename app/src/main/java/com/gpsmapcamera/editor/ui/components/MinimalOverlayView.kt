package com.gpsmapcamera.editor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gpsmapcamera.editor.data.model.GpsOverlayState
import com.gpsmapcamera.editor.data.model.OverlayTextAlign
import com.gpsmapcamera.editor.domain.util.CoordinatesFormatter

@Composable
fun MinimalOverlayView(
    state: GpsOverlayState,
    modifier: Modifier = Modifier
) {
    val textColor = Color(state.textColorHex)
    val bgColor = if (state.backgroundOpacity > 0.05f) {
        Color(state.backgroundColorHex).copy(alpha = state.backgroundOpacity)
    } else Color.Transparent

    val textAlign = when (state.textAlign) {
        OverlayTextAlign.LEFT -> TextAlign.Start
        OverlayTextAlign.CENTER -> TextAlign.Center
        OverlayTextAlign.RIGHT -> TextAlign.End
    }

    val shadowStyle = TextStyle(
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.9f),
            offset = Offset(2f, 2f),
            blurRadius = 6f
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth(state.panelWidthPercent)
            .clip(RoundedCornerShape(state.cornerRadiusDp.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = state.locationTitle,
                color = textColor,
                fontSize = (14.sp * state.fontSizeMultiplier),
                fontWeight = FontWeight.Bold,
                fontFamily = state.fontFamily.composeFontFamily,
                textAlign = textAlign,
                style = shadowStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = CoordinatesFormatter.format(state.latitude, state.longitude, state.coordinatesFormat),
                color = textColor,
                fontSize = (11.sp * state.fontSizeMultiplier),
                fontWeight = FontWeight.Medium,
                fontFamily = state.fontFamily.composeFontFamily,
                textAlign = textAlign,
                style = shadowStyle,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "${state.customDate}   ${state.customTime}   ${state.customTimezone}",
                color = textColor.copy(alpha = 0.9f),
                fontSize = (10.sp * state.fontSizeMultiplier),
                fontFamily = state.fontFamily.composeFontFamily,
                textAlign = textAlign,
                style = shadowStyle,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
