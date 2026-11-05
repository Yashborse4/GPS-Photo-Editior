package com.gpsphotocamera.locationeditor.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gpsphotocamera.locationeditor.ui.components.CoordinateInputCard
import com.gpsphotocamera.locationeditor.ui.components.LocationFormCard
import com.gpsphotocamera.locationeditor.ui.components.MapControlsBar
import com.gpsphotocamera.locationeditor.ui.components.PhotoStampSampleOverlay
import com.gpsphotocamera.locationeditor.ui.components.SatelliteMapView
import com.gpsphotocamera.locationeditor.ui.components.ThumbnailPreviewDialog
import com.gpsphotocamera.locationeditor.ui.theme.DarkSurface
import com.gpsphotocamera.locationeditor.ui.theme.DarkSurfaceElevated
import com.gpsphotocamera.locationeditor.ui.theme.GpsRed
import com.gpsphotocamera.locationeditor.ui.theme.TextPrimary
import com.gpsphotocamera.locationeditor.ui.theme.TextSecondary
import com.gpsphotocamera.locationeditor.viewmodel.LocationEditorIntent
import com.gpsphotocamera.locationeditor.viewmodel.LocationEditorViewModel

/**
 * Main Jetpack Compose Screen for the GPS Location Editor and Satellite Map Module.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationEditorScreen(
    viewModel: LocationEditorViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    // Show status messages in Snackbar
    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onIntent(LocationEditorIntent.ClearStatusMessage)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "GPS Location Editor",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Satellite Map & Stamp Generator",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    Icon(
                        Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = GpsRed,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(24.dp)
                    )
                },
                actions = {
                    // Quick trigger to view simulated photo stamp
                    IconButton(onClick = { viewModel.onIntent(LocationEditorIntent.TogglePhotoStampPreview(true)) }) {
                        Icon(
                            Icons.Default.Camera,
                            contentDescription = "Simulate Photo Stamp",
                            tint = GpsRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurfaceElevated
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkSurface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Satellite Map View with Red Location Pin
            SatelliteMapView(
                targetLatLng = uiState.targetLatLng,
                zoomLevel = uiState.zoomLevel,
                cameraMoveTrigger = uiState.cameraMoveTrigger,
                isSnapshotInProgress = uiState.isSnapshotInProgress,
                onSnapshotCaptured = { bitmap, density ->
                    viewModel.onIntent(LocationEditorIntent.OnSnapshotRawCaptured(bitmap, density))
                },
                onMapCenterChanged = { clickedLatLng ->
                    viewModel.onIntent(
                        LocationEditorIntent.UpdateFromMapDrag(
                            clickedLatLng.latitude,
                            clickedLatLng.longitude
                        )
                    )
                },
                onZoomChanged = { zoom ->
                    viewModel.onIntent(LocationEditorIntent.SetZoomLevel(zoom))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            )

            // 2. Map Controls (Zoom slider, center, snapshot, reset)
            MapControlsBar(
                zoomLevel = uiState.zoomLevel,
                isSnapshotInProgress = uiState.isSnapshotInProgress,
                onZoomChanged = { viewModel.onIntent(LocationEditorIntent.SetZoomLevel(it)) },
                onZoomIn = { viewModel.onIntent(LocationEditorIntent.ZoomIn) },
                onZoomOut = { viewModel.onIntent(LocationEditorIntent.ZoomOut) },
                onRecenter = { viewModel.onIntent(LocationEditorIntent.RecenterMap) },
                onReset = { viewModel.onIntent(LocationEditorIntent.ResetToDefaults) },
                onCaptureSnapshot = { viewModel.onIntent(LocationEditorIntent.RequestSnapshot) }
            )

            // 3. Coordinate Input & Validation Card
            CoordinateInputCard(
                locationData = uiState.locationData,
                validationResult = uiState.validationResult,
                onLatitudeChange = { viewModel.onIntent(LocationEditorIntent.UpdateLatitude(it)) },
                onLongitudeChange = { viewModel.onIntent(LocationEditorIntent.UpdateLongitude(it)) },
                onApplyPreset = { viewModel.onIntent(LocationEditorIntent.ApplyPreset(it)) }
            )

            // 4. Manual Location Information Form Card
            LocationFormCard(
                locationData = uiState.locationData,
                onHeadingChange = { viewModel.onIntent(LocationEditorIntent.UpdateHeading(it)) },
                onCountryChange = { viewModel.onIntent(LocationEditorIntent.UpdateCountry(it)) },
                onStateChange = { viewModel.onIntent(LocationEditorIntent.UpdateState(it)) },
                onCityChange = { viewModel.onIntent(LocationEditorIntent.UpdateCity(it)) },
                onPostalAddressChange = { viewModel.onIntent(LocationEditorIntent.UpdatePostalAddress(it)) },
                onPinCodeChange = { viewModel.onIntent(LocationEditorIntent.UpdatePinCode(it)) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Dialog: Satellite Map Thumbnail Inspector & Size Selector
        if (uiState.showSnapshotDialog) {
            ThumbnailPreviewDialog(
                bitmap = uiState.capturedThumbnail,
                selectedConfig = uiState.selectedThumbnailConfig,
                onSelectConfig = { config ->
                    viewModel.onIntent(LocationEditorIntent.SelectThumbnailConfig(config))
                    viewModel.onIntent(LocationEditorIntent.RequestSnapshot)
                },
                onOpenPhotoStampPreview = {
                    viewModel.onIntent(LocationEditorIntent.TogglePhotoStampPreview(true))
                },
                onDismiss = {
                    viewModel.onIntent(LocationEditorIntent.DismissSnapshotDialog)
                }
            )
        }

        // Dialog: Photo Stamp Simulation (Reproducing reference photo watermark)
        if (uiState.showPhotoStampPreview) {
            PhotoStampSampleOverlay(
                thumbnailBitmap = uiState.capturedThumbnail,
                locationData = uiState.locationData,
                onDismiss = {
                    viewModel.onIntent(LocationEditorIntent.TogglePhotoStampPreview(false))
                }
            )
        }
    }
}
