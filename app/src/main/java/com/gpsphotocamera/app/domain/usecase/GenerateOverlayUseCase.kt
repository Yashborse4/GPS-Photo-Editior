package com.gpsphotocamera.app.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.gpsphotocamera.app.data.model.GpsPhotoData
import com.gpsphotocamera.app.image.BitmapUtils
import com.gpsphotocamera.app.image.CanvasOverlayRenderer
import com.gpsphotocamera.app.maps.MapSnapshotHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * UseCase coordinating:
 * 1. Base photo retrieval / downsampling
 * 2. Satellite map thumbnail generation
 * 3. 2D Canvas overlay rendering
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

            // 2. Generate Map snapshot / vector tile
            val mapThumbnail = MapSnapshotHelper.generateMapThumbnail(
                latitude = photoData.latitude,
                longitude = photoData.longitude,
                mapType = photoData.mapType,
                size = 480
            )

            // 3. Render complete overlay on canvas
            val finalBitmap = CanvasOverlayRenderer.renderOverlay(
                baseBitmap = baseBitmap,
                data = photoData,
                mapThumbnail = mapThumbnail
            )

            Result.success(finalBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
