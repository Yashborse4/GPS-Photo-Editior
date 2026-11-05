package com.gpsphotocamera.locationeditor.viewmodel

import android.graphics.Bitmap
import com.google.android.gms.maps.model.LatLng
import com.gpsphotocamera.locationeditor.model.CoordinateValidationResult
import com.gpsphotocamera.locationeditor.model.LocationData
import com.gpsphotocamera.locationeditor.model.ThumbnailConfig

/**
 * Immutable UI State for the GPS Location Editor screen.
 */
data class LocationEditorUiState(
    val locationData: LocationData = LocationData.DEFAULT_PUNE,
    val validationResult: CoordinateValidationResult = CoordinateValidationResult.VALID,
    val zoomLevel: Float = 17.5f,
    val targetLatLng: LatLng = LatLng(18.511347, 73.833891),
    val capturedThumbnail: Bitmap? = null,
    val selectedThumbnailConfig: ThumbnailConfig = ThumbnailConfig.SQUARE_DEFAULT,
    val isSnapshotInProgress: Boolean = false,
    val statusMessage: String? = null,
    val showSnapshotDialog: Boolean = false,
    val showPhotoStampPreview: Boolean = false,
    val isManualMapDragSyncEnabled: Boolean = true,
    val cameraMoveTrigger: Long = 0L // Increment to command camera relocation
) {
    /**
     * Whether the entered coordinates are valid for map centering and marker placement.
     */
    val canRenderMarker: Boolean
        get() = validationResult.isValid && locationData.toLatLng() != null
}
