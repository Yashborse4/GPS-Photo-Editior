package com.gpsphotocamera.locationeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gpsphotocamera.locationeditor.model.LocationData
import com.gpsphotocamera.locationeditor.ui.theme.DarkBorder
import com.gpsphotocamera.locationeditor.ui.theme.DarkSurfaceCard
import com.gpsphotocamera.locationeditor.ui.theme.GpsRed
import com.gpsphotocamera.locationeditor.ui.theme.TextPrimary
import com.gpsphotocamera.locationeditor.ui.theme.TextSecondary

/**
 * Manual location information form.
 * Contains:
 * - Location heading
 * - Country
 * - State
 * - City
 * - Complete postal address
 * - Postal PIN code
 *
 * All fields are independently and manually editable.
 */
@Composable
fun LocationFormCard(
    locationData: LocationData,
    onHeadingChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onStateChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onPostalAddressChange: (String) -> Unit,
    onPinCodeChange: (String) -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.EditLocationAlt,
                contentDescription = null,
                tint = GpsRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Location Information",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        }

        Text(
            text = "Enter metadata to stamp onto photographs (independent of GPS)",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        // 1. Location Heading
        OutlinedTextField(
            value = locationData.heading,
            onValueChange = onHeadingChange,
            label = { Text("Location Heading") },
            placeholder = { Text("e.g. Pune, Maharashtra, India") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GpsRed,
                unfocusedBorderColor = DarkBorder
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. City & State (Side-by-side)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = locationData.city,
                onValueChange = onCityChange,
                label = { Text("City") },
                placeholder = { Text("e.g. Pune") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GpsRed,
                    unfocusedBorderColor = DarkBorder
                )
            )

            OutlinedTextField(
                value = locationData.state,
                onValueChange = onStateChange,
                label = { Text("State") },
                placeholder = { Text("e.g. Maharashtra") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GpsRed,
                    unfocusedBorderColor = DarkBorder
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Country & Postal PIN Code (Side-by-side)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = locationData.country,
                onValueChange = onCountryChange,
                label = { Text("Country") },
                placeholder = { Text("e.g. India") },
                singleLine = true,
                modifier = Modifier.weight(1.2f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GpsRed,
                    unfocusedBorderColor = DarkBorder
                )
            )

            OutlinedTextField(
                value = locationData.pinCode,
                onValueChange = onPinCodeChange,
                label = { Text("PIN / Postal Code") },
                placeholder = { Text("e.g. 411004") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GpsRed,
                    unfocusedBorderColor = DarkBorder
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Complete Postal Address
        OutlinedTextField(
            value = locationData.postalAddress,
            onValueChange = onPostalAddressChange,
            label = { Text("Complete Postal Address") },
            placeholder = { Text("e.g. 40/34, Near Prime Furnishings, Bhonde Colony, Erandwane...") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GpsRed,
                unfocusedBorderColor = DarkBorder
            )
        )
    }
}
