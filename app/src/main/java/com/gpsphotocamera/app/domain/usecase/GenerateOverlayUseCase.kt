package com.gpsphotocamera.app.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.gpsphotocamera.app.data.model.GpsPhotoData
import com.gpsphotocamera.app.engine.OverlayConfig
import com.gpsphotocamera.app.engine.OverlayRenderer
import com.gpsphotocamera.app.image.BitmapUtils
import com.gpsphotocamera.app.maps.MapSnapshotHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * UseCase coordinating:
 * 1. Base photo retrieval / downsampling
 * 2. Satellite map thumbnail generation (or user-uploaded satellite image)
 * 3. Canonical OverlayRenderer Canvas overlay rendering
 */
class GenerateOverlayUseCase(private val context: Context) {

    suspend operator fun invoke(
        photoData: GpsPhotoData,
        cachedBaseBitmap: Bitmap? = null
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        try {
            // 1. Get base photo bitmap
            val baseBitmap = cachedBaseBitmap ?: run {
                val uri = Uri.parse(photoData.originalImageUri)
                BitmapUtils.decodeSampledBitmapFromUri(context, uri)
            } ?: return@withContext Result.failure(Exception("Could not load original photograph"))

            // 2. Obtain Satellite map thumbnail (user-uploaded satellite image or ArcGIS World Imagery)
            val satelliteBitmap = if (photoData.showMap) {
                if (!photoData.customMapImageUri.isNullOrBlank()) {
                    val customUri = Uri.parse(photoData.customMapImageUri)
                    BitmapUtils.decodeSampledBitmapFromUri(context, customUri, 480)
                } else {
                    MapSnapshotHelper.fetchCleanSatelliteImagery(
                        latitude = photoData.latitude,
                        longitude = photoData.longitude,
                        mapType = photoData.mapType,
                        zoom = photoData.mapZoom,
                        size = 480
                    )
                }
            } else null

            // 3. Build OverlayConfig from GpsPhotoData
            val config = OverlayConfig.fromGpsPhotoData(photoData, satelliteBitmap)

            // 4. Render overlay with canonical OverlayRenderer
            val finalBitmap = OverlayRenderer.renderOverlay(
                sourceBitmap = baseBitmap,
                config = config
            )

            Result.success(finalBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
