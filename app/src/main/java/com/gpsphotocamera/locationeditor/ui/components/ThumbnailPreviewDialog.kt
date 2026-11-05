package com.gpsphotocamera.locationeditor.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gpsphotocamera.locationeditor.model.ThumbnailConfig
import com.gpsphotocamera.locationeditor.ui.theme.DarkBorder
import com.gpsphotocamera.locationeditor.ui.theme.DarkSurfaceElevated
import com.gpsphotocamera.locationeditor.ui.theme.GpsRed
import com.gpsphotocamera.locationeditor.ui.theme.TextPrimary
import com.gpsphotocamera.locationeditor.ui.theme.TextSecondary

/**
 * Dialog displaying captured satellite map thumbnail with size/aspect ratio selector.
 */
@Composable
fun ThumbnailPreviewDialog(
    bitmap: Bitmap?,
    selectedConfig: ThumbnailConfig,
    onSelectConfig: (ThumbnailConfig) -> Unit,
    onOpenPhotoStampPreview: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Satellite Map Thumbnail",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )

                Text(
                    text = "Ready for embedding into GPS Photo Camera watermark",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                )

                // Render captured bitmap preview
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Satellite Thumbnail",
                            modifier = Modifier.size(220.dp)
                        )
                    } else {
                        Text("No snapshot captured", color = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Resolution and aspect ratio badge
                if (bitmap != null) {
                    Text(
                        text = "${bitmap.width} × ${bitmap.height} px | ${selectedConfig.aspectRatio.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GpsRed
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Aspect Ratio / Thumbnail Size Selector
                Text(
                    text = "Select Thumbnail Style:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ThumbnailConfig.ALL_PRESETS.forEach { config ->
                        val isSelected = config.id == selectedConfig.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectConfig(config) },
                            label = { Text(config.name, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GpsRed,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close")
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onOpenPhotoStampPreview()
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GpsRed,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stamp Preview")
                    }
                }
            }
        }
    }
}
