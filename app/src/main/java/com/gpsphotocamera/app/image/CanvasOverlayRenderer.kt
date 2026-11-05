package com.gpsphotocamera.app.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.gpsphotocamera.app.data.model.GpsPhotoData
import com.gpsphotocamera.app.data.model.StampStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * High-precision graphics rendering engine that draws the GPS Map Camera
 * metadata overlay, satellite map thumbnail, coordinates, and timestamps
 * onto the target photograph using Android Canvas and Bitmap APIs.
 *
 * Implements resolution-independent scaling with normalized coordinates (normX, normY)
 * so the preview position translates with 100% fidelity to the high-resolution output.
 */
object CanvasOverlayRenderer {

    suspend fun renderOverlay(
        baseBitmap: Bitmap,
        data: GpsPhotoData,
        mapThumbnail: Bitmap?
    ): Bitmap = withContext(Dispatchers.Default) {
        val outputBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)

        val imageWidth = outputBitmap.width.toFloat()
        val imageHeight = outputBitmap.height.toFloat()

        // Responsive scaling factor based on 1080px baseline
        val scale = max(0.6f, imageWidth / 1080f)

        when (data.stampStyle) {
            StampStyle.CLASSIC_BOTTOM -> {
                drawClassicPreset(canvas, imageWidth, imageHeight, scale, data, mapThumbnail)
            }
            StampStyle.COMPACT_MINIMAL -> {
                drawCompactPreset(canvas, imageWidth, imageHeight, scale, data, mapThumbnail)
            }
            StampStyle.MODERN_CARD -> {
                drawModernCardPreset(canvas, imageWidth, imageHeight, scale, data, mapThumbnail)
            }
            StampStyle.MINIMAL_WATERMARK -> {
                drawMinimalWatermarkPreset(canvas, imageWidth, imageHeight, scale, data)
            }
        }

        outputBitmap
    }

    private fun getTypeface(family: String, isBold: Boolean): Typeface {
        val baseTypeface = when (family.uppercase()) {
            "MONOSPACE" -> Typeface.MONOSPACE
            "SERIF" -> Typeface.SERIF
            else -> Typeface.DEFAULT
        }
        val style = if (isBold) Typeface.BOLD else Typeface.NORMAL
        return Typeface.create(baseTypeface, style)
    }

    /**
     * Classic GPS Map Camera layout strictly reproducing the reference photograph:
     * - Dark translucent card
     * - Left: Map thumbnail with red pin & compass rose
     * - Right: Heading, address, coordinates, altitude + compass, date/time, note
     * - Top-right badge "GPS Map Camera"
     */
    private fun drawClassicPreset(
        canvas: Canvas,
        width: Float,
        height: Float,
        scale: Float,
        data: GpsPhotoData,
        mapThumbnail: Bitmap?
    ) {
        val panelWidth = (width * data.panelWidthPercent * data.scale).coerceAtMost(width * 0.98f)
        val padding = data.panelHeightPaddingDp * scale * 1.4f
        val cornerRadius = data.cornerRadiusDp * scale

        val typeface = getTypeface(data.fontFamily, data.isBold)
        val titleTypeface = getTypeface(data.fontFamily, true)

        val titleSize = 22f * scale * data.fontSizeMultiplier
        val bodySize = 16f * scale * data.fontSizeMultiplier
        val metaSize = 14.5f * scale * data.fontSizeMultiplier
        val lineSpacing = data.lineSpacingDp * scale

        val textColor = data.textColorHex.toInt()
        val accentColor = data.accentColorHex.toInt()

        // Map dimensions
        val showMap = data.showMap && mapThumbnail != null
        val mapWidth = if (showMap) (panelWidth * 0.28f * data.mapThumbnailScale).coerceAtLeast(100f * scale) else 0f
        val mapHeight = when (data.mapAspectRatio) {
            "LANDSCAPE" -> mapWidth * 0.75f
            "WIDE" -> mapWidth * 0.56f
            else -> mapWidth
        }

        val textContentWidth = if (showMap) {
            panelWidth - mapWidth - (padding * 2.5f)
        } else {
            panelWidth - (padding * 2f)
        }.toInt().coerceAtLeast(100)

        // Text paints
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = titleSize
            this.typeface = titleTypeface
        }

        val addressPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(230, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            textSize = bodySize
            this.typeface = typeface
        }

        val coordPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = metaSize
            this.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            textSize = metaSize
            this.typeface = typeface
        }

        val notePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(253, 224, 71) // Warm Amber
            textSize = metaSize
            this.typeface = typeface
        }

        // Layout text lines
        val alignment = when (data.textAlign.uppercase()) {
            "CENTER" -> Layout.Alignment.ALIGN_CENTER
            "RIGHT" -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }

        val titleLayout = StaticLayout.Builder.obtain(data.locationHeading, 0, data.locationHeading.length, titlePaint, textContentWidth)
            .setAlignment(alignment).setMaxLines(1).build()

        val addressLayout = StaticLayout.Builder.obtain(data.postalAddress, 0, data.postalAddress.length, addressPaint, textContentWidth)
            .setAlignment(alignment).setMaxLines(2).build()

        val coordsText = data.formattedCoordinates()
        val coordsLayout = StaticLayout.Builder.obtain(coordsText, 0, coordsText.length, coordPaint, textContentWidth)
            .setAlignment(alignment).setMaxLines(1).build()

        val altHeadingText = data.formattedAltitudeCompass()
        val altLayout = StaticLayout.Builder.obtain(altHeadingText, 0, altHeadingText.length, metaPaint, textContentWidth)
            .setAlignment(alignment).setMaxLines(1).build()

        val dateTimeText = data.formattedTimestampWithZone()
        val dateLayout = StaticLayout.Builder.obtain(dateTimeText, 0, dateTimeText.length, metaPaint, textContentWidth)
            .setAlignment(alignment).setMaxLines(1).build()

        val noteLayout = if (data.showNote && data.customNote.isNotBlank()) {
            val noteString = "Note: ${data.customNote}"
            StaticLayout.Builder.obtain(noteString, 0, noteString.length, notePaint, textContentWidth)
                .setAlignment(alignment).setMaxLines(1).build()
        } else null

        // Calculate heights
        val badgeHeight = 22f * scale
        var textTotalHeight = titleLayout.height + addressLayout.height + coordsLayout.height + altLayout.height + dateLayout.height + (5 * lineSpacing)
        if (noteLayout != null) {
            textTotalHeight += noteLayout.height + lineSpacing
        }

        val panelContentHeight = max(textTotalHeight, mapHeight)
        val panelHeight = panelContentHeight + (padding * 2f) + badgeHeight

        // Position from normalized coordinates
        val centerX = width * data.normX
        val bottomY = height * data.normY
        val left = (centerX - (panelWidth / 2f)).coerceIn(8f * scale, width - panelWidth - 8f * scale)
        val top = (bottomY - panelHeight).coerceIn(8f * scale, height - panelHeight - 8f * scale)
        val panelRect = RectF(left, top, left + panelWidth, top + panelHeight)

        // 1. Draw Panel Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = data.backgroundColorHex.toInt()
            alpha = (data.backgroundOpacity * 255).roundToInt().coerceIn(0, 255)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(panelRect, cornerRadius, cornerRadius, bgPaint)

        // 2. Draw Top Right Badge
        val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 255, 255, 255)
            textSize = 13f * scale
            this.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("GPS Map Camera", left + panelWidth - padding, top + (16f * scale), badgePaint)

        // 3. Draw Map Thumbnail
        val contentTop = top + badgeHeight + (padding * 0.6f)
        var textLeft = left + padding

        if (showMap && mapThumbnail != null) {
            val mapLeft = left + padding
            val mapTop = contentTop + ((panelContentHeight - mapHeight) / 2f)
            val mapRect = RectF(mapLeft, mapTop, mapLeft + mapWidth, mapTop + mapHeight)

            // Clip map rounded corners
            canvas.save()
            val clipPath = Path().apply {
                addRoundRect(mapRect, 8f * scale, 8f * scale, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            canvas.drawBitmap(mapThumbnail, null, mapRect, null)

            // Red Location Pin
            if (data.showMarker) {
                val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    style = Paint.Style.FILL
                }
                val pinX = mapLeft + (mapWidth / 2f)
                val pinY = mapTop + (mapHeight / 2f)
                canvas.drawCircle(pinX, pinY, 6f * scale, pinPaint)
                pinPaint.color = Color.WHITE
                canvas.drawCircle(pinX, pinY, 2.5f * scale, pinPaint)
            }

            canvas.restore()

            // Border around map
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(120, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 1.5f * scale
            }
            canvas.drawRoundRect(mapRect, 8f * scale, 8f * scale, borderPaint)

            textLeft = mapLeft + mapWidth + (padding * 0.8f)
        }

        // 4. Draw Text Elements
        var currentY = contentTop
        fun drawLayout(layout: StaticLayout) {
            canvas.save()
            canvas.translate(textLeft, currentY)
            layout.draw(canvas)
            canvas.restore()
            currentY += layout.height + lineSpacing
        }

        drawLayout(titleLayout)
        drawLayout(addressLayout)
        drawLayout(coordsLayout)
        drawLayout(altLayout)
        drawLayout(dateLayout)
        noteLayout?.let { drawLayout(it) }
    }

    /**
     * Compact layout banner.
     */
    private fun drawCompactPreset(
        canvas: Canvas,
        width: Float,
        height: Float,
        scale: Float,
        data: GpsPhotoData,
        mapThumbnail: Bitmap?
    ) {
        val panelWidth = (width * data.panelWidthPercent * data.scale).coerceAtMost(width * 0.98f)
        val bannerHeight = 110f * scale
        val padding = 14f * scale
        val mapSize = bannerHeight - (padding * 2f)

        val centerX = width * data.normX
        val bottomY = height * data.normY
        val left = (centerX - (panelWidth / 2f)).coerceIn(8f * scale, width - panelWidth - 8f * scale)
        val top = (bottomY - bannerHeight).coerceIn(8f * scale, height - bannerHeight - 8f * scale)
        val panelRect = RectF(left, top, left + panelWidth, top + bannerHeight)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = data.backgroundColorHex.toInt()
            alpha = (data.backgroundOpacity * 255).roundToInt().coerceIn(0, 255)
        }
        canvas.drawRoundRect(panelRect, data.cornerRadiusDp * scale, data.cornerRadiusDp * scale, bgPaint)

        var textLeft = left + padding
        if (data.showMap && mapThumbnail != null) {
            val mapRect = RectF(left + padding, top + padding, left + padding + mapSize, top + padding + mapSize)
            canvas.drawBitmap(mapThumbnail, null, mapRect, null)
            textLeft += mapSize + padding
        }

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = data.textColorHex.toInt()
            textSize = 19f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = data.accentColorHex.toInt()
            textSize = 15f * scale
            typeface = Typeface.MONOSPACE
        }
        val datePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            textSize = 14f * scale
        }

        canvas.drawText(data.locationHeading, textLeft, top + padding + (20f * scale), titlePaint)
        canvas.drawText(data.formattedCoordinates(), textLeft, top + padding + (44f * scale), subPaint)
        canvas.drawText(data.formattedTimestampWithZone(), textLeft, top + padding + (66f * scale), datePaint)
    }

    /**
     * Modern Card layout with elevated landscape map and clean metadata grid.
     */
    private fun drawModernCardPreset(
        canvas: Canvas,
        width: Float,
        height: Float,
        scale: Float,
        data: GpsPhotoData,
        mapThumbnail: Bitmap?
    ) {
        val panelWidth = (width * data.panelWidthPercent * data.scale).coerceAtMost(width * 0.98f)
        val padding = 16f * scale
        val cardHeight = 260f * scale

        val centerX = width * data.normX
        val bottomY = height * data.normY
        val left = (centerX - (panelWidth / 2f)).coerceIn(8f * scale, width - panelWidth - 8f * scale)
        val top = (bottomY - cardHeight).coerceIn(8f * scale, height - cardHeight - 8f * scale)
        val panelRect = RectF(left, top, left + panelWidth, top + cardHeight)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = data.backgroundColorHex.toInt()
            alpha = (data.backgroundOpacity * 255).roundToInt().coerceIn(0, 255)
        }
        canvas.drawRoundRect(panelRect, data.cornerRadiusDp * scale, data.cornerRadiusDp * scale, bgPaint)

        var currentY = top + padding + (18f * scale)
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = data.textColorHex.toInt()
            textSize = 21f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(data.locationHeading, left + padding, currentY, titlePaint)
        currentY += 12f * scale

        if (data.showMap && mapThumbnail != null) {
            val mapH = 90f * scale
            val mapRect = RectF(left + padding, currentY, left + panelWidth - padding, currentY + mapH)
            canvas.drawBitmap(mapThumbnail, null, mapRect, null)
            currentY += mapH + (12f * scale)
        }

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            textSize = 15f * scale
        }
        val coordPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = data.accentColorHex.toInt()
            textSize = 15f * scale
            typeface = Typeface.MONOSPACE
        }

        canvas.drawText(data.postalAddress, left + padding, currentY + (14f * scale), textPaint)
        canvas.drawText(data.formattedCoordinates(), left + padding, currentY + (34f * scale), coordPaint)
        canvas.drawText(data.formattedTimestampWithZone(), left + padding, currentY + (54f * scale), textPaint)
    }

    /**
     * Minimal Watermark preset pill.
     */
    private fun drawMinimalWatermarkPreset(
        canvas: Canvas,
        width: Float,
        height: Float,
        scale: Float,
        data: GpsPhotoData
    ) {
        val panelWidth = (width * data.panelWidthPercent * data.scale).coerceAtMost(width * 0.98f)
        val pillHeight = 72f * scale

        val centerX = width * data.normX
        val bottomY = height * data.normY
        val left = (centerX - (panelWidth / 2f)).coerceIn(8f * scale, width - panelWidth - 8f * scale)
        val top = (bottomY - pillHeight).coerceIn(8f * scale, height - pillHeight - 8f * scale)
        val panelRect = RectF(left, top, left + panelWidth, top + pillHeight)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = data.backgroundColorHex.toInt()
            alpha = (data.backgroundOpacity * 255).roundToInt().coerceIn(0, 255)
        }
        canvas.drawRoundRect(panelRect, pillHeight / 2f, pillHeight / 2f, bgPaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = data.textColorHex.toInt()
            textSize = 16f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = data.accentColorHex.toInt()
            textSize = 14f * scale
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }

        val midX = left + (panelWidth / 2f)
        canvas.drawText("${data.locationHeading} • ${data.formattedCoordinates()}", midX, top + (28f * scale), titlePaint)
        canvas.drawText("${data.formattedTimestampWithZone()} • Alt ${data.altitudeMeters.toInt()}m", midX, top + (52f * scale), subPaint)
    }
}
