package com.gpsphotocamera.app.ui.screens.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gpsphotocamera.app.R
import com.gpsphotocamera.app.ui.components.AppDatePickerDialog
import com.gpsphotocamera.app.ui.components.AppTimePickerDialog
import com.gpsphotocamera.app.ui.components.AppTimezoneDialog
import com.gpsphotocamera.app.ui.components.AppTopBar
import com.gpsphotocamera.app.ui.components.LoadingOverlay
import com.gpsphotocamera.app.ui.components.RoundedTextField
import com.gpsphotocamera.app.ui.components.SaveLocationDialog
import com.gpsphotocamera.app.ui.components.SavedLocationsListDialog
import com.gpsphotocamera.app.ui.components.SatelliteMapCard
import com.gpsphotocamera.app.ui.components.StampStyleSelector
import com.gpsphotocamera.app.utils.PermissionUtils
import com.gpsphotocamera.app.viewmodel.EditorViewModel
import kotlinx.coroutines.launch

@Composable
fun PhotoEditorScreen(
    viewModel: EditorViewModel,
    initialPhotoUri: String,
    onNavigateBack: () -> Unit,
    onNavigateToPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(initialPhotoUri) {
        if (initialPhotoUri.isNotBlank()) {
            viewModel.setInitialPhotoUri(initialPhotoUri)
        }
    }

    val photoData by viewModel.photoData.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generationError by viewModel.generationError.collectAsState()

    val heading by viewModel.locationHeadingInput.collectAsState()
    val address by viewModel.postalAddressInput.collectAsState()
    val latitude by viewModel.latitudeInput.collectAsState()
    val longitude by viewModel.longitudeInput.collectAsState()
    val date by viewModel.dateInput.collectAsState()
    val time by viewModel.timeInput.collectAsState()
    val timezone by viewModel.timezoneInput.collectAsState()
    val note by viewModel.customNoteInput.collectAsState()

    val savedLocations by viewModel.savedLocations.collectAsState()

    // Dialog display states
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showTimezonePicker by remember { mutableStateOf(false) }
    var showSaveLocationDialog by remember { mutableStateOf(false) }
    var showSavedLocationsListDialog by remember { mutableStateOf(false) }

    // Photo change picker (preserves existing GPS & address settings for easy multi-photo editing)
    val changePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.changePhotoUri(uri.toString(), preserveLocationSettings = true)
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Photograph changed. Location and stamp settings preserved!")
            }
        }
    }

    // Location permissions launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            viewModel.fetchCurrentDeviceLocation()
        }
    }

    LaunchedEffect(generationError) {
        generationError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.photo_editor),
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Persistent Bottom Action Bar: Generate Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.generatePreview(onSuccess = onNavigateToPreview)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(imageVector = Icons.Default.EditLocation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.generate_preview),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Uploaded Photograph Thumbnail Card with Change Photo action
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable {
                                    changePhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(photoData.originalImageUri)
                                    .crossfade(true)
                                    .error(R.drawable.ic_placeholder_photo)
                                    .build(),
                                contentDescription = "Original photograph",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Quick tap hint (Top Start)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Tap Photo to Change",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Change Photo Floating Pill (Bottom End)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                                    .clickable {
                                        changePhotoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(id = R.string.change_photo),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. GPS Coordinates & Heading Section with Saved Location bookmarks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GPS & Location Information",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { showSavedLocationsListDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmarks,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Saved (${savedLocations.size})",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        IconButton(
                            onClick = { showSaveLocationDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = "Save this location",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Quick chips for top saved locations
                if (savedLocations.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick apply:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        savedLocations.take(4).forEach { loc ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    viewModel.applySavedLocation(loc)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Applied location: ${loc.title}")
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                label = {
                                    Text(
                                        text = loc.title,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                }

                // Location Heading
                RoundedTextField(
                    value = heading,
                    onValueChange = viewModel::updateHeading,
                    label = stringResource(id = R.string.location_heading),
                    placeholder = stringResource(id = R.string.location_heading_hint),
                    leadingIcon = Icons.Default.LocationOn
                )

                // Postal Address
                RoundedTextField(
                    value = address,
                    onValueChange = viewModel::updatePostalAddress,
                    label = stringResource(id = R.string.postal_address),
                    placeholder = stringResource(id = R.string.postal_address_hint),
                    leadingIcon = Icons.Default.Public,
                    singleLine = false,
                    maxLines = 3
                )

                // Latitude and Longitude (Editable independently)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val latValid = latitude.toDoubleOrNull()?.let { it in -90.0..90.0 } ?: false
                    RoundedTextField(
                        value = latitude,
                        onValueChange = viewModel::updateLatitude,
                        label = stringResource(id = R.string.latitude),
                        placeholder = stringResource(id = R.string.latitude_hint),
                        isError = !latValid && latitude.isNotEmpty(),
                        errorMessage = "Invalid (-90 to 90)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )

                    val lngValid = longitude.toDoubleOrNull()?.let { it in -180.0..180.0 } ?: false
                    RoundedTextField(
                        value = longitude,
                        onValueChange = viewModel::updateLongitude,
                        label = stringResource(id = R.string.longitude),
                        placeholder = stringResource(id = R.string.longitude_hint),
                        isError = !lngValid && longitude.isNotEmpty(),
                        errorMessage = "Invalid (-180 to 180)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Use Current Device GPS button
                OutlinedButton(
                    onClick = {
                        if (PermissionUtils.hasLocationPermission(context)) {
                            viewModel.fetchCurrentDeviceLocation()
                        } else {
                            locationPermissionLauncher.launch(PermissionUtils.locationPermissions)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.MyLocation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.use_current_location))
                }

                // 3. Map Preview (OpenStreetMap / Satellite / Standard / Terrain)
                SatelliteMapCard(
                    latitude = photoData.latitude,
                    longitude = photoData.longitude,
                    selectedMapType = photoData.mapType,
                    onMapTypeChanged = viewModel::updateMapType
                )

                // 4. Date, Time & Timezone Section
                Text(
                    text = "Timestamp & Timezone",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date
                    RoundedTextField(
                        value = date,
                        onValueChange = viewModel::updateDate,
                        label = stringResource(id = R.string.date),
                        leadingIcon = Icons.Default.CalendarMonth,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Pick date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { showDatePicker = true }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Time
                    RoundedTextField(
                        value = time,
                        onValueChange = viewModel::updateTime,
                        label = stringResource(id = R.string.time),
                        leadingIcon = Icons.Default.Schedule,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Pick time",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { showTimePicker = true }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Timezone
                RoundedTextField(
                    value = timezone,
                    onValueChange = viewModel::updateTimezone,
                    label = stringResource(id = R.string.timezone),
                    leadingIcon = Icons.Default.Public,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Select timezone",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showTimezonePicker = true }
                        )
                    }
                )

                // 5. Custom Note
                Text(
                    text = "Additional Details",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                RoundedTextField(
                    value = note,
                    onValueChange = viewModel::updateNote,
                    label = stringResource(id = R.string.custom_note),
                    placeholder = stringResource(id = R.string.custom_note_hint),
                    leadingIcon = Icons.AutoMirrored.Filled.Notes
                )

                // 6. Stamp Style Selection
                StampStyleSelector(
                    selectedStyle = photoData.stampStyle,
                    onStyleSelected = viewModel::updateStampStyle
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Dialogs
            if (showDatePicker) {
                AppDatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    onDateSelected = viewModel::updateDate
                )
            }

            if (showTimePicker) {
                AppTimePickerDialog(
                    onDismissRequest = { showTimePicker = false },
                    onTimeSelected = viewModel::updateTime
                )
            }

            if (showTimezonePicker) {
                AppTimezoneDialog(
                    onDismissRequest = { showTimezonePicker = false },
                    onTimezoneSelected = viewModel::updateTimezone
                )
            }

            if (showSaveLocationDialog) {
                SaveLocationDialog(
                    initialTitle = heading.ifBlank { "Location ${date}" },
                    heading = heading,
                    address = address,
                    coordinates = "${latitude}° N, ${longitude}° E",
                    onDismissRequest = { showSaveLocationDialog = false },
                    onConfirm = { title ->
                        viewModel.saveCurrentLocation(title)
                        showSaveLocationDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Location saved as '$title'!")
                        }
                    }
                )
            }

            if (showSavedLocationsListDialog) {
                SavedLocationsListDialog(
                    savedLocations = savedLocations,
                    onSelectLocation = { loc ->
                        viewModel.applySavedLocation(loc)
                        showSavedLocationsListDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Applied saved location: ${loc.title}")
                        }
                    },
                    onDeleteLocation = { id ->
                        viewModel.deleteSavedLocation(id)
                    },
                    onOpenSaveCurrentDialog = {
                        showSavedLocationsListDialog = false
                        showSaveLocationDialog = true
                    },
                    onDismissRequest = { showSavedLocationsListDialog = false }
                )
            }

            // Loading state when rendering overlay
            if (isGenerating) {
                LoadingOverlay(message = "Rendering high-resolution GPS overlay…")
            }
        }
    }
}
