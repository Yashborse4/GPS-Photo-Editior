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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gpsphotocamera.locationeditor.ui.theme.DarkBorder
import com.gpsphotocamera.locationeditor.ui.theme.DarkSurfaceCard
import com.gpsphotocamera.locationeditor.ui.theme.GpsRed
import com.gpsphotocamera.locationeditor.ui.theme.TextPrimary
import com.gpsphotocamera.locationeditor.ui.theme.TextSecondary
import java.util.Locale

/**
 * Control bar for satellite map interaction:
 * - Zoom slider with +/- step buttons and zoom level readout
 * - Map Re-center button
 * - Map Snapshot capture button with loading state
 * - Reset coordinates button
 */
@Composable
fun MapControlsBar(
    zoomLevel: Float,
    isSnapshotInProgress: Boolean,
    onZoomChanged: (Float) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onRecenter: () -> Unit,
    onReset: () -> Unit,
    onCaptureSnapshot: () -> Unit,
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
        // Zoom controls row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Satellite Zoom",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )

            val zoomDescription = when {
                zoomLevel >= 18f -> "Street Detail"
                zoomLevel >= 15f -> "Neighborhood"
                zoomLevel >= 11f -> "City"
                zoomLevel >= 7f -> "Region"
                else -> "Country"
            }

            Text(
                text = String.format(Locale.US, "%.1fx (%s)", zoomLevel, zoomDescription),
                style = MaterialTheme.typography.labelSmall,
                color = GpsRed
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.iconButtonColors(contentColor = TextPrimary)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }

            Slider(
                value = zoomLevel,
                onValueChange = onZoomChanged,
                valueRange = 2.0f..21.0f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = GpsRed,
                    activeTrackColor = GpsRed,
                    inactiveTrackColor = DarkBorder
                )
            )

            IconButton(
                onClick = onZoomIn,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.iconButtonColors(contentColor = TextPrimary)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons Row: Center, Reset, Snapshot
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Re-center on marker
            FilledTonalButton(
                onClick = onRecenter,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Center", style = MaterialTheme.typography.labelMedium)
            }

            // Reset coordinates
            FilledTonalButton(
                onClick = onReset,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset", style = MaterialTheme.typography.labelMedium)
            }

            // Capture Satellite Snapshot
            Button(
                onClick = onCaptureSnapshot,
                enabled = !isSnapshotInProgress,
                modifier = Modifier.weight(1.3f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GpsRed,
                    contentColor = Color.White
                )
            ) {
                if (isSnapshotInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Capturing...", style = MaterialTheme.typography.labelMedium)
                } else {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Snapshot", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
