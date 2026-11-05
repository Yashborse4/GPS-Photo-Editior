package com.gpsmapcamera.editor.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.gpsmapcamera.editor.data.model.MapAspectRatio

@Composable
fun MapThumbnailView(
    mapBitmap: Bitmap?,
    aspectRatio: MapAspectRatio,
    modifier: Modifier = Modifier
) {
    if (mapBitmap == null) return

    val shape = when (aspectRatio) {
        MapAspectRatio.CIRCLE -> CircleShape
        else -> RoundedCornerShape(8.dp)
    }

    val ratio = when (aspectRatio) {
        MapAspectRatio.SQUARE, MapAspectRatio.CIRCLE -> 1.0f
        MapAspectRatio.LANDSCAPE -> 4f / 3f
        MapAspectRatio.WIDE -> 16f / 9f
    }

    Box(
        modifier = modifier
            .aspectRatio(ratio)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
    ) {
        Image(
            bitmap = mapBitmap.asImageBitmap(),
            contentDescription = "Satellite Map",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
