package com.gpsmapcamera.editor.ui.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gpsmapcamera.editor.data.model.GpsOverlayState
import com.gpsmapcamera.editor.data.model.LayoutPreset

@Composable
fun OverlayDispatcher(
    state: GpsOverlayState,
    mapBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    when (state.preset) {
        LayoutPreset.CLASSIC -> ClassicOverlayView(state = state, mapBitmap = mapBitmap, modifier = modifier)
        LayoutPreset.COMPACT -> CompactOverlayView(state = state, mapBitmap = mapBitmap, modifier = modifier)
        LayoutPreset.LARGE_CARD -> LargeCardOverlayView(state = state, mapBitmap = mapBitmap, modifier = modifier)
        LayoutPreset.MINIMAL -> MinimalOverlayView(state = state, modifier = modifier)
    }
}
