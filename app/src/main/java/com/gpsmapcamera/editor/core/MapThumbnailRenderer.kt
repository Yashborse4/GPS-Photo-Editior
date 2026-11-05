package com.gpsmapcamera.editor.core

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
            // In Google Maps, the pin's bottom tip points exactly at the target coordinate (center)
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
     * - Dark vegetation patches
     * - Urban street grid and road networks
     * - Varied building rooftop blocks
     * - Ambient water bodies
     */
    fun drawProceduralSatelliteTerrain(canvas: Canvas, rect: RectF, location: LocationData) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Base terrain (Dark forest green / earth tone)
        paint.color = Color.parseColor("#223326")
        canvas.drawRect(rect, paint)

        // Seed determinism based on coordinates
        val seed = ((location.latitude * 10000).toInt() xor (location.longitude * 10000).toInt()).toLong()
        val random = java.util.Random(seed)

        // 1. Organic terrain patches
        val patchPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 0..12) {
            val px = rect.left + random.nextFloat() * rect.width()
            val py = rect.top + random.nextFloat() * rect.height()
            val pr = (rect.width() * (0.15f + random.nextFloat() * 0.25f))
            patchPaint.color = if (i % 2 == 0) Color.parseColor("#1B2E21") else Color.parseColor("#2D4030")
            canvas.drawCircle(px, py, pr, patchPaint)
        }

        // 2. Water body / Canal patch
        val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#183042")
        }
        val waterPath = Path().apply {
            val startY = rect.top + rect.height() * 0.35f
            moveTo(rect.left, startY)
            quadTo(
                rect.left + rect.width() * 0.5f,
                rect.top + rect.height() * 0.25f,
                rect.right,
                rect.top + rect.height() * 0.45f
            )
            lineTo(rect.right, rect.top + rect.height() * 0.55f)
            quadTo(
                rect.left + rect.width() * 0.5f,
                rect.top + rect.height() * 0.35f,
                rect.left,
                rect.top + rect.height() * 0.45f
            )
            close()
        }
        canvas.drawPath(waterPath, waterPaint)

        // 3. Street Grid (Asphalt & Roads)
        val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4A524A")
            strokeWidth = rect.width() * 0.045f
            style = Paint.Style.STROKE
        }
        // Horizontal streets
        canvas.drawLine(rect.left, rect.top + rect.height() * 0.28f, rect.right, rect.top + rect.height() * 0.28f, roadPaint)
        canvas.drawLine(rect.left, rect.top + rect.height() * 0.65f, rect.right, rect.top + rect.height() * 0.65f, roadPaint)
        // Vertical streets
        canvas.drawLine(rect.left + rect.width() * 0.32f, rect.top, rect.left + rect.width() * 0.32f, rect.bottom, roadPaint)
        canvas.drawLine(rect.left + rect.width() * 0.72f, rect.top, rect.left + rect.width() * 0.72f, rect.bottom, roadPaint)

        // 4. Urban Building Footprints / Rooftops
        val buildingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val buildingColors = arrayOf(
            "#6E6359", "#7D7267", "#545C55", "#8A7D71", "#3E4540", "#63584E"
        )
        val cols = 6
        val rows = 6
        val cellW = rect.width() / cols
        val cellH = rect.height() / rows

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (random.nextFloat() > 0.35f) {
                    val bx = rect.left + c * cellW + cellW * 0.15f
                    val by = rect.top + r * cellH + cellH * 0.15f
                    val bw = cellW * (0.6f + random.nextFloat() * 0.25f)
                    val bh = cellH * (0.6f + random.nextFloat() * 0.25f)

                    buildingPaint.color = Color.parseColor(buildingColors[random.nextInt(buildingColors.size)])
                    canvas.drawRoundRect(RectF(bx, by, bx + bw, by + bh), 2f, 2f, buildingPaint)

                    // Small rooftop shadow
                    buildingPaint.color = Color.argb(80, 0, 0, 0)
                    canvas.drawRect(bx, by + bh - 2f, bx + bw, by + bh, buildingPaint)
                }
            }
        }
    }

    /**
     * Draws the iconic Google Maps teardrop location pin with a dark red center dot and drop shadow.
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

        // 1. Drop shadow ellipse on the ground
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 0, 0, 0)
            style = Paint.Style.FILL
        }
        val shadowRect = RectF(
            tipX - (width * 0.35f),
            tipY - (width * 0.10f),
            tipX + (width * 0.35f),
            tipY + (width * 0.15f)
        )
        canvas.drawOval(shadowRect, shadowPaint)

        // 2. Pin body (Teardrop vector path)
        val pinPath = Path().apply {
            moveTo(tipX, tipY)
            // Left flank curving up to tangent of circular head
            cubicTo(
                tipX - (width * 0.05f), tipY - (height * 0.40f),
                tipX - headRadius, headCenterY + (headRadius * 0.45f),
                tipX - headRadius, headCenterY
            )
            // Top circular arc
            arcTo(
                RectF(tipX - headRadius, headCenterY - headRadius, tipX + headRadius, headCenterY + headRadius),
                180f,
                180f,
                false
            )
            // Right flank curving back down to tip
            cubicTo(
                tipX + headRadius, headCenterY + (headRadius * 0.45f),
                tipX + (width * 0.05f), tipY - (height * 0.40f),
                tipX, tipY
            )
            close()
        }

        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinColor
            style = Paint.Style.FILL
        }
        canvas.drawPath(pinPath, pinPaint)

        // Subtle highlight on top left of pin head
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 255, 255, 255)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(tipX - (headRadius * 0.25f), headCenterY - (headRadius * 0.25f), headRadius * 0.5f, highlightPaint)

        // 3. Center dark circle cutout / eye
        val centerRadius = headRadius * 0.42f
        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = centerColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(tipX, headCenterY, centerRadius, centerPaint)
    }

    /**
     * Draws the official Google watermark logo at the bottom left of the map thumbnail.
     * Features crisp white lettering with a dark drop shadow outline for visibility over satellite tiles.
     */
    fun drawGoogleWatermark(canvas: Canvas, mapRect: RectF, scale: Float) {
        val logoText = "Google"
        val fontSize = (22f * scale).coerceAtLeast(14f)

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            this.textSize = fontSize
            color = Color.argb(200, 0, 0, 0)
            style = Paint.Style.STROKE
            strokeWidth = 3.5f * scale
            strokeJoin = Paint.Join.ROUND
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            this.textSize = fontSize
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val x = mapRect.left + (12f * scale)
        val y = mapRect.bottom - (12f * scale)

        // Draw shadow/outline first, then white fill
        canvas.drawText(logoText, x, y, shadowPaint)
        canvas.drawText(logoText, x, y, textPaint)
    }
}
