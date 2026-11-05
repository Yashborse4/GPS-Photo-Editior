package com.gpsphotocamera.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.gpsphotocamera.app.data.model.AppMapType
import com.gpsphotocamera.app.maps.MapSnapshotHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SatelliteMapCard(
    latitude: Double,
    longitude: Double,
    selectedMapType: AppMapType,
    onMapTypeChanged: (AppMapType) -> Unit,
    modifier: Modifier = Modifier
) {
    val targetLatLng = remember(latitude, longitude) { LatLng(latitude, longitude) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(targetLatLng, 15f)
    }

    LaunchedEffect(targetLatLng) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(targetLatLng, 15f)
    }

    // OpenStreetMap preview bitmap state
    var osmBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingOsm by remember { mutableStateOf(false) }

    LaunchedEffect(latitude, longitude, selectedMapType) {
        if (selectedMapType == AppMapType.OPEN_STREET_MAP) {
            isLoadingOsm = true
            val bmp = withContext(Dispatchers.IO) {
                MapSnapshotHelper.generateMapThumbnail(
                    latitude = latitude,
                    longitude = longitude,
                    mapType = AppMapType.OPEN_STREET_MAP,
                    size = 600
                )
            }
            osmBitmap = bmp
            isLoadingOsm = false
        }
    }

    val composeMapType = when (selectedMapType) {
        AppMapType.SATELLITE -> MapType.SATELLITE
        AppMapType.NORMAL, AppMapType.OPEN_STREET_MAP -> MapType.NORMAL
        AppMapType.TERRAIN -> MapType.TERRAIN
        AppMapType.HYBRID -> MapType.HYBRID
    }

    val mapProperties = remember(composeMapType) {
        MapProperties(mapType = composeMapType)
    }

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = true,
            myLocationButtonEnabled = false
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Map Preview (${selectedMapType.displayName})",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Map Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (selectedMapType == AppMapType.OPEN_STREET_MAP) {
                    if (isLoadingOsm && osmBitmap == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                Text(
                                    text = "Loading OpenStreetMap tiles…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (osmBitmap != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                bitmap = osmBitmap!!.asImageBitmap(),
                                contentDescription = "OpenStreetMap Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Attribution badge compliant with OpenStreetMap Foundation policy
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                shape = RoundedCornerShape(topStart = 8.dp),
                                modifier = Modifier.align(Alignment.BottomEnd)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "© OpenStreetMap contributors",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                } else {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = mapProperties,
                        uiSettings = mapUiSettings
                    ) {
                        Marker(
                            state = MarkerState(position = targetLatLng),
                            title = "Tagged Location"
                        )
                    }
                }
            }

            // Map Style selector chips (horizontal scrollable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppMapType.entries.forEach { type ->
                    val isSelected = selectedMapType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { onMapTypeChanged(type) },
                        label = { Text(type.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
    }
}
