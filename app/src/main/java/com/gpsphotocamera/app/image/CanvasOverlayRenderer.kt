package com.gpsphotocamera.app.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import com.gpsphotocamera.app.data.model.GpsPhotoData
import com.gpsphotocamera.app.data.model.StampStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pixel-accurate graphics rendering engine that draws the GPS Map Camera
 * metadata overlay, satellite map thumbnail, coordinates, and timestamps
 * onto the target photograph using Android Canvas and Bitmap APIs.
 *
 * Faithfully reproduces the visual appearance, typography, logo, and layout
 * of the GPS Map Camera reference photograph across all resolutions.
 */
object CanvasOverlayRenderer {

    suspend fun renderOverlay(
        baseBitmap: Bitmap,
        data: GpsPhotoData,
        mapThumbnail: Bitmap?
    ): Bitmap = withContext(Dispatchers.Default) {
        if (data.stampStyle == StampStyle.CLASSIC_BOTTOM) {
            val config = com.gpsphotocamera.app.engine.OverlayConfig.fromGpsPhotoData(data, mapThumbnail)
            return@withContext com.gpsphotocamera.app.engine.OverlayRenderer.renderOverlay(baseBitmap, config)
        }

        val outputBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)

        val imageWidth = outputBitmap.width.toFloat()
        val imageHeight = outputBitmap.height.toFloat()

        when (data.stampStyle) {
            StampStyle.CLASSIC_BOTTOM -> {
                // Handled above
            }
            StampStyle.COMPACT_MINIMAL -> {
                drawCompactPreset(canvas, imageWidth, imageHeight, data, mapThumbnail)
            }
            StampStyle.MODERN_CARD -> {
                drawModernCardPreset(canvas, imageWidth, imageHeight, data, mapThumbnail)
            }
            StampStyle.MINIMAL_WATERMARK -> {
                drawMinimalWatermarkPreset(canvas, imageWidth, imageHeight, data)
            }
        }

        outputBitmap
    }

    /**
     * Exact 1:1 reproduction of the reference photograph:
     * - Left: Standalone satellite map thumbnail (198x198 px on 1024 base) with red pin,
     *   blue heading radar cone, and Google script watermark.
     * - Right: Standalone dark translucent text card (596x198 px on 1024 base) with pure white typography:
     *   Line 1: Heading with national flag emoji (Pune, Maharashtra, India 🇮🇳)
     *   Line 2 & 3: Multi-line postal address
     *   Line 4: Coordinates (Lat 18.518084° Long 73.835936°)
     *   Line 5: Date, Time & Timezone (Monday, 19/10/2026 12:18 PM GMT +05:30)
     *   Line 6: Note : Captured by GPS Map Camera
     * - Top-Right Badge: GPS Map Camera badge floating atop the text card with official camera logo.
     */
    private fun drawClassicPreset(
        canvas: Canvas,
        width: Float,
        height: Float,
        data: GpsPhotoData,
        mapThumbnail: Bitmap?
    ) {
        val baseScale = (width / 1024f) * data.scale

        val mapSize = 198f * baseScale
        val gap = 17f * baseScale
        val textCardWidth = 596f * baseScale
        val cardHeight = mapSize // 198f * baseScale (Exact height alignment with map thumbnail)
        val totalBlockWidth = mapSize + gap + textCardWidth

        // Responsive positioning: center horizontally and anchor near bottom
        val blockLeft = ((width - totalBlockWidth) / 2f).coerceAtLeast(8f * baseScale)
        val blockBottom = height - (16f * baseScale)
        val blockTop = blockBottom - cardHeight

        val mapLeft = blockLeft
        val mapTop = blockTop
        val mapRight = mapLeft + mapSize
        val mapBottom = blockBottom

        val cardLeft = mapRight + gap
        val cardTop = blockTop
        val cardRight = cardLeft + textCardWidth
        val cardBottom = blockBottom

        val badgeWidth = 158f * baseScale
        val badgeHeight = 34f * baseScale
        val badgeRight = cardRight
        val badgeBottom = cardTop + (1.5f * baseScale) // Slight overlap for seamless union
        val badgeLeft = badgeRight - badgeWidth
        val badgeTop = badgeBottom - badgeHeight

        // 1. Draw Top-Right "GPS Map Camera" Badge with Camera Logo Icon
        drawGpsCameraBadge(canvas, badgeLeft, badgeTop, badgeRight, badgeBottom, baseScale, data.fontSizeMultiplier)

        // 2. Draw Left Satellite Map Thumbnail (with Red Pin, Blue Heading Cone, and Google Watermark)
        drawMapThumbnailCard(canvas, mapLeft, mapTop, mapRight, mapBottom, baseScale, mapThumbnail)

        // 3. Draw Right Text Card with Exact Typography and All 6 Lines
        drawClassicTextCard(canvas, cardLeft, cardTop, cardRight, cardBottom, baseScale, data)
    }

    /**
     * Draws the official GPS Map Camera badge on the top-right of the text card.
     */
    private fun drawGpsCameraBadge(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        scale: Float,
        fontSizeMultiplier: Float
    ) {
        val cornerRadius = 9f * scale
        val rect = RectF(left, top, right, bottom)

        // Dark translucent background (#26282B, alpha 0.88)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(225, 34, 36, 40)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        // Official GPS Map Camera App Logo Icon
        val iconSize = 22f * scale
        val iconLeft = left + (8f * scale)
        val iconTop = top + ((bottom - top - iconSize) / 2f)
        drawBadgeAppLogo(canvas, iconLeft, iconTop, iconSize)

        // "GPS Map Camera" text in crisp white bold
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 12f * scale * fontSizeMultiplier
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val fontMetrics = textPaint.fontMetrics
        val textY = top + ((bottom - top) / 2f) - ((fontMetrics.ascent + fontMetrics.descent) / 2f)
        canvas.drawText("GPS Map Camera", iconLeft + iconSize + (7f * scale), textY, textPaint)
    }

    /**
     * Draws the GPS Map Camera logo icon:
     * Sky blue rounded squircle, lime/gold globe ring, dark camera lens, and white specular glare.
     */
    private fun drawBadgeAppLogo(canvas: Canvas, left: Float, top: Float, size: Float) {
        val squircleRect = RectF(left, top, left + size, top + size)
        val radius = size * 0.22f

        // 1. Sky blue rounded background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(2, 136, 209)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(squircleRect, radius, radius, bgPaint)

        val cx = left + (size / 2f)
        val cy = top + (size / 2f)

        // 2. Earth lime green outer ring
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(139, 195, 74)
            style = Paint.Style.STROKE
            strokeWidth = size * 0.14f
        }
        canvas.drawCircle(cx, cy, size * 0.32f, ringPaint)

        // 3. Dark camera lens center
        val lensPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(24, 32, 44)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, size * 0.24f, lensPaint)

        // 4. White highlight reflection glint
        val glintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 255, 255, 255)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx - (size * 0.08f), cy - (size * 0.08f), size * 0.06f, glintPaint)
    }

    /**
     * Draws the standalone satellite map thumbnail card on the left:
     * - Clipped to 16px corner radius
     * - Translucent blue heading radar cone radiating North-East
     * - Red location pin marker
     * - White "Google" script watermark at the bottom
     */
    private fun drawMapThumbnailCard(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        scale: Float,
        mapThumbnail: Bitmap?
    ) {
        val cornerRadius = 16f * scale
        val mapRect = RectF(left, top, right, bottom)
        val width = right - left
        val height = bottom - top

        canvas.save()
        val clipPath = Path().apply {
            addRoundRect(mapRect, cornerRadius, cornerRadius, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)

        // 1. Draw Satellite Map
        if (mapThumbnail != null && !mapThumbnail.isRecycled) {
            canvas.drawBitmap(mapThumbnail, null, mapRect, Paint(Paint.FILTER_BITMAP_FLAG))
        } else {
            val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 48, 40)
            }
            canvas.drawRect(mapRect, fallbackPaint)
        }

        val cx = left + (width / 2f)
        val cy = top + (height / 2f)

        // 2. Draw Translucent Blue Heading Cone / Radar Beam (pointing North-East at -45°)
        val beamRadius = width * 0.42f
        val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(85, 30, 136, 229)
            style = Paint.Style.FILL
        }
        val beamPath = Path().apply {
            moveTo(cx, cy)
            arcTo(
                RectF(cx - beamRadius, cy - beamRadius, cx + beamRadius, cy + beamRadius),
                -68f,
                46f
            )
            close()
        }
        canvas.drawPath(beamPath, beamPaint)

        // 3. Draw Classic Red Location Pin
        val pinHeight = height * 0.175f
        drawClassicRedPin(canvas, cx, cy, pinHeight)

        // 4. Draw White "Google" Watermark at Bottom-Left
        drawGoogleWatermark(canvas, left + (14f * scale), bottom - (12f * scale), 16f * scale)

        canvas.restore()
    }

    /**
     * Draws the classic teardrop red location pin with center dot and soft shadow.
     */
    private fun drawClassicRedPin(canvas: Canvas, cx: Float, cy: Float, pinHeight: Float) {
        val pinWidth = pinHeight * 0.68f
        val headRadius = pinWidth / 2f
        val headCy = cy - pinHeight + headRadius

        // Soft shadow under tip
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawOval(
            cx - (pinWidth * 0.35f), cy - (pinHeight * 0.05f),
            cx + (pinWidth * 0.35f), cy + (pinHeight * 0.12f),
            shadowPaint
        )

        // Teardrop pin body (Google Red #E53935)
        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(229, 57, 53)
            style = Paint.Style.FILL
        }

        val pinPath = Path().apply {
            moveTo(cx, cy)
            cubicTo(
                cx - (pinWidth * 0.55f), headCy + (headRadius * 0.7f),
                cx - headRadius, headCy + (headRadius * 0.2f),
                cx - headRadius, headCy
            )
            arcTo(
                RectF(cx - headRadius, headCy - headRadius, cx + headRadius, headCy + headRadius),
                180f, 180f
            )
            cubicTo(
                cx + headRadius, headCy + (headRadius * 0.2f),
                cx + (pinWidth * 0.55f), headCy + (headRadius * 0.7f),
                cx, cy
            )
            close()
        }
        canvas.drawPath(pinPath, pinPaint)

        // White center circle dot
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, headCy, headRadius * 0.45f, dotPaint)
    }

    /**
     * Draws the white Google script watermark with drop shadow on the satellite map thumbnail.
     */
    private fun drawGoogleWatermark(canvas: Canvas, x: Float, y: Float, textSize: Float) {
        val text = "Google"

        // Shadow
        val shadowPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(140, 0, 0, 0)
            this.textSize = textSize
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            isFakeBoldText = true
        }
        canvas.drawText(text, x + 1.2f, y + 1.2f, shadowPaint)

        // White Text
        val whitePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.textSize = textSize
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            isFakeBoldText = true
        }
        canvas.drawText(text, x, y, whitePaint)
    }

    /**
     * Draws the dark translucent text card on the right with all 6 lines of pure white text.
     */
    private fun drawClassicTextCard(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        scale: Float,
        data: GpsPhotoData
    ) {
        val cornerRadius = 16f * scale
        val cardRect = RectF(left, top, right, bottom)

        // Background dark translucent (#202225, alpha 0.78)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = data.backgroundColorHex.toInt()
            alpha = (data.backgroundOpacity * 255).roundToInt().coerceIn(0, 255)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

        val textLeft = left + (16f * scale)
        val maxTextWidth = (right - left) - (32f * scale)

        // Title Paint: Bold Sans-Serif, 25sp
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 25f * scale * data.fontSizeMultiplier
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Body Paint: Regular Sans-Serif, 15.5sp
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15.5f * scale * data.fontSizeMultiplier
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // 1. Line 1: Heading with Flag (e.g. "Pune, Maharashtra, India 🇮🇳")
        val headingText = data.headingWithFlag
        canvas.drawText(headingText, textLeft, top + (35f * scale), titlePaint)

        // 2. Lines 2 & 3: Postal Address split into 2 lines
        val addressLines = wrapTextToLines(data.postalAddress, bodyPaint, maxTextWidth, maxLines = 2)
        val addrLine1 = addressLines.getOrNull(0) ?: ""
        val addrLine2 = addressLines.getOrNull(1) ?: ""
        if (addrLine1.isNotBlank()) {
            canvas.drawText(addrLine1, textLeft, top + (60f * scale), bodyPaint)
        }
        if (addrLine2.isNotBlank()) {
            canvas.drawText(addrLine2, textLeft, top + (84f * scale), bodyPaint)
        }

        // 3. Line 4: Coordinates (e.g. "Lat 18.518084° Long 73.835936°")
        val coordsText = data.formattedCoordinatesExact()
        canvas.drawText(coordsText, textLeft, top + (108f * scale), bodyPaint)

        // 4. Line 5: Timestamp & Timezone (e.g. "Monday, 19/10/2026 12:18 PM GMT +05:30")
        val timeText = data.formattedTimestampWithZone()
        canvas.drawText(timeText, textLeft, top + (132f * scale), bodyPaint)

        // 5. Line 6: Custom Note (e.g. "Note : Captured by GPS Map Camera")
        val noteText = "Note : " + data.customNote.ifBlank { "Captured by GPS Map Camera" }
        canvas.drawText(noteText, textLeft, top + (156f * scale), bodyPaint)
    }

    /**
     * Splits a text string across up to [maxLines] lines respecting word boundaries and comma splits.
     */
    private fun wrapTextToLines(text: String, paint: TextPaint, maxWidth: Float, maxLines: Int): List<String> {
        if (text.isBlank()) return emptyList()
        if (paint.measureText(text) <= maxWidth) return listOf(text)

        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    if (lines.size == maxLines - 1) {
                        val remainingIndex = words.indexOf(word)
                        val rest = words.subList(remainingIndex, words.size).joinToString(" ")
                        lines.add(rest)
                        return lines
                    }
                    currentLine = StringBuilder(word)
                } else {
                    lines.add(word)
                }
            }
        }
        if (currentLine.isNotEmpty() && lines.size < maxLines) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    /**
     * Compact layout banner.
     */
    private fun drawCompactPreset(
        canvas: Canvas,
        width: Float,
        height: Float,
        data: GpsPhotoData,
        mapThumbnail: Bitmap?
    ) {
        val scale = max(0.6f, width / 1080f)
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

        canvas.drawText(data.headingWithFlag, textLeft, top + padding + (20f * scale), titlePaint)
        canvas.drawText(data.formattedCoordinatesExact(), textLeft, top + padding + (44f * scale), subPaint)
        canvas.drawText(data.formattedTimestampWithZone(), textLeft, top + padding + (66f * scale), datePaint)
    }

    /**
     * Modern Card layout with elevated landscape map and clean metadata grid.
     */
    private fun drawModernCardPreset(
        canvas: Canvas,
        width: Float,
        height: Float,
        data: GpsPhotoData,
        mapThumbnail: Bitmap?
    ) {
        val scale = max(0.6f, width / 1080f)
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
        canvas.drawText(data.headingWithFlag, left + padding, currentY, titlePaint)
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
        canvas.drawText(data.formattedCoordinatesExact(), left + padding, currentY + (34f * scale), coordPaint)
        canvas.drawText(data.formattedTimestampWithZone(), left + padding, currentY + (54f * scale), textPaint)
    }

    /**
     * Minimal Watermark preset pill.
     */
    private fun drawMinimalWatermarkPreset(
        canvas: Canvas,
        width: Float,
        height: Float,
        data: GpsPhotoData
    ) {
        val scale = max(0.6f, width / 1080f)
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
        canvas.drawText("${data.headingWithFlag} • ${data.formattedCoordinatesExact()}", midX, top + (28f * scale), titlePaint)
        canvas.drawText("${data.formattedTimestampWithZone()} • Alt ${data.altitudeMeters.toInt()}m", midX, top + (52f * scale), subPaint)
    }
}
