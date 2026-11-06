package com.gpsphotocamera.app.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.gpsphotocamera.app.data.model.AppMapType
import com.gpsphotocamera.app.data.model.GpsPhotoData
import com.gpsphotocamera.app.data.model.SavedLocation
import com.gpsphotocamera.app.data.model.StampStyle
import com.gpsphotocamera.app.data.repository.SavedLocationsRepository
import com.gpsphotocamera.app.domain.usecase.GenerateOverlayUseCase
import com.gpsphotocamera.app.image.BitmapHolder
import com.gpsphotocamera.app.image.BitmapUtils
import com.gpsphotocamera.app.image.ExifExtractor
import com.gpsphotocamera.app.utils.AppDateTimeFormatter
import com.gpsphotocamera.app.utils.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * ViewModel managing the entire GPS Photo Editor state.
 * Allows independent editing of:
 * - Photo Uri
 * - Latitude and Longitude coordinates
 * - Location Heading
 * - Postal Address
 * - Date, Time, and Timezone
 * - Custom Note
 * - Stamp Style & Map Style
 */
class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val generateOverlayUseCase = GenerateOverlayUseCase(application.applicationContext)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application.applicationContext)
    private val savedLocationsRepository = SavedLocationsRepository(application.applicationContext)

    val savedLocations: StateFlow<List<SavedLocation>> = savedLocationsRepository.savedLocations

    private val _photoData = MutableStateFlow(
        GpsPhotoData(
            latitude = 18.518084,
            longitude = 73.835936,
            locationHeading = "Pune, Maharashtra, India",
            postalAddress = "870/7, Bhandarkar Rd, Near Hdfc Bank, Shreeman Society, Deccan Gymkhana, Pune, Maharashtra 411004, India",
            customNote = "Captured by GPS Map Camera",
            dateString = "19/10/2026",
            timeString = "12:18 PM",
            timezone = "GMT +05:30",
            mapType = AppMapType.SATELLITE
        )
    )
    val photoData: StateFlow<GpsPhotoData> = _photoData.asStateFlow()

    // String representations for text input fields
    val latitudeInput = MutableStateFlow("18.518084")
    val longitudeInput = MutableStateFlow("73.835936")
    val locationHeadingInput = MutableStateFlow("Pune, Maharashtra, India")
    val postalAddressInput = MutableStateFlow("870/7, Bhandarkar Rd, Near Hdfc Bank, Shreeman Society, Deccan Gymkhana, Pune, Maharashtra 411004, India")
    val dayOfWeekInput = MutableStateFlow("Monday")
    val dateInput = MutableStateFlow("19/10/2026")
    val timeInput = MutableStateFlow("12:18 PM")
    val timezoneInput = MutableStateFlow("GMT +05:30")
    val customNoteInput = MutableStateFlow("Captured by GPS Map Camera")

    // UI generation & loading state
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationError = MutableStateFlow<String?>(null)
    val generationError: StateFlow<String?> = _generationError.asStateFlow()

    fun setInitialPhotoUri(uriString: String) {
        if (uriString.isBlank()) return
        _photoData.update { it.copy(originalImageUri = uriString) }

        // Load bitmap into cache and extract EXIF
        viewModelScope.launch {
            val uri = Uri.parse(uriString)
            val context = getApplication<Application>().applicationContext
            val bitmap = BitmapUtils.decodeSampledBitmapFromUri(context, uri)
            BitmapHolder.originalBitmap = bitmap

            // Read EXIF if available to optionally pre-fill coordinates
            val exif = ExifExtractor.extract(context, uri)
            if (exif.latitude != null && exif.longitude != null) {
                updateCoordinates(exif.latitude, exif.longitude)
            }
            if (!exif.dateString.isNullOrBlank()) {
                dateInput.value = exif.dateString
                val computedDay = AppDateTimeFormatter.calculateDayOfWeek(exif.dateString) ?: "Monday"
                dayOfWeekInput.value = computedDay
                _photoData.update { it.copy(dateString = exif.dateString, dayOfWeek = computedDay) }
            }
            if (!exif.timeString.isNullOrBlank()) {
                timeInput.value = exif.timeString
                _photoData.update { it.copy(timeString = exif.timeString) }
            }
        }
    }

    /**
     * Updates photograph while intelligently preserving the currently configured
     * GPS coordinates, address, and stamp settings for batch/multi-photo workflows.
     */
    fun changePhotoUri(uriString: String, preserveLocationSettings: Boolean = true) {
        if (uriString.isBlank()) return
        _photoData.update { it.copy(originalImageUri = uriString) }

        viewModelScope.launch {
            val uri = Uri.parse(uriString)
            val context = getApplication<Application>().applicationContext
            val bitmap = BitmapUtils.decodeSampledBitmapFromUri(context, uri)
            BitmapHolder.originalBitmap = bitmap

            if (!preserveLocationSettings) {
                val exif = ExifExtractor.extract(context, uri)
                if (exif.latitude != null && exif.longitude != null) {
                    updateCoordinates(exif.latitude, exif.longitude)
                }
                if (!exif.dateString.isNullOrBlank()) {
                    dateInput.value = exif.dateString
                    val computedDay = AppDateTimeFormatter.calculateDayOfWeek(exif.dateString) ?: "Monday"
                    dayOfWeekInput.value = computedDay
                    _photoData.update { it.copy(dateString = exif.dateString, dayOfWeek = computedDay) }
                }
                if (!exif.timeString.isNullOrBlank()) {
                    timeInput.value = exif.timeString
                    _photoData.update { it.copy(timeString = exif.timeString) }
                }
            }
        }
    }

    /**
     * Bookmarks the current location parameters into persistent storage.
     */
    fun saveCurrentLocation(title: String) {
        viewModelScope.launch {
            val loc = SavedLocation(
                title = title.ifBlank { locationHeadingInput.value.ifBlank { "Saved Location" } },
                heading = locationHeadingInput.value,
                address = postalAddressInput.value,
                latitude = _photoData.value.latitude,
                longitude = _photoData.value.longitude,
                timezone = timezoneInput.value,
                mapType = _photoData.value.mapType,
                note = customNoteInput.value,
                timestamp = System.currentTimeMillis()
            )
            savedLocationsRepository.saveLocation(loc)
        }
    }

    /**
     * Applies a saved/favorite location to the active photo editor state.
     */
    fun applySavedLocation(location: SavedLocation) {
        updateCoordinates(location.latitude, location.longitude)
        updateHeading(location.heading)
        updatePostalAddress(location.address)
        updateTimezone(location.timezone)
        updateMapType(location.mapType)
        if (location.note.isNotBlank()) {
            updateNote(location.note)
        }
    }

    /**
     * Removes a saved location from persistent storage.
     */
    fun deleteSavedLocation(id: String) {
        viewModelScope.launch {
            savedLocationsRepository.deleteLocation(id)
        }
    }

    fun updateHeading(value: String) {
        locationHeadingInput.value = value
        _photoData.update { it.copy(locationHeading = value) }
    }

    fun updatePostalAddress(value: String) {
        postalAddressInput.value = value
        _photoData.update { it.copy(postalAddress = value) }
    }

    fun updateLatitude(value: String) {
        latitudeInput.value = value
        val parsed = value.toDoubleOrNull()
        if (parsed != null && parsed in -90.0..90.0) {
            _photoData.update { it.copy(latitude = parsed) }
        }
    }

    fun updateLongitude(value: String) {
        longitudeInput.value = value
        val parsed = value.toDoubleOrNull()
        if (parsed != null && parsed in -180.0..180.0) {
            _photoData.update { it.copy(longitude = parsed) }
        }
    }

    fun updateCoordinates(lat: Double, lng: Double) {
        latitudeInput.value = String.format(Locale.US, "%.6f", lat)
        longitudeInput.value = String.format(Locale.US, "%.6f", lng)
        _photoData.update { it.copy(latitude = lat, longitude = lng) }
    }

    fun updateDayOfWeek(value: String) {
        dayOfWeekInput.value = value
        _photoData.update { it.copy(dayOfWeek = value) }
    }

    fun updateDate(value: String) {
        dateInput.value = value
        val computedDay = AppDateTimeFormatter.calculateDayOfWeek(value)
        if (computedDay != null) {
            dayOfWeekInput.value = computedDay
            _photoData.update { it.copy(dateString = value, dayOfWeek = computedDay) }
        } else {
            _photoData.update { it.copy(dateString = value) }
        }
    }

    fun updateTime(value: String) {
        timeInput.value = value
        _photoData.update { it.copy(timeString = value) }
    }

    fun updateTimezone(value: String) {
        timezoneInput.value = value
        _photoData.update { it.copy(timezone = value) }
    }

    fun updateNote(value: String) {
        customNoteInput.value = value
        _photoData.update { it.copy(customNote = value) }
    }

    fun updateStampStyle(style: StampStyle) {
        _photoData.update { it.copy(stampStyle = style) }
    }

    fun updateMapType(type: AppMapType) {
        _photoData.update { it.copy(mapType = type) }
    }

    fun updateCustomMapImageUri(uri: String?) {
        _photoData.update { it.copy(customMapImageUri = uri) }
    }

    fun updateMapZoom(zoom: Int) {
        _photoData.update { it.copy(mapZoom = zoom.coerceIn(12, 19)) }
    }

    /**
     * Optional utility to fetch current GPS coordinates from device hardware
     * and reverse geocode into address, while keeping all fields fully editable.
     */
    @SuppressLint("MissingPermission")
    fun fetchCurrentDeviceLocation() {
        val context = getApplication<Application>().applicationContext
        if (!PermissionUtils.hasLocationPermission(context)) return

        viewModelScope.launch {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: android.location.Location? ->
                    if (location != null) {
                        updateCoordinates(location.latitude, location.longitude)
                        reverseGeocode(context, location.latitude, location.longitude)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun reverseGeocode(context: Context, lat: Double, lng: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(lat, lng, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            val addr = addresses[0]
                            val heading = listOfNotNull(addr.locality, addr.adminArea, addr.countryName)
                                .joinToString(", ")
                            val fullAddress = addr.getAddressLine(0) ?: heading

                            viewModelScope.launch(Dispatchers.Main) {
                                if (heading.isNotBlank()) updateHeading(heading)
                                if (fullAddress.isNotBlank()) updatePostalAddress(fullAddress)
                            }
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val heading = listOfNotNull(addr.locality, addr.adminArea, addr.countryName)
                            .joinToString(", ")
                        val fullAddress = addr.getAddressLine(0) ?: heading

                        withContext(Dispatchers.Main) {
                            if (heading.isNotBlank()) updateHeading(heading)
                            if (fullAddress.isNotBlank()) updatePostalAddress(fullAddress)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore geocode error
            }
        }
    }

    /**
     * Executes the overlay rendering engine on background thread
     * and stores the resulting bitmap in BitmapHolder for PreviewScreen.
     */
    fun generatePreview(onSuccess: () -> Unit) {
        _isGenerating.value = true
        _generationError.value = null

        viewModelScope.launch {
            val result = generateOverlayUseCase(
                photoData = _photoData.value,
                cachedBaseBitmap = BitmapHolder.originalBitmap
            )

            _isGenerating.value = false

            result.fold(
                onSuccess = { stampedBitmap ->
                    BitmapHolder.activeBitmap = stampedBitmap
                    onSuccess()
                },
                onFailure = { error ->
                    _generationError.value = error.localizedMessage ?: "Failed to render GPS overlay"
                }
            )
        }
    }
}
