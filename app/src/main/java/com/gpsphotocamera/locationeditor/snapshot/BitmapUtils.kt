package com.gpsphotocamera.locationeditor.snapshot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.gpsphotocamera.locationeditor.model.ThumbnailConfig
import com.gpsphotocamera.locationeditor.model.ThumbnailShape
import kotlin.math.min

/**
 * High-performance Android Bitmap processing utilities for map thumbnails.
 * Handles centering, aspect-ratio cropping, scaling, corner rounding, and border styling.
 */
object BitmapUtils {

    /**
     * Center-crop a source bitmap to match a target aspect ratio.
     * Ensures the center of the map (where the location marker resides) is perfectly preserved.
     */
    fun cropToAspectRatio(source: Bitmap, ratioWidth: Float, ratioHeight: Float): Bitmap {
        val srcWidth = source.width
        val srcHeight = source.height
        val targetAspect = ratioWidth / ratioHeight
        val srcAspect = srcWidth.toFloat() / srcHeight.toFloat()

        var cropWidth = srcWidth
        var cropHeight = srcHeight
        var cropX = 0
        var cropY = 0

        if (srcAspect > targetAspect) {
            // Source is wider than target aspect: crop sides
            cropWidth = (srcHeight * targetAspect).toInt()
            cropX = (srcWidth - cropWidth) / 2
        } else if (srcAspect < targetAspect) {
            // Source is taller than target aspect: crop top and bottom
            cropHeight = (srcWidth / targetAspect).toInt()
            cropY = (srcHeight - cropHeight) / 2
        }

        return Bitmap.createBitmap(source, cropX, cropY, cropWidth, cropHeight)
    }

    /**
     * Scale a bitmap to exact target dimensions.
     */
    fun scaleBitmap(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        if (source.width == targetWidth && source.height == targetHeight) {
            return source
        }
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    /**
     * Apply corner radius (rounded rect) or circular masking and optional border to the bitmap.
     */
    fun formatThumbnail(source: Bitmap, config: ThumbnailConfig, density: Float = 2.0f): Bitmap {
        // Step 1: Center-crop to the target aspect ratio
        val cropped = cropToAspectRatio(
            source,
            config.aspectRatio.ratioWidth,
            config.aspectRatio.ratioHeight
        )

        // Step 2: Scale to the configured pixel resolution
        val scaled = scaleBitmap(cropped, config.widthPx, config.heightPx)

        // Step 3: Apply shape mask and border
        val output = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }

        val rect = RectF(0f, 0f, scaled.width.toFloat(), scaled.height.toFloat())
        val cornerRadiusPx = config.cornerRadiusDp * density
        val borderWidthPx = config.borderWidthDp * density

        val path = Path()
        when (config.shape) {
            ThumbnailShape.CIRCLE -> {
                val radius = min(scaled.width, scaled.height) / 2f
                path.addCircle(scaled.width / 2f, scaled.height / 2f, radius, Path.Direction.CCW)
            }
            ThumbnailShape.ROUNDED_RECT -> {
                path.addRoundRect(rect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CCW)
            }
            ThumbnailShape.SHARP_RECT -> {
                path.addRect(rect, Path.Direction.CCW)
            }
        }

        // Draw shape mask
        canvas.drawPath(path, paint)

        // Draw the scaled bitmap inside the shape mask
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, 0f, 0f, paint)

        // Step 4: Draw crisp outer border
        if (borderWidthPx > 0f) {
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = borderWidthPx
                color = Color.WHITE
            }
            // Inset by half stroke width to keep border inside bounds
            val insetRect = RectF(rect).apply {
                inset(borderWidthPx / 2f, borderWidthPx / 2f)
            }
            val strokePath = Path()
            when (config.shape) {
                ThumbnailShape.CIRCLE -> {
                    val radius = (min(scaled.width, scaled.height) - borderWidthPx) / 2f
                    strokePath.addCircle(scaled.width / 2f, scaled.height / 2f, radius, Path.Direction.CCW)
                }
                ThumbnailShape.ROUNDED_RECT -> {
                    val adjustedRadius = (cornerRadiusPx - borderWidthPx / 2f).coerceAtLeast(0f)
                    strokePath.addRoundRect(insetRect, adjustedRadius, adjustedRadius, Path.Direction.CCW)
                }
                ThumbnailShape.SHARP_RECT -> {
                    strokePath.addRect(insetRect, Path.Direction.CCW)
                }
            }
            paint.xfermode = null
            canvas.drawPath(strokePath, strokePaint)
        }

        return output
    }
}
