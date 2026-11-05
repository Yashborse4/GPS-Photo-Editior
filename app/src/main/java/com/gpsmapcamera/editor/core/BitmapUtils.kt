package com.gpsmapcamera.editor.core

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Robust bitmap processing utilities for high-resolution photograph loading,
 * memory optimization, OOM protection, and MediaStore saving.
 */
object BitmapUtils {

    /**
     * Decodes a photograph from [Uri] at its full native resolution with EXIF orientation correction.
     * Implements safe fallback to prevent OutOfMemoryError on extreme camera resolutions (50MP+).
     */
    suspend fun decodeFullResolutionBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        try {
            // 1. Measure dimensions without allocating pixel memory
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (options.outWidth <= 0 || options.outHeight <= 0) return@withContext null

            // 2. Read EXIF orientation
            val orientation = ExifUtils.readOrientation(context, uri)

            // 3. Check memory availability: modern smartphones have heap limits
            val maxDimension = 6144 // Max 6K safely decodable on standard Android heap
            var sampleSize = 1
            var w = options.outWidth
            var h = options.outHeight
            while (w > maxDimension || h > maxDimension) {
                sampleSize *= 2
                w /= 2
                h /= 2
            }

            // 4. Decode full bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = true
            }

            inputStream = context.contentResolver.openInputStream(uri)
            val decoded = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (decoded != null) {
                ExifUtils.applyOrientation(decoded, orientation)
            } else {
                null
            }
        } catch (e: OutOfMemoryError) {
            System.gc()
            // Retry with 2x downsampling on extreme memory pressure
            decodeDownscaledBitmap(context, uri, 3072)
        } catch (e: Exception) {
            null
        } finally {
            inputStream?.close()
        }
    }

    /**
     * Decodes a downscaled bitmap optimized for smooth, responsive live UI preview.
     */
    suspend fun decodePreviewBitmap(
        context: Context,
        uri: Uri,
        targetWidth: Int = 1200
    ): Bitmap? = withContext(Dispatchers.IO) {
        decodeDownscaledBitmap(context, uri, targetWidth)
    }

    private fun decodeDownscaledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        var inputStream: InputStream? = null
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            val orientation = ExifUtils.readOrientation(context, uri)

            var sampleSize = 1
            var w = options.outWidth
            var h = options.outHeight
            while (w > maxDimension || h > maxDimension) {
                sampleSize *= 2
                w /= 2
                h /= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            inputStream = context.contentResolver.openInputStream(uri)
            val decoded = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            return if (decoded != null) {
                ExifUtils.applyOrientation(decoded, orientation)
            } else {
                null
            }
        } catch (e: Exception) {
            return null
        } finally {
            inputStream?.close()
        }
    }

    /**
     * Safely creates a mutable bitmap for rendering, handling OutOfMemoryError gracefully.
     */
    fun createMutableBitmap(width: Int, height: Int): Bitmap? {
        return try {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            System.gc()
            try {
                // Fallback to RGB_565 if ARGB_8888 fails on extremely tight memory
                Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            } catch (e2: OutOfMemoryError) {
                null
            }
        }
    }

    /**
     * Saves a rendered [bitmap] to the Android MediaStore gallery (scoped storage compliant).
     * Returns the content [Uri] of the saved photo or null on failure.
     */
    suspend fun saveToMediaStore(
        context: Context,
        bitmap: Bitmap,
        config: OverlayConfig,
        sourceUri: Uri? = null,
        baseFilename: String = "GPS_MAP_PHOTO_${System.currentTimeMillis()}"
    ): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val isPng = config.output.format == ExportFormat.PNG
        val extension = if (isPng) ".png" else ".jpg"
        val mimeType = if (isPng) "image/png" else "image/jpeg"
        val filename = "$baseFilename$extension"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/GPSMapCamera")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val itemUri = resolver.insert(collection, contentValues) ?: return@withContext null

        try {
            val outputStream: OutputStream = resolver.openOutputStream(itemUri)
                ?: return@withContext null

            outputStream.use { out ->
                if (isPng) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                } else {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, config.output.jpegQuality, out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
            }

            // Inject EXIF metadata if JPEG
            if (!isPng && config.output.preserveExif) {
                try {
                    resolver.openFileDescriptor(itemUri, "rw")?.use { pfd ->
                        val exif = androidx.exifinterface.media.ExifInterface(pfd.fileDescriptor)
                        exif.setAttribute(
                            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL.toString()
                        )
                        exif.saveAttributes()
                    }
                } catch (e: Exception) {
                    // Non-fatal
                }
            }

            itemUri
        } catch (e: Exception) {
            resolver.delete(itemUri, null, null)
            null
        }
    }

    /**
     * Saves a rendered [bitmap] directly to a target [file].
     */
    suspend fun saveToFile(
        file: File,
        bitmap: Bitmap,
        config: OverlayConfig,
        sourceFile: File? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { out ->
                if (config.output.format == ExportFormat.PNG) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                } else {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, config.output.jpegQuality, out)
                }
            }

            if (config.output.format == ExportFormat.JPEG && config.output.preserveExif) {
                ExifUtils.copyAndInjectExif(
                    sourceFile = sourceFile,
                    targetFile = file,
                    location = config.location,
                    dateTimeConfig = config.dateTime
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
