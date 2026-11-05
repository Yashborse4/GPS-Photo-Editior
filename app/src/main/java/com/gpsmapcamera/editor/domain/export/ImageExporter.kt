package com.gpsmapcamera.editor.domain.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.gpsmapcamera.editor.data.model.ExportFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportResult(
    val uri: Uri,
    val filePath: String,
    val fileName: String,
    val width: Int,
    val height: Int,
    val fileSizeFormatted: String,
    val format: ExportFormat
)

class ImageExporter(private val context: Context) {

    /**
     * Saves the final rendered bitmap to device storage via MediaStore.
     */
    suspend fun saveImageToGallery(
        bitmap: Bitmap,
        format: ExportFormat = ExportFormat.JPEG,
        quality: Int = 95
    ): Result<ExportResult> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "GPS_CAMERA_${timestamp}.${format.extension}"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                put(MediaStore.Images.Media.WIDTH, bitmap.width)
                put(MediaStore.Images.Media.HEIGHT, bitmap.height)
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/GPS Map Camera")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

            var outputStream: OutputStream? = null
            try {
                outputStream = resolver.openOutputStream(imageUri)
                    ?: return@withContext Result.failure(Exception("Failed to open output stream"))

                val compressFormat = if (format == ExportFormat.PNG) {
                    Bitmap.CompressFormat.PNG
                } else {
                    Bitmap.CompressFormat.JPEG
                }

                val success = bitmap.compress(compressFormat, quality, outputStream)
                if (!success) {
                    return@withContext Result.failure(Exception("Failed to compress bitmap"))
                }
            } finally {
                outputStream?.flush()
                outputStream?.close()
            }

            // Mark as not pending for Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }

            // Estimate file size
            val estimatedBytes = if (format == ExportFormat.PNG) {
                (bitmap.width * bitmap.height * 2.5).toLong()
            } else {
                (bitmap.width * bitmap.height * 0.45).toLong()
            }
            val sizeFormatted = formatFileSize(estimatedBytes)

            val result = ExportResult(
                uri = imageUri,
                filePath = "Pictures/GPS Map Camera/$filename",
                fileName = filename,
                width = bitmap.width,
                height = bitmap.height,
                fileSizeFormatted = sizeFormatted,
                format = format
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a shareable temporary cached image file and returns an ACTION_SEND Intent.
     */
    suspend fun createShareIntent(
        bitmap: Bitmap,
        format: ExportFormat = ExportFormat.JPEG,
        title: String = "GPS Map Camera Photo"
    ): Result<Intent> = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "images").apply { mkdirs() }
            val tempFile = File(cacheDir, "shared_gps_photo_${System.currentTimeMillis()}.${format.extension}")
            val fos = FileOutputStream(tempFile)
            bitmap.compress(format.compressFormat, 95, fos)
            fos.flush()
            fos.close()

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, tempFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Photo tagged with GPS Map Camera: $title")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            Result.success(Intent.createChooser(shareIntent, "Share GPS Photo"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
            else -> "$bytes B"
        }
    }
}
