package com.gpsphotocamera.locationeditor.viewmodel

import android.graphics.Bitmap
import com.gpsphotocamera.locationeditor.model.LocationData
import com.gpsphotocamera.locationeditor.model.ThumbnailConfig

/**
 * Sealed hierarchy of user intents / actions for the Location Editor module.
 */
sealed interface LocationEditorIntent {
    data class UpdateHeading(val value: String) : LocationEditorIntent
    data class UpdateCountry(val value: String) : LocationEditorIntent
    data class UpdateState(val value: String) : LocationEditorIntent
    data class UpdateCity(val value: String) : LocationEditorIntent
    data class UpdatePostalAddress(val value: String) : LocationEditorIntent
    data class UpdatePinCode(val value: String) : LocationEditorIntent
    data class UpdateLatitude(val value: String) : LocationEditorIntent
    data class UpdateLongitude(val value: String) : LocationEditorIntent
    data class SetZoomLevel(val zoom: Float) : LocationEditorIntent
    data object ZoomIn : LocationEditorIntent
    data object ZoomOut : LocationEditorIntent
    data object RecenterMap : LocationEditorIntent
    data object ResetToDefaults : LocationEditorIntent
    data class ApplyPreset(val preset: LocationData) : LocationEditorIntent
    data class SelectThumbnailConfig(val config: ThumbnailConfig) : LocationEditorIntent
    data object RequestSnapshot : LocationEditorIntent
    data class OnSnapshotRawCaptured(val rawBitmap: Bitmap, val density: Float) : LocationEditorIntent
    data object DismissSnapshotDialog : LocationEditorIntent
    data class TogglePhotoStampPreview(val show: Boolean) : LocationEditorIntent
    data object ClearStatusMessage : LocationEditorIntent
    data class UpdateFromMapDrag(val lat: Double, val lon: Double) : LocationEditorIntent
}
