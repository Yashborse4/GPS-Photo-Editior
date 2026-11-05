package com.gpsphotocamera.app.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.gpsphotocamera.app.data.model.GpsPhotoData
import com.gpsphotocamera.app.data.model.SavedPhotoItem
import com.gpsphotocamera.app.data.repository.RecentPhotosRepository
import com.gpsphotocamera.app.utils.AppDateTimeFormatter
import com.gpsphotocamera.app.utils.MediaStoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * UseCase to save the generated stamped bitmap to Android MediaStore gallery
 * and track it inside the recent photographs repository.
 */
class SavePhotoUseCase(
    private val context: Context,
    private val recentPhotosRepository: RecentPhotosRepository
) {

    suspend operator fun invoke(
        bitmap: Bitmap,
        photoData: GpsPhotoData
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val saveResult = MediaStoreHelper.saveBitmapToGallery(
            context = context,
            bitmap = bitmap,
            titlePrefix = "GPS_MAP"
        )

        saveResult.fold(
            onSuccess = { (savedUri, filePath) ->
                // Record in recent history
                val item = SavedPhotoItem(
                    id = UUID.randomUUID().toString(),
                    contentUri = savedUri.toString(),
                    filePath = filePath,
                    locationHeading = photoData.locationHeading,
                    coordinates = photoData.formattedCoordinates(),
                    timestamp = System.currentTimeMillis(),
                    formattedDate = AppDateTimeFormatter.getCurrentDateString() + " " + AppDateTimeFormatter.getCurrentTimeString(false)
                )
                recentPhotosRepository.addPhoto(item)
                Result.success(savedUri)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }
}
