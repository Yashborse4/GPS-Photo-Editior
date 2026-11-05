package com.gpsmapcamera.editor.domain.renderer

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
import java.io.InputStream

object BitmapUtils {

    /**
     * Decodes a Bitmap from Uri at full resolution with correct EXIF orientation.
     */
    fun decodeFullBitmap(context: Context, uri: Uri, maxDimension: Int = 4096): Bitmap? {
        var inputStream: InputStream? = null
        try {
            // First check bounds
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            // Calculate sample size if exceeds maxDimension to prevent OutOfMemoryError
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
            val decodedBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions) ?: return null
            inputStream?.close()

            // Rotate based on EXIF orientation
            val rotated = fixOrientation(context, uri, decodedBitmap)
            return rotated
        } catch (e: OutOfMemoryError) {
            System.gc()
            return null
        } catch (e: Exception) {
            return null
        } finally {
            inputStream?.close()
        }
    }

    /**
     * Decodes a downscaled preview Bitmap for fast UI rendering.
     */
    fun decodePreviewBitmap(context: Context, uri: Uri, targetWidth: Int = 1200): Bitmap? {
        return decodeFullBitmap(context, uri, maxDimension = targetWidth)
    }

    private fun fixOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }

            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0,
                bitmap.width, bitmap.height,
                matrix, true
            )
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            return rotated
        } catch (e: Exception) {
            return bitmap
        } finally {
            inputStream?.close()
        }
    }

    /**
     * Generates a high quality realistic sample landscape photo bitmap
     * with mountains, blue sky, clouds, and road for testing or initial launch.
     */
    fun createSamplePhotoBitmap(width: Int = 1920, height: Int = 1440): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Sky Gradient
        val skyGradient = LinearGradient(
            0f, 0f, 0f, height * 0.6f,
            intArrayOf(Color.parseColor("#1565C0"), Color.parseColor("#42A5F5"), Color.parseColor("#BBDEFB")),
            null, Shader.TileMode.CLAMP
        )
        paint.shader = skyGradient
        canvas.drawRect(0f, 0f, width.toFloat(), height * 0.6f, paint)
        paint.shader = null

        // 2. Distant Mountains (Dark Teal / Slate)
        val mountainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#37474F")
            style = Paint.Style.FILL
        }
        val mountainPath = Path().apply {
            moveTo(0f, height * 0.55f)
            lineTo(width * 0.2f, height * 0.35f)
            lineTo(width * 0.45f, height * 0.50f)
            lineTo(width * 0.7f, height * 0.30f)
            lineTo(width * 0.9f, height * 0.48f)
            lineTo(width.toFloat(), height * 0.40f)
            lineTo(width.toFloat(), height * 0.65f)
            lineTo(0f, height * 0.65f)
            close()
        }
        canvas.drawPath(mountainPath, mountainPaint)

        // 3. Foreground Hills (Forest Green)
        val hillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2E7D32")
            style = Paint.Style.FILL
        }
        val hillPath = Path().apply {
            moveTo(0f, height * 0.60f)
            cubicTo(width * 0.3f, height * 0.52f, width * 0.7f, height * 0.62f, width.toFloat(), height * 0.56f)
            lineTo(width.toFloat(), height * 0.85f)
            lineTo(0f, height * 0.85f)
            close()
        }
        canvas.drawPath(hillPath, hillPaint)

        // 4. Ground / Road (Asphalt)
        val groundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#263238")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, height * 0.75f, width.toFloat(), height.toFloat(), groundPaint)

        // 5. Road center line dashes
        val roadLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD54F")
            strokeWidth = 14f
            style = Paint.Style.STROKE
        }
        val roadPath = Path().apply {
            moveTo(width * 0.5f, height * 0.75f)
            lineTo(width * 0.5f, height.toFloat())
        }
        canvas.drawPath(roadPath, roadLinePaint)

        return bitmap
    }
}
