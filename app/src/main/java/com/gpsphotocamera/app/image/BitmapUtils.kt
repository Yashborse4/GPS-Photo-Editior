package com.gpsphotocamera.app.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.max

/**
 * High-performance, OOM-safe bitmap decoding, scaling, and transformation utilities.
 * Handles EXIF orientation correction and full-resolution native decoding.
 */
object BitmapUtils {

    private const val PREVIEW_MAX_DIMENSION = 2048 // Balanced for snappy live preview

    /**
     * Decodes and auto-rotates a bitmap from a content Uri with sub-sampling for editing & preview.
     */
    suspend fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = PREVIEW_MAX_DIMENSION
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

    /**
     * Decodes the photograph at its native 100% full original resolution (12MP - 108MP)
     * with EXIF orientation correction for final export rendering.
     */
    suspend fun decodeFullResolutionBitmapFromUri(
        context: Context,
        uri: Uri
    ): Bitmap? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        try {
            // Decode full-resolution with ARGB_8888
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val stream = resolver.openInputStream(uri)
            val decodedBitmap = BitmapFactory.decodeStream(stream, null, options)
            stream?.close()

            if (decodedBitmap == null) return@withContext null

            val orientation = getExifOrientation(context, uri)
            rotateBitmap(decodedBitmap, orientation)
        } catch (e: OutOfMemoryError) {
            // If device runs low on memory for ultra-high res, safely decode at 4K resolution
            decodeSampledBitmapFromUri(context, uri, 3840)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Synthesizes an authentic sample photograph (1086 x 1448 matching reference aspect ratio)
     * displaying an architectural landscape scene when no photo is uploaded yet.
     */
    fun createSamplePhotoBitmap(width: Int = 1086, height: Int = 1448): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val w = width.toFloat()
        val h = height.toFloat()

        // 1. Sky Gradient (Golden hour twilight sky)
        val skyGradient = LinearGradient(
            0f, 0f, 0f, h * 0.55f,
            intArrayOf(
                Color.rgb(30, 58, 138),   // Deep indigo sky
                Color.rgb(59, 130, 246),  // Azure blue
                Color.rgb(251, 146, 60),  // Warm dusk orange
                Color.rgb(254, 215, 170)  // Golden horizon haze
            ),
            floatArrayOf(0f, 0.4f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = skyGradient
        canvas.drawRect(0f, 0f, w, h * 0.55f, paint)

        // 2. Distant Mountain Silhouettes
        paint.shader = null
        paint.color = Color.rgb(67, 56, 120)
        val mountainPath = Path().apply {
            moveTo(0f, h * 0.45f)
            lineTo(w * 0.22f, h * 0.38f)
            lineTo(w * 0.45f, h * 0.46f)
            lineTo(w * 0.72f, h * 0.35f)
            lineTo(w, h * 0.44f)
            lineTo(w, h * 0.55f)
            lineTo(0f, h * 0.55f)
            close()
        }
        canvas.drawPath(mountainPath, paint)

        // 3. Rolling Hill & Vegetation Terrain
        val groundGradient = LinearGradient(
            0f, h * 0.50f, 0f, h,
            intArrayOf(
                Color.rgb(40, 75, 45),    // Forest green
                Color.rgb(30, 55, 35),    // Olive earth
                Color.rgb(22, 38, 25),    // Deep dark terrain
                Color.rgb(18, 28, 20)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        paint.shader = groundGradient
        canvas.drawRect(0f, h * 0.50f, w, h, paint)

        // 4. Subtle architectural grid / pathway
        paint.shader = null
        paint.color = Color.argb(40, 255, 255, 255)
        paint.strokeWidth = 3f
        canvas.drawLine(w * 0.15f, h, w * 0.45f, h * 0.55f, paint)
        canvas.drawLine(w * 0.85f, h, w * 0.55f, h * 0.55f, paint)

        return bitmap
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
