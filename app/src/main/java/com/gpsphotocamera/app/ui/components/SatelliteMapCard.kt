package com.gpsphotocamera.app.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gpsphotocamera.app.data.model.AppMapType
import com.gpsphotocamera.app.maps.MapSnapshotHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Interactive map card displaying high-detail zoom-in satellite/aerial imagery,
 * with zoom controls (+ / - buttons and quick zoom presets 16x - 19x),
 * style switcher (Satellite, OpenStreetMap, Terrain, Hybrid),
 * and custom satellite image upload support.
 */
@Composable
fun SatelliteMapCard(
    latitude: Double,
    longitude: Double,
    selectedMapType: AppMapType,
    onMapTypeChanged: (AppMapType) -> Unit,
    mapZoom: Int = 18,
    onMapZoomChanged: ((Int) -> Unit)? = null,
    customMapImageUri: String? = null,
    onCustomMapImageSelected: ((String?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Satellite / OSM map bitmap state
    var mapBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingMap by remember { mutableStateOf(false) }

    val customMapPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        onCustomMapImageSelected?.invoke(uri?.toString())
    }

    LaunchedEffect(latitude, longitude, selectedMapType, customMapImageUri, mapZoom) {
        if (customMapImageUri.isNullOrBlank()) {
            isLoadingMap = true
            val bmp = withContext(Dispatchers.IO) {
                MapSnapshotHelper.generateMapThumbnail(
                    latitude = latitude,
                    longitude = longitude,
                    mapType = selectedMapType,
                    zoom = mapZoom,
                    size = 600
                )
            }
            mapBitmap = bmp
            isLoadingMap = false
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Map Type Title & Custom Map Clear Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (!customMapImageUri.isNullOrBlank()) {
                            "Custom Satellite Image"
                        } else {
                            "Satellite Map Preview (${selectedMapType.displayName})"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                if (!customMapImageUri.isNullOrBlank()) {
                    IconButton(
                        onClick = { onCustomMapImageSelected?.invoke(null) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear custom map",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Map Container Box with Zoom HUD Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (!customMapImageUri.isNullOrBlank()) {
                    // Show User-Uploaded Satellite Image
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(customMapImageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "User uploaded satellite map",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (isLoadingMap && mapBitmap == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Text(
                                text = "Fetching high-resolution satellite imagery…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (mapBitmap != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = mapBitmap!!.asImageBitmap(),
                            contentDescription = "Satellite Map Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Subtle loading spinner in corner if refreshing zoom
                        if (isLoadingMap) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopStart)
                                    .padding(8.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        }

                        // Attribution badge
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
                                    text = if (selectedMapType == AppMapType.SATELLITE) "© Esri / Maxar World Imagery" else "© OpenStreetMap contributors",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Interactive Floating Zoom Control HUD (Top Right)
                if (customMapImageUri.isNullOrBlank() && onMapZoomChanged != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            // Zoom In Button (+)
                            IconButton(
                                onClick = {
                                    if (mapZoom < 19) onMapZoomChanged(mapZoom + 1)
                                },
                                enabled = mapZoom < 19,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Zoom in",
                                    tint = if (mapZoom < 19) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Zoom level pill
                            Text(
                                text = "${mapZoom}x",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )

                            // Zoom Out Button (-)
                            IconButton(
                                onClick = {
                                    if (mapZoom > 13) onMapZoomChanged(mapZoom - 1)
                                },
                                enabled = mapZoom > 13,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Zoom out",
                                    tint = if (mapZoom > 13) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Zoom Presets Row
            if (customMapImageUri.isNullOrBlank() && onMapZoomChanged != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Zoom:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val presets = listOf(
                        Pair("Area", 16),
                        Pair("Block", 17),
                        Pair("Building (Default)", 18),
                        Pair("Close-up", 19)
                    )

                    presets.forEach { (label, zoomVal) ->
                        val isSelected = mapZoom == zoomVal
                        FilterChip(
                            selected = isSelected,
                            onClick = { onMapZoomChanged(zoomVal) },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // Map Style Selector & Custom Map Upload Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Map Style selector chips (horizontal scrollable)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppMapType.entries.forEach { type ->
                        val isSelected = selectedMapType == type && customMapImageUri.isNullOrBlank()
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onCustomMapImageSelected?.invoke(null)
                                onMapTypeChanged(type)
                            },
                            label = { Text(type.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Custom satellite image upload button
                OutlinedButton(
                    onClick = {
                        customMapPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Upload Satellite Photo",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (!customMapImageUri.isNullOrBlank()) "Change" else "Upload Map",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
