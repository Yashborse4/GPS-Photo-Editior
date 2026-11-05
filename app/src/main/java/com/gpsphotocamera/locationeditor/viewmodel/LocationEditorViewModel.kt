package com.gpsphotocamera.locationeditor.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.gpsphotocamera.locationeditor.model.LocationData
import com.gpsphotocamera.locationeditor.model.ThumbnailConfig
import com.gpsphotocamera.locationeditor.snapshot.BitmapUtils
import com.gpsphotocamera.locationeditor.snapshot.MapSnapshotProvider
import com.gpsphotocamera.locationeditor.validation.CoordinateValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ViewModel orchestrating manual location editing, validation, satellite map synchronization,
 * and snapshot thumbnail generation.
 *
 * Implements MapSnapshotProvider for decoupled integration with photo rendering engines.
 */
class LocationEditorViewModel : ViewModel(), MapSnapshotProvider {

    private val _uiState = MutableStateFlow(LocationEditorUiState())
    val uiState: StateFlow<LocationEditorUiState> = _uiState.asStateFlow()

    // Expose captured snapshot to external photo stamp modules
    override val currentSnapshot: StateFlow<Bitmap?> = _uiState
        .map { it.capturedThumbnail }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Expose location metadata to external photo stamp modules
    override val currentLocationData: StateFlow<LocationData> = _uiState
        .map { it.locationData }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.locationData)

    init {
        // Run initial validation on the default reference coordinates
        validateAndUpdateCoordinates(
            _uiState.value.locationData.latitudeString,
            _uiState.value.locationData.longitudeString,
            triggerCameraMove = true
        )
    }

    /**
     * Entry point for processing UI actions and user interactions.
     */
    fun onIntent(intent: LocationEditorIntent) {
        when (intent) {
            is LocationEditorIntent.UpdateHeading -> {
                _uiState.update { it.copy(locationData = it.locationData.copy(heading = intent.value)) }
            }
            is LocationEditorIntent.UpdateCountry -> {
                _uiState.update { it.copy(locationData = it.locationData.copy(country = intent.value)) }
            }
            is LocationEditorIntent.UpdateState -> {
                _uiState.update { it.copy(locationData = it.locationData.copy(state = intent.value)) }
            }
            is LocationEditorIntent.UpdateCity -> {
                _uiState.update { it.copy(locationData = it.locationData.copy(city = intent.value)) }
            }
            is LocationEditorIntent.UpdatePostalAddress -> {
                _uiState.update { it.copy(locationData = it.locationData.copy(postalAddress = intent.value)) }
            }
            is LocationEditorIntent.UpdatePinCode -> {
                _uiState.update { it.copy(locationData = it.locationData.copy(pinCode = intent.value)) }
            }
            is LocationEditorIntent.UpdateLatitude -> {
                val sanitized = CoordinateValidator.sanitizeInput(intent.value)
                _uiState.update { it.copy(locationData = it.locationData.copy(latitudeString = sanitized)) }
                validateAndUpdateCoordinates(sanitized, _uiState.value.locationData.longitudeString, triggerCameraMove = true)
            }
            is LocationEditorIntent.UpdateLongitude -> {
                val sanitized = CoordinateValidator.sanitizeInput(intent.value)
                _uiState.update { it.copy(locationData = it.locationData.copy(longitudeString = sanitized)) }
                validateAndUpdateCoordinates(_uiState.value.locationData.latitudeString, sanitized, triggerCameraMove = true)
            }
            is LocationEditorIntent.SetZoomLevel -> {
                val clamped = intent.zoom.coerceIn(2.0f, 21.0f)
                _uiState.update { it.copy(zoomLevel = clamped) }
            }
            is LocationEditorIntent.ZoomIn -> {
                val newZoom = (_uiState.value.zoomLevel + 1.0f).coerceAtMost(21.0f)
                _uiState.update { it.copy(zoomLevel = newZoom) }
            }
            is LocationEditorIntent.ZoomOut -> {
                val newZoom = (_uiState.value.zoomLevel - 1.0f).coerceAtLeast(2.0f)
                _uiState.update { it.copy(zoomLevel = newZoom) }
            }
            is LocationEditorIntent.RecenterMap -> {
                _uiState.update { it.copy(cameraMoveTrigger = System.currentTimeMillis()) }
            }
            is LocationEditorIntent.ResetToDefaults -> {
                val defaultData = LocationData.DEFAULT_PUNE
                _uiState.update {
                    it.copy(
                        locationData = defaultData,
                        zoomLevel = 17.5f,
                        targetLatLng = LatLng(18.511347, 73.833891),
                        cameraMoveTrigger = System.currentTimeMillis(),
                        statusMessage = "Reset to default Pune location."
                    )
                }
                validateAndUpdateCoordinates(defaultData.latitudeString, defaultData.longitudeString, triggerCameraMove = true)
            }
            is LocationEditorIntent.ApplyPreset -> {
                val preset = intent.preset
                _uiState.update {
                    it.copy(
                        locationData = preset,
                        cameraMoveTrigger = System.currentTimeMillis(),
                        statusMessage = "Loaded preset: ${preset.heading}"
                    )
                }
                validateAndUpdateCoordinates(preset.latitudeString, preset.longitudeString, triggerCameraMove = true)
            }
            is LocationEditorIntent.SelectThumbnailConfig -> {
                _uiState.update { it.copy(selectedThumbnailConfig = intent.config) }
            }
            is LocationEditorIntent.RequestSnapshot -> {
                _uiState.update { it.copy(isSnapshotInProgress = true) }
            }
            is LocationEditorIntent.OnSnapshotRawCaptured -> {
                processRawSnapshot(intent.rawBitmap, intent.density)
            }
            is LocationEditorIntent.DismissSnapshotDialog -> {
                _uiState.update { it.copy(showSnapshotDialog = false) }
            }
            is LocationEditorIntent.TogglePhotoStampPreview -> {
                _uiState.update { it.copy(showPhotoStampPreview = intent.show) }
            }
            is LocationEditorIntent.ClearStatusMessage -> {
                _uiState.update { it.copy(statusMessage = null) }
            }
            is LocationEditorIntent.UpdateFromMapDrag -> {
                val latFormatted = String.format(Locale.US, "%.6f", intent.lat)
                val lonFormatted = String.format(Locale.US, "%.6f", intent.lon)
                _uiState.update {
                    it.copy(
                        locationData = it.locationData.copy(
                            latitudeString = latFormatted,
                            longitudeString = lonFormatted
                        ),
                        targetLatLng = LatLng(intent.lat, intent.lon)
                    )
                }
                validateAndUpdateCoordinates(latFormatted, lonFormatted, triggerCameraMove = false)
            }
        }
    }

    /**
     * Validate coordinates and update map target without crashing.
     */
    private fun validateAndUpdateCoordinates(
        latStr: String,
        lonStr: String,
        triggerCameraMove: Boolean
    ) {
        val result = CoordinateValidator.validateCoordinates(latStr, lonStr)
        _uiState.update { state ->
            val newLatLng = if (result.isValid) {
                val lat = latStr.toDouble()
                val lon = lonStr.toDouble()
                LatLng(lat, lon)
            } else {
                state.targetLatLng
            }
            state.copy(
                validationResult = result,
                targetLatLng = newLatLng,
                cameraMoveTrigger = if (triggerCameraMove && result.isValid) System.currentTimeMillis() else state.cameraMoveTrigger
            )
        }
    }

    /**
     * Process raw satellite map snapshot into the desired aspect ratio and styling.
     */
    private fun processRawSnapshot(rawBitmap: Bitmap, density: Float) {
        viewModelScope.launch {
            try {
                val config = _uiState.value.selectedThumbnailConfig
                val formattedBitmap = BitmapUtils.formatThumbnail(rawBitmap, config, density)
                _uiState.update {
                    it.copy(
                        capturedThumbnail = formattedBitmap,
                        isSnapshotInProgress = false,
                        showSnapshotDialog = true,
                        statusMessage = "Satellite thumbnail generated (${config.name})"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSnapshotInProgress = false,
                        statusMessage = "Snapshot processing failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * External contract implementation for MapSnapshotProvider.
     */
    override fun captureMapSnapshot(config: ThumbnailConfig, onComplete: (Bitmap?) -> Unit) {
        _uiState.update {
            it.copy(
                selectedThumbnailConfig = config,
                isSnapshotInProgress = true
            )
        }
        // The UI observing isSnapshotInProgress triggers the Map snapshot callback,
        // which then delivers through onComplete when formatted.
    }
}
