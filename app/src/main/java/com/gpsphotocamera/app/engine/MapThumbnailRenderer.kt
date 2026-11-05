package com.gpsphotocamera.app.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.min

/**
 * High-precision renderer for the satellite map thumbnail, red location marker,
 * and Google watermark branding.
 */
object MapThumbnailRenderer {

    /**
     * Draws the complete satellite map component onto the canvas:
     * 1. Rounded rectangle clipping.
     * 2. Satellite imagery (supplied bitmap or procedural realistic satellite terrain).
     * 3. Google location pin in the center with drop shadow.
     * 4. Google watermark logo at the bottom left.
     */
    fun drawMapThumbnail(
        canvas: Canvas,
        mapRect: RectF,
        cornerRadius: Float,
        config: MapConfig,
        location: LocationData,
        scale: Float
    ) {
        val path = Path().apply {
            addRoundRect(mapRect, cornerRadius, cornerRadius, Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(path)

        // 1. Draw Satellite Imagery
        if (config.satelliteBitmap != null && !config.satelliteBitmap.isRecycled) {
            drawCenterCroppedBitmap(canvas, config.satelliteBitmap, mapRect)
        } else {
            drawProceduralSatelliteTerrain(canvas, mapRect, location)
        }

        canvas.restore()

        // 2. Draw Red Location Marker Pin (Centered)
        if (config.showMarker) {
            val pinCenterX = mapRect.centerX()
            val pinTipY = mapRect.centerY() + (6f * scale)
            val pinWidth = (config.markerWidthPercent * 1086f * scale).coerceAtLeast(18f)
            val pinHeight = (config.markerHeightPercent * 1086f * scale).coerceAtLeast(30f)
            drawGoogleLocationPin(
                canvas = canvas,
                tipX = pinCenterX,
                tipY = pinTipY,
                width = pinWidth,
                height = pinHeight,
                pinColor = config.markerColor,
                centerColor = config.markerCenterColor
            )
        }

        // 3. Draw "Google" Watermark Logo (Bottom Left of Map)
        if (config.showGoogleLogo) {
            drawGoogleWatermark(canvas, mapRect, scale)
        }
    }

    /**
     * Center-crops a bitmap to completely fill [destinationRect] without distortion.
     */
    private fun drawCenterCroppedBitmap(canvas: Canvas, bitmap: Bitmap, destinationRect: RectF) {
        val srcWidth = bitmap.width.toFloat()
        val srcHeight = bitmap.height.toFloat()
        val dstWidth = destinationRect.width()
        val dstHeight = destinationRect.height()

        val scaleFactor = kotlin.math.max(dstWidth / srcWidth, dstHeight / srcHeight)
        val scaledWidth = srcWidth * scaleFactor
        val scaledHeight = srcHeight * scaleFactor

        val dx = destinationRect.left + (dstWidth - scaledWidth) / 2f
        val dy = destinationRect.top + (dstHeight - scaledHeight) / 2f

        val matrix = Matrix().apply {
            postScale(scaleFactor, scaleFactor)
            postTranslate(dx, dy)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, matrix, paint)
    }

    /**
     * Generates an authentic satellite terrain texture matching Google Maps zoom level 16-17:
     * Dense urban buildings, roads, vegetation, and natural terrain contrast.
     */
    private fun drawProceduralSatelliteTerrain(canvas: Canvas, mapRect: RectF, location: LocationData) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(45, 55, 42) // Satellite vegetation base
        }
        canvas.drawRect(mapRect, bgPaint)

        val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(78, 82, 85)
            strokeWidth = mapRect.width() * 0.055f
            style = Paint.Style.STROKE
        }

        val w = mapRect.width()
        val h = mapRect.height()
        val l = mapRect.left
        val t = mapRect.top

        // Main arterial road
        val roadPath = Path().apply {
            moveTo(l, t + h * 0.42f)
            cubicTo(l + w * 0.35f, t + h * 0.45f, l + w * 0.65f, t + h * 0.38f, l + w, t + h * 0.65f)
        }
        canvas.drawPath(roadPath, roadPaint)

        // Secondary cross street
        canvas.drawLine(l + w * 0.52f, t, l + w * 0.48f, t + h, roadPaint)

        // Building rooftop clusters
        val roofPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val buildingColors = intArrayOf(
            Color.rgb(180, 160, 140), // Terracotta/concrete
            Color.rgb(150, 145, 140), // Flat gray
            Color.rgb(120, 115, 110), // Darker concrete
            Color.rgb(195, 185, 175)  // Light roof
        )

        val pseudoRand = (Math.abs(location.latitude * 1000) + Math.abs(location.longitude * 1000)).toInt()

        val cols = 5
        val rows = 5
        val cellW = w / cols
        val cellH = h / rows

        for (i in 0 until cols) {
            for (j in 0 until rows) {
                if ((i == 2 && j == 2) || (i == 2 && j == 1)) continue // Leave intersection clear for pin

                val colorIdx = (pseudoRand + i * 7 + j * 13) % buildingColors.size
                roofPaint.color = buildingColors[colorIdx]

                val bx = l + i * cellW + (cellW * 0.15f)
                val by = t + j * cellH + (cellH * 0.15f)
                val bw = cellW * 0.70f
                val bh = cellH * 0.70f

                canvas.drawRect(bx, by, bx + bw, by + bh, roofPaint)
            }
        }
    }

    /**
     * Draws the authentic Google Maps red location pin with teardrop body and white/dark center dot.
     */
    fun drawGoogleLocationPin(
        canvas: Canvas,
        tipX: Float,
        tipY: Float,
        width: Float,
        height: Float,
        pinColor: Int,
        centerColor: Int
    ) {
        val headRadius = width / 2f
        val headCenterY = tipY - height + headRadius

        // 1. Draw subtle drop shadow at the tip
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawOval(
            tipX - (width * 0.35f),
            tipY - (height * 0.08f),
            tipX + (width * 0.35f),
            tipY + (height * 0.08f),
            shadowPaint
        )

        // 2. Teardrop body path
        val pinPath = Path().apply {
            moveTo(tipX, tipY)
            quadTo(
                tipX - headRadius * 0.85f,
                tipY - height * 0.45f,
                tipX - headRadius,
                headCenterY
            )
            arcTo(
                RectF(tipX - headRadius, headCenterY - headRadius, tipX + headRadius, headCenterY + headRadius),
                180f,
                180f,
                false
            )
            quadTo(
                tipX + headRadius * 0.85f,
                tipY - height * 0.45f,
                tipX,
                tipY
            )
            close()
        }

        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinColor
            style = Paint.Style.FILL
        }
        canvas.drawPath(pinPath, pinPaint)

        // 3. Dark inner center dot
        val dotRadius = headRadius * 0.38f
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = centerColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(tipX, headCenterY, dotRadius, dotPaint)
    }

    /**
     * Draws the official styled "Google" watermark at the bottom-left of the satellite map card.
     */
    private fun drawGoogleWatermark(canvas: Canvas, mapRect: RectF, scale: Float) {
        val text = "Google"
        val textSize = (13f * scale).coerceAtLeast(10f)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            this.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            this.color = Color.WHITE
            setShadowLayer(3f, 0f, 1f, Color.argb(180, 0, 0, 0))
        }

        val margin = 8f * scale
        val x = mapRect.left + margin
        val y = mapRect.bottom - margin
        canvas.drawText(text, x, y, textPaint)
    }
}
