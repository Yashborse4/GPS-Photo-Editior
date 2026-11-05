package com.gpsphotocamera.locationeditor.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.gpsphotocamera.locationeditor.ui.theme.DarkBorder

/**
 * High-performance Satellite Map composable tailored for GPS Photo Camera.
 *
 * Configured with:
 * - Pure satellite imagery (GoogleMap.MAP_TYPE_SATELLITE)
 * - Custom red location pin placed at the exact target coordinates
 * - Camera centering and smooth zoom synchronization
 * - Clutter-free UI (no map compass/toolbar/zoom buttons obscuring snapshot area)
 * - Direct GoogleMap.snapshot hook via Compose MapEffect
 */
@Composable
fun SatelliteMapView(
    targetLatLng: LatLng,
    zoomLevel: Float,
    cameraMoveTrigger: Long,
    isSnapshotInProgress: Boolean,
    onSnapshotCaptured: (Bitmap, Float) -> Unit,
    onMapCenterChanged: (LatLng) -> Unit,
    onZoomChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    // Camera position state managing satellite viewport
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(targetLatLng, zoomLevel)
    }

    // Marker state tracking entered coordinates
    val markerState = rememberMarkerState(position = targetLatLng)

    // Red location marker icon descriptor matching GPS camera pin
    val markerIcon = remember(context) {
        RedLocationMarker.getBitmapDescriptor(context)
    }

    // Synchronize marker position when coordinates change
    LaunchedEffect(targetLatLng) {
        markerState.position = targetLatLng
    }

    // Synchronize camera when target coordinates or re-center trigger fires
    LaunchedEffect(targetLatLng, cameraMoveTrigger) {
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(targetLatLng, zoomLevel),
            durationMs = 600
        )
    }

    // Synchronize camera when zoom level changes from UI controls
    LaunchedEffect(zoomLevel) {
        if (cameraPositionState.position.zoom != zoomLevel) {
            cameraPositionState.animate(
                CameraUpdateFactory.zoomTo(zoomLevel),
                durationMs = 300
            )
        }
    }

    // Sync camera gesture changes back to ViewModel
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            onZoomChanged(cameraPositionState.position.zoom)
        }
    }

    // Satellite Map UI settings: Clean and uncluttered for photo snapshotting
    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false,
            rotationGesturesEnabled = false,
            tiltGesturesEnabled = false,
            scrollGesturesEnabled = true,
            zoomGesturesEnabled = true
        )
    }

    // Satellite Map Properties: Explicit satellite tiles
    val mapProperties = remember {
        MapProperties(
            mapType = MapType.SATELLITE,
            isBuildingEnabled = true,
            isTrafficEnabled = false
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings,
            onMapClick = { clickedLatLng ->
                // Allow tapping map to reposition marker and update coordinates
                onMapCenterChanged(clickedLatLng)
            }
        ) {
            // Render the authentic red GPS pin marker
            Marker(
                state = markerState,
                title = "Photo Location",
                snippet = "${targetLatLng.latitude}, ${targetLatLng.longitude}",
                icon = markerIcon,
                anchor = Offset(0.5f, 1.0f) // Pin tip at the coordinate
            )

            // Snapshot capture engine via Compose MapEffect
            if (isSnapshotInProgress) {
                MapEffect(isSnapshotInProgress) { googleMap ->
                    googleMap.snapshot { snapshotBitmap ->
                        if (snapshotBitmap != null) {
                            onSnapshotCaptured(snapshotBitmap, density)
                        }
                    }
                }
            }
        }
    }
}
