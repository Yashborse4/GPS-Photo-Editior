package com.gpsphotocamera.app.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.gpsphotocamera.app.data.model.GpsPhotoData
import com.gpsphotocamera.app.engine.ExifUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Handles saving generated bitmaps to Android MediaStore gallery
 * supporting Scoped Storage on Android 10+ and legacy storage on Android 8-9.
 *
 * Automatically injects accurate GPS coordinates, user-edited Date/Day/Time,
 * Timezone offset, location descriptions, and camera EXIF headers into the saved file.
 */
object MediaStoreHelper {

    private const val DIRECTORY_NAME = "GPSMapCamera"

    suspend fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        photoData: GpsPhotoData? = null,
        titlePrefix: String = "GPS_PHOTO",
        compressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    ): Result<Pair<Uri, String?>> = withContext(Dispatchers.IO) {
        val isPng = compressFormat == Bitmap.CompressFormat.PNG
        val extension = if (isPng) "png" else "jpg"
        val mimeType = if (isPng) "image/png" else "image/jpeg"
        val quality = if (isPng) 100 else 95

        val epochMillis = if (photoData != null) {
            AppDateTimeFormatter.parseToEpochMillis(
                photoData.dateString,
                photoData.timeString,
                photoData.timezone
            )
        } else {
            System.currentTimeMillis()
        }

        val filename = "${titlePrefix}_${System.currentTimeMillis()}.$extension"
        val resolver = context.contentResolver

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + File.separator + DIRECTORY_NAME
                    )
                    put(MediaStore.Images.Media.DATE_TAKEN, epochMillis)
                    put(MediaStore.Images.Media.DATE_ADDED, epochMillis / 1000)
                    put(MediaStore.Images.Media.DATE_MODIFIED, epochMillis / 1000)
                    if (photoData != null) {
                        put(
                            MediaStore.Images.Media.DESCRIPTION,
                            "${photoData.locationHeading} - ${photoData.customNote}".trim()
                        )
                    }
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val imageUri = resolver.insert(
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    contentValues
                ) ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

                val stream: OutputStream? = resolver.openOutputStream(imageUri)
                if (stream == null) {
                    return@withContext Result.failure(Exception("Failed to open output stream"))
                }

                stream.use { out ->
                    bitmap.compress(compressFormat, quality, out)
                }

                // Inject EXIF metadata while still pending so MediaStore indexes full tags upon completion
                if (photoData != null && !isPng) {
                    val sourceUri = if (photoData.originalImageUri.isNotBlank()) {
                        Uri.parse(photoData.originalImageUri)
                    } else null
                    ExifUtils.injectExifIntoUri(context, imageUri, photoData, sourceUri)
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)

                val displayPath = "Pictures/$DIRECTORY_NAME/$filename"
                Result.success(Pair(imageUri, displayPath))
            } else {
                // Android 8.0 - 9.0 (API 26-28)
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, DIRECTORY_NAME)
                if (!appDir.exists()) {
                    appDir.mkdirs()
                }

                val destFile = File(appDir, filename)
                FileOutputStream(destFile).use { out ->
                    bitmap.compress(compressFormat, quality, out)
                }

                if (photoData != null && !isPng) {
                    val sourceUri = if (photoData.originalImageUri.isNotBlank()) {
                        Uri.parse(photoData.originalImageUri)
                    } else null
                    ExifUtils.injectExifIntoFile(destFile, photoData, sourceUri, context)
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, destFile.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.DATE_TAKEN, epochMillis)
                    put(MediaStore.Images.Media.DATE_ADDED, epochMillis / 1000)
                    put(MediaStore.Images.Media.DATE_MODIFIED, epochMillis / 1000)
                    if (photoData != null) {
                        put(MediaStore.Images.Media.LATITUDE, photoData.latitude)
                        put(MediaStore.Images.Media.LONGITUDE, photoData.longitude)
                        put(
                            MediaStore.Images.Media.DESCRIPTION,
                            "${photoData.locationHeading} - ${photoData.customNote}".trim()
                        )
                    }
                }

                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: Uri.fromFile(destFile)

                Result.success(Pair(imageUri, destFile.absolutePath))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
