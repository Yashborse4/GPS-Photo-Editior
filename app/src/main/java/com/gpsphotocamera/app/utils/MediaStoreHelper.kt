package com.gpsphotocamera.app.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Handles saving generated bitmaps to Android MediaStore gallery
 * supporting Scoped Storage on Android 10+ and legacy storage on Android 8-9.
 */
object MediaStoreHelper {

    private const val DIRECTORY_NAME = "GPSMapCamera"

    suspend fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        titlePrefix: String = "GPS_PHOTO"
    ): Result<Pair<Uri, String?>> = withContext(Dispatchers.IO) {
        val filename = "${titlePrefix}_${System.currentTimeMillis()}.jpg"
        val resolver = context.contentResolver

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + File.separator + DIRECTORY_NAME
                    )
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
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)

                Result.success(Pair(imageUri, null))
            } else {
                // Android 8.0 - 9.0 (API 26-28)
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, DIRECTORY_NAME)
                if (!appDir.exists()) {
                    appDir.mkdirs()
                }

                val destFile = File(appDir, filename)
                FileOutputStream(destFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, destFile.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
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
