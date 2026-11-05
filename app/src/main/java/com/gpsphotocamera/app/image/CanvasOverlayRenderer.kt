package com.gpsphotocamera.app.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.gpsphotocamera.app.data.model.GpsPhotoData
import com.gpsphotocamera.app.data.model.StampStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * High-precision graphics rendering engine that draws the GPS Map Camera
 * metadata overlay, satellite map thumbnail, coordinates, and timestamps
 * onto the target photograph using Android Canvas and Bitmap APIs.
 */
object CanvasOverlayRenderer {

    /**
     * Renders the complete GPS information overlay on top of [baseBitmap]
     * using the metadata provided in [data] and map tile [mapThumbnail].
     *
     * Returns a new immutable [Bitmap] with the stamp permanently applied.
     */
    suspend fun renderOverlay(
        baseBitmap: Bitmap,
        data: GpsPhotoData,
        mapThumbnail: Bitmap
    ): Bitmap = withContext(Dispatchers.Default) {
        // Create a mutable copy with ARGB_8888 config to support alpha blending
        val outputBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)

        val imageWidth = outputBitmap.width.toFloat()
        val imageHeight = outputBitmap.height.toFloat()

        // Responsive scaling factor based on 1080px reference width
        val scale = max(0.5f, imageWidth / 1080f)

        when (data.stampStyle) {
            StampStyle.CLASSIC_BOTTOM -> {
                drawClassicBottomBanner(canvas, imageWidth, imageHeight, scale, data, mapThumbnail)
            }
            StampStyle.MODERN_CARD -> {
                drawModernFloatingCard(canvas, imageWidth, imageHeight, scale, data, mapThumbnail)
            }
            StampStyle.COMPACT_MINIMAL -> {
                drawCompactMinimalBanner(canvas, imageWidth, imageHeight, scale, data, mapThumbnail)
            }
        }

        outputBitmap
    }

    /**
     * Classic GPS Map Camera layout:
     * Full-width dark semi-transparent banner at the bottom with:
     * - Square satellite map thumbnail on the left
     * - Location heading (bold)
     * - Full postal address (multi-line)
     * - GPS coordinates (Lat / Long)
     * - Date, Time & Timezone
     * - Custom note
     * - GPS Camera watermark badge
     */
    private fun drawClassicBottomBanner(
        canvas: Canvas,
        width: Float,
        height: Float,
        scale: Float,
        data: GpsPhotoData,
        mapThumbnail: Bitmap
    ) {
        val bannerPadding = 24f * scale
        val mapSize = 220f * scale
        val contentLeft = bannerPadding + mapSize + (20f * scale)
        val contentRight = width - bannerPadding

        // 1. Calculate height needed for content (including wrapped address and note)
        val titleTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val addressTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240) // Slate 200
            textSize = 19f * scale
            typeface = Typeface.DEFAULT
        }

        val availableTextWidth = (contentRight - contentLeft).toInt().coerceAtLeast(100)
        val addressLayout = StaticLayout.Builder.obtain(
            data.postalAddress,
            0,
            data.postalAddress.length,
            addressTextPaint,
            availableTextWidth
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f * scale, 1f)
            .setMaxLines(3)
            .build()

        val addressHeight = addressLayout.height.toFloat()

        val metaTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(52, 211, 153) // Emerald accent for coordinates
            textSize = 19f * scale
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val timeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225) // Slate 300
            textSize = 18f * scale
            typeface = Typeface.DEFAULT
        }

        val noteTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(253, 224, 71) // Warm amber for note
            textSize = 17f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        var textContentHeight = (32f * scale) + // Title
                (8f * scale) + addressHeight + // Address
                (8f * scale) + (22f * scale) + // Coordinates
                (6f * scale) + (20f * scale)   // Timestamp

        if (data.customNote.isNotBlank()) {
            textContentHeight += (6f * scale) + (20f * scale)
        }

        val bannerContentHeight = max(mapSize, textContentHeight)
        val bannerHeight = bannerContentHeight + (bannerPadding * 2)
        val bannerTop = height - bannerHeight

        // 2. Draw Translucent Dark Gradient Banner Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, bannerTop, 0f, height,
                intArrayOf(
                    Color.argb(220, 15, 23, 42),  // Top: Slate 900 translucent
                    Color.argb(245, 10, 15, 30)   // Bottom: Deep slate almost opaque
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        val bannerRect = RectF(0f, bannerTop, width, height)
        canvas.drawRect(bannerRect, bgPaint)

        // Subtle top accent line (Emerald green)
        val topBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 16, 185, 129)
            strokeWidth = 3f * scale
        }
        canvas.drawLine(0f, bannerTop, width, bannerTop, topBorderPaint)

        // 3. Draw Map Thumbnail
        val mapTop = bannerTop + bannerPadding
        val mapRect = RectF(bannerPadding, mapTop, bannerPadding + mapSize, mapTop + mapSize)

        // Map border / outline
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * scale
        }
        canvas.drawBitmap(mapThumbnail, null, mapRect, null)
        canvas.drawRoundRect(mapRect, 8f * scale, 8f * scale, borderPaint)

        // Micro branding tag under map
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 12f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "GPS MAP CAMERA",
            bannerPadding + (mapSize / 2),
            mapTop + mapSize + (16f * scale),
            tagPaint
        )

        // 4. Draw Metadata Text on the right
        var currentY = bannerTop + bannerPadding + (24f * scale)

        // Location Heading
        canvas.drawText(data.locationHeading, contentLeft, currentY, titleTextPaint)
        currentY += 12f * scale

        // Postal Address (wrapped)
        canvas.save()
        canvas.translate(contentLeft, currentY)
        addressLayout.draw(canvas)
        canvas.restore()
        currentY += addressHeight + (10f * scale)

        // GPS Coordinates (Lat & Long)
        val coordString = data.formattedCoordinates()
        canvas.drawText(coordString, contentLeft, currentY, metaTextPaint)
        currentY += 24f * scale

        // Date, Time & Timezone
        val timestampString = data.formattedTimestampWithZone()
        canvas.drawText(timestampString, contentLeft, currentY, timeTextPaint)

        // Custom Note
        if (data.customNote.isNotBlank()) {
            currentY += 22f * scale
            canvas.drawText("Note: ${data.customNote}", contentLeft, currentY, noteTextPaint)
        }
    }

    /**
     * Modern Floating Card Layout:
     * Rounded card floating slightly above the bottom with soft drop shadow.
     */
    private fun drawModernFloatingCard(
        canvas: Canvas,
        width: Float,
        height: Float,
        scale: Float,
        data: GpsPhotoData,
        mapThumbnail: Bitmap
    ) {
        val marginH = 32f * scale
        val marginB = 32f * scale
        val cardPadding = 20f * scale
        val mapSize = 180f * scale

        val cardLeft = marginH
        val cardRight = width - marginH
        val contentLeft = cardLeft + cardPadding + mapSize + (18f * scale)
        val contentRight = cardRight - cardPadding

        val availableWidth = (contentRight - contentLeft).toInt().coerceAtLeast(100)

        val addressPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(241, 245, 249)
            textSize = 18f * scale
        }
        val addressLayout = StaticLayout.Builder.obtain(
            data.postalAddress, 0, data.postalAddress.length, addressPaint, availableWidth
        ).setMaxLines(2).build()

        val cardHeight = max(mapSize + (cardPadding * 2), 220f * scale + addressLayout.height)
        val cardTop = height - marginB - cardHeight
        val cardBottom = height - marginB
        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)

        // Card background (Dark Glass)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(230, 15, 23, 42)
        }
        canvas.drawRoundRect(cardRect, 24f * scale, 24f * scale, bgPaint)

        // Card border (Subtle Cyan accent)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(100, 56, 189, 248)
            style = Paint.Style.STROKE
            strokeWidth = 2f * scale
        }
        canvas.drawRoundRect(cardRect, 24f * scale, 24f * scale, borderPaint)

        // Map
        val mapRect = RectF(
            cardLeft + cardPadding,
            cardTop + cardPadding,
            cardLeft + cardPadding + mapSize,
            cardTop + cardPadding + mapSize
        )
        canvas.drawBitmap(mapThumbnail, null, mapRect, null)
        borderPaint.color = Color.argb(160, 255, 255, 255)
        canvas.drawRoundRect(mapRect, 16f * scale, 16f * scale, borderPaint)

        // Text
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        var textY = cardTop + cardPadding + (24f * scale)
        canvas.drawText(data.locationHeading, contentLeft, textY, titlePaint)
        textY += 10f * scale

        canvas.save()
        canvas.translate(contentLeft, textY)
        addressLayout.draw(canvas)
        canvas.restore()
        textY += addressLayout.height + (12f * scale)

        val coordPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(56, 189, 248) // Sky blue
            textSize = 17f * scale
            typeface = Typeface.MONOSPACE
        }
        canvas.drawText(data.formattedCoordinates(), contentLeft, textY, coordPaint)
        textY += 22f * scale

        val timePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            textSize = 16f * scale
        }
        canvas.drawText(data.formattedTimestampWithZone(), contentLeft, textY, timePaint)
    }

    /**
     * Compact Minimal Banner:
     * Streamlined watermark banner for minimalist photo aesthetics.
     */
    private fun drawCompactMinimalBanner(
        canvas: Canvas,
        width: Float,
        height: Float,
        scale: Float,
        data: GpsPhotoData,
        mapThumbnail: Bitmap
    ) {
        val bannerHeight = 120f * scale
        val bannerTop = height - bannerHeight
        val padding = 16f * scale
        val mapSize = bannerHeight - (padding * 2)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 0, 0, 0)
        }
        canvas.drawRect(0f, bannerTop, width, height, bgPaint)

        // Small map
        val mapRect = RectF(padding, bannerTop + padding, padding + mapSize, bannerTop + padding + mapSize)
        canvas.drawBitmap(mapThumbnail, null, mapRect, null)

        val textLeft = padding + mapSize + (16f * scale)
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            textSize = 16f * scale
        }

        canvas.drawText(data.locationHeading, textLeft, bannerTop + padding + (22f * scale), titlePaint)
        canvas.drawText(data.formattedCoordinates(), textLeft, bannerTop + padding + (48f * scale), textPaint)
        canvas.drawText(data.formattedTimestampWithZone(), textLeft, bannerTop + padding + (72f * scale), textPaint)
    }
}
