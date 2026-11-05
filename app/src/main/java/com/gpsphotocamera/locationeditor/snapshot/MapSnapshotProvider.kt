package com.gpsphotocamera.locationeditor.snapshot

import android.graphics.Bitmap
import com.gpsphotocamera.locationeditor.model.LocationData
import com.gpsphotocamera.locationeditor.model.ThumbnailConfig
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean decoupled interface exposing captured map thumbnails to external modules
 * (such as image rendering, photo watermarking, EXIF writers, or file exporters).
 *
 * This ensures strict architectural separation: the photo renderer does not depend
 * on the Google Maps SDK or Jetpack Compose UI state directly.
 */
interface MapSnapshotProvider {

    /**
     * Flow of the latest captured satellite map snapshot Bitmap.
     */
    val currentSnapshot: StateFlow<Bitmap?>

    /**
     * Complete location metadata associated with the snapshot.
     */
    val currentLocationData: StateFlow<LocationData>

    /**
     * Request a map capture with the specified configuration.
     * @param config Target resolution, aspect ratio, and shape formatting.
     * @param onComplete Callback invoked with resulting processed Bitmap (or null on failure).
     */
    fun captureMapSnapshot(
        config: ThumbnailConfig,
        onComplete: (Bitmap?) -> Unit
    )
}
