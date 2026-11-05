package com.gpsphotocamera.app.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.max

/**
 * High-performance, OOM-safe bitmap decoding, scaling, and transformation utilities.
 */
object BitmapUtils {

    private const val MAX_RENDER_DIMENSION = 2560 // 2.5K high resolution limit for editing & rendering

    /**
     * Decodes and auto-rotates a bitmap from a content Uri with sub-sampling.
     */
    suspend fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = MAX_RENDER_DIMENSION
    ): Bitmap? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        try {
            // First pass: Read dimensions only
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            var stream: InputStream? = resolver.openInputStream(uri)
            BitmapFactory.decodeStream(stream, null, options)
            stream?.close()

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return@withContext null
            }

            // Calculate sample size
            options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            // Second pass: Decode actual bitmap
            stream = resolver.openInputStream(uri)
            val decodedBitmap = BitmapFactory.decodeStream(stream, null, options)
            stream?.close()

            if (decodedBitmap == null) return@withContext null

            // Read EXIF orientation and rotate accordingly
            val orientation = getExifOrientation(context, uri)
            rotateBitmap(decodedBitmap, orientation)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }

    private fun getExifOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (e: OutOfMemoryError) {
            bitmap
        }
    }
}
