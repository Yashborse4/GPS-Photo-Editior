package com.gpsphotocamera.locationeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gpsphotocamera.locationeditor.model.CoordinateValidationResult
import com.gpsphotocamera.locationeditor.model.LocationData
import com.gpsphotocamera.locationeditor.ui.theme.DarkBorder
import com.gpsphotocamera.locationeditor.ui.theme.DarkSurfaceCard
import com.gpsphotocamera.locationeditor.ui.theme.GpsRed
import com.gpsphotocamera.locationeditor.ui.theme.StatusGreen
import com.gpsphotocamera.locationeditor.ui.theme.StatusRed
import com.gpsphotocamera.locationeditor.ui.theme.TextPrimary
import com.gpsphotocamera.locationeditor.ui.theme.TextSecondary

/**
 * Coordinate input card supporting decimal degree entry, instant validation feedback,
 * DMS translation, and sample location presets.
 */
@Composable
fun CoordinateInputCard(
    locationData: LocationData,
    validationResult: CoordinateValidationResult,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onApplyPreset: (LocationData) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        // Header with validation status indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Explore,
                    contentDescription = null,
                    tint = GpsRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GPS Coordinates",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }

            // Valid / Invalid status chip
            if (validationResult.isValid) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Valid",
                        tint = StatusGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Valid",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Invalid",
                        tint = StatusRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Fix Errors",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Latitude & Longitude inputs side-by-side or stacked
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Latitude Field
            OutlinedTextField(
                value = locationData.latitudeString,
                onValueChange = onLatitudeChange,
                label = { Text("Latitude") },
                placeholder = { Text("e.g. 18.511347") },
                singleLine = true,
                isError = !validationResult.isLatitudeValid,
                supportingText = {
                    validationResult.latitudeError?.let {
                        Text(it, color = StatusRed, style = MaterialTheme.typography.labelSmall)
                    } ?: Text("-90.0° to +90.0°", style = MaterialTheme.typography.labelSmall)
                },
                trailingIcon = {
                    if (locationData.latitudeString.isNotEmpty()) {
                        IconButton(onClick = { onLatitudeChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GpsRed,
                    unfocusedBorderColor = DarkBorder,
                    errorBorderColor = StatusRed
                )
            )

            // Longitude Field
            OutlinedTextField(
                value = locationData.longitudeString,
                onValueChange = onLongitudeChange,
                label = { Text("Longitude") },
                placeholder = { Text("e.g. 73.833891") },
                singleLine = true,
                isError = !validationResult.isLongitudeValid,
                supportingText = {
                    validationResult.longitudeError?.let {
                        Text(it, color = StatusRed, style = MaterialTheme.typography.labelSmall)
                    } ?: Text("-180.0° to +180.0°", style = MaterialTheme.typography.labelSmall)
                },
                trailingIcon = {
                    if (locationData.longitudeString.isNotEmpty()) {
                        IconButton(onClick = { onLongitudeChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GpsRed,
                    unfocusedBorderColor = DarkBorder,
                    errorBorderColor = StatusRed
                )
            )
        }

        // Live formatted readout (DMS and Decimal)
        if (validationResult.isValid) {
            Spacer(modifier = Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "DMS: ${locationData.toDmsCoordinates()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary
                )
                Text(
                    text = "Cardinal: ${locationData.toFormattedCoordinates()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Preset quick selectors for convenient testing
        Text(
            text = "Quick Presets:",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AssistChip(
                onClick = { onApplyPreset(LocationData.DEFAULT_PUNE) },
                label = { Text("Pune (Reference)") },
                colors = AssistChipDefaults.assistChipColors(labelColor = TextPrimary)
            )
            AssistChip(
                onClick = { onApplyPreset(LocationData.PRESET_NEW_YORK) },
                label = { Text("New York (W/N)") },
                colors = AssistChipDefaults.assistChipColors(labelColor = TextPrimary)
            )
            AssistChip(
                onClick = { onApplyPreset(LocationData.PRESET_SYDNEY) },
                label = { Text("Sydney (S/E)") },
                colors = AssistChipDefaults.assistChipColors(labelColor = TextPrimary)
            )
            AssistChip(
                onClick = { onApplyPreset(LocationData.PRESET_TOKYO) },
                label = { Text("Tokyo") },
                colors = AssistChipDefaults.assistChipColors(labelColor = TextPrimary)
            )
        }
    }
}
