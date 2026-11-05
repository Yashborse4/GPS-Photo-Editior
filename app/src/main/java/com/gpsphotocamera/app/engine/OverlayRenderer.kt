package com.gpsphotocamera.app.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.min

/**
 * High-Precision GPS Photograph Overlay Rendering Engine.
 *
 * Implements the full 14-step canvas rendering pipeline directly onto Android Bitmaps
 * without WebViews or HTML rendering. Replicates the exact proportions, typography,
 * transparency, and layout of the reference GPS Map Camera photograph.
 */
object OverlayRenderer {

    /**
     * Renders the complete GPS Map Camera overlay onto a mutable copy of [sourceBitmap].
     *
     * @param sourceBitmap Original photograph bitmap (EXIF orientation corrected).
     * @param config The full overlay configuration model.
     * @return Output bitmap containing the composite photograph with GPS overlay.
     */
    fun renderOverlay(sourceBitmap: Bitmap, config: OverlayConfig): Bitmap {
        val width = sourceBitmap.width
        val height = sourceBitmap.height

        // 1. Create mutable output Bitmap matching original dimensions exactly
        val outputBitmap = try {
            sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        } catch (oom: OutOfMemoryError) {
            System.gc()
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }

        // 2. Initialize Canvas
        val canvas = Canvas(outputBitmap)

        // 3. Draw original photograph if we had to create a fresh bitmap
        if (outputBitmap !== sourceBitmap) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(sourceBitmap, 0f, 0f, bgPaint)
        }

        // 4. Calculate dynamic scale factor based on reference width (1086 px)
        val baseWidth = 1086f
        val baseHeight = 1448f
        val scale = if (width <= height) {
            width.toFloat() / baseWidth
        } else {
            // In landscape, prevent overlay from dominating vertical viewport
            min(width.toFloat() / baseWidth, (height.toFloat() * 1.33f) / baseHeight)
        }

        // 5. Calculate component bounds based on calibrated reference proportions
        val p = config.panel
        val m = config.map
        val b = config.badge

        val hMargin = width * p.horizontalMarginPercent
        val bMargin = height * p.bottomMarginPercent
        val gap = width * p.gapBetweenMapAndPanelPercent

        val mapWidth = width * m.widthPercent
        val mapHeight = width * m.heightPercent
        val panelWidth = width * p.panelWidthPercent
        val panelHeight = mapHeight // Panel height matches map height for balanced row

        val cornerRadius = width * p.cornerRadiusPercent

        // Vertical panel placement
        val (mapTop, mapBottom) = when (p.position) {
            PanelPosition.BOTTOM -> {
                val bottom = height - bMargin
                val top = bottom - mapHeight
                Pair(top, bottom)
            }
            PanelPosition.TOP -> {
                val top = height * p.topMarginPercent
                val bottom = top + mapHeight
                Pair(top, bottom)
            }
            PanelPosition.CUSTOM -> {
                val centerY = height * p.customCenterYPercent
                val top = centerY - (mapHeight / 2f)
                val bottom = top + mapHeight
                Pair(top, bottom)
            }
        }

        val mapLeft = hMargin
        val mapRight = mapLeft + mapWidth
        val mapRect = RectF(mapLeft, mapTop, mapRight, mapBottom)

        val panelLeft = mapRight + gap
        val panelRight = panelLeft + panelWidth
        val panelTop = mapTop
        val panelBottom = mapBottom
        val panelRect = RectF(panelLeft, panelTop, panelRight, panelBottom)

        // 6. Draw Top-Right Badge (sits atop the main panel flush right)
        if (b.visible) {
            val badgeWidth = width * b.widthPercent
            val badgeHeight = width * b.heightPercent
            val badgeRight = panelRight
            val badgeLeft = badgeRight - badgeWidth
            val badgeBottom = panelTop
            val badgeTop = badgeBottom - badgeHeight
            val badgeRect = RectF(badgeLeft, badgeTop, badgeRight, badgeBottom)

            drawBadge(
                canvas = canvas,
                badgeRect = badgeRect,
                cornerRadius = cornerRadius,
                badgeConfig = b,
                typography = config.typography,
                scale = scale
            )
        }

        // 7. Draw Translucent Rounded Rectangle Information Panel
        drawInformationPanel(
            canvas = canvas,
            panelRect = panelRect,
            cornerRadius = cornerRadius,
            panelConfig = p
        )

        // 8. Draw Satellite Map Thumbnail
        if (m.enabled) {
            drawSatelliteMap(
                canvas = canvas,
                mapRect = mapRect,
                cornerRadius = cornerRadius,
                mapConfig = m,
                location = config.location,
                scale = scale
            )
        }

        // 9. Draw Information Panel Content (Heading, Address, Coordinates, Date/Time, Note)
        drawPanelContent(
            canvas = canvas,
            panelRect = panelRect,
            config = config,
            scale = scale
        )

        return outputBitmap
    }

    /**
     * Draws the main translucent rounded rectangle panel background.
     */
    private fun drawInformationPanel(
        canvas: Canvas,
        panelRect: RectF,
        cornerRadius: Float,
        panelConfig: PanelConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = panelConfig.backgroundColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(panelRect, cornerRadius, cornerRadius, paint)
    }

    /**
     * Draws the satellite map thumbnail, pin marker, and Google watermark.
     */
    private fun drawSatelliteMap(
        canvas: Canvas,
        mapRect: RectF,
        cornerRadius: Float,
        mapConfig: MapConfig,
        location: LocationData,
        scale: Float
    ) {
        MapThumbnailRenderer.drawMapThumbnail(
            canvas = canvas,
            mapRect = mapRect,
            cornerRadius = cornerRadius,
            config = mapConfig,
            location = location,
            scale = scale
        )
    }

    /**
     * Draws the top-right badge ("GPS Map Camera") sitting atop the main panel.
     */
    private fun drawBadge(
        canvas: Canvas,
        badgeRect: RectF,
        cornerRadius: Float,
        badgeConfig: BadgeConfig,
        typography: TypographyConfig,
        scale: Float
    ) {
        val path = Path().apply {
            val radii = floatArrayOf(
                cornerRadius, cornerRadius, // Top-left
                cornerRadius, cornerRadius, // Top-right
                0f, 0f,                     // Bottom-right
                0f, 0f                      // Bottom-left
            )
            addRoundRect(badgeRect, radii, Path.Direction.CW)
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeConfig.backgroundColor
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, bgPaint)

        // Draw camera aperture / lens vector icon
        val iconRadius = 8f * scale
        val iconCenterX = badgeRect.left + (18f * scale)
        val iconCenterY = badgeRect.centerY()

        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeConfig.textColor
            style = Paint.Style.STROKE
            strokeWidth = 2.2f * scale
        }
        canvas.drawCircle(iconCenterX, iconCenterY, iconRadius, iconPaint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeConfig.textColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(iconCenterX, iconCenterY, iconRadius * 0.42f, dotPaint)

        // Draw badge text
        val badgeTextSize = 20f * scale * typography.fontSizeMultiplier
        val textPaint = TextRenderer.createPaint(
            typeface = Typeface.create(typography.bodyFontFamily, Typeface.NORMAL),
            textSize = badgeTextSize,
            textColor = badgeConfig.textColor
        )

        val fontMetrics = textPaint.fontMetrics
        val textBaselineY = badgeRect.centerY() - (fontMetrics.descent + fontMetrics.ascent) / 2f
        val textStartX = iconCenterX + iconRadius + (10f * scale)

        canvas.drawText(badgeConfig.text, textStartX, textBaselineY, textPaint)
    }

    /**
     * Draws text content inside the information panel with exact hierarchy and line wrapping.
     */
    private fun drawPanelContent(
        canvas: Canvas,
        panelRect: RectF,
        config: OverlayConfig,
        scale: Float
    ) {
        val p = config.panel
        val t = config.typography
        val loc = config.location
        val dt = config.dateTime
        val note = config.note

        val paddingLeft = panelRect.width() * p.paddingLeftPercent
        val paddingRight = panelRect.width() * p.paddingRightPercent
        val paddingTop = panelRect.height() * p.paddingTopPercent

        val contentLeft = panelRect.left + paddingLeft
        val availableWidth = (panelRect.width() - paddingLeft - paddingRight).toInt()

        var currentY = panelRect.top + paddingTop

        // 1. Location Heading (Bold, white, larger font, native flag emoji)
        val headingSize = 46f * scale * t.fontSizeMultiplier
        val headingPaint = TextRenderer.createPaint(
            typeface = Typeface.create(t.headingFontFamily, t.headingFontWeight),
            textSize = headingSize,
            textColor = t.headingColor
        )

        val headingMetrics = headingPaint.fontMetrics
        val headingCapHeight = TextRenderer.measureCapHeight(headingPaint)
        val headingBaseline = currentY + headingCapHeight

        val fullHeading = loc.displayHeading
        val safeHeading = TextRenderer.ellipsizeText(fullHeading, headingPaint, availableWidth.toFloat())
        TextRenderer.drawSingleLine(canvas, safeHeading, contentLeft, headingBaseline, headingPaint)

        currentY = headingBaseline + headingMetrics.descent + (14f * scale)

        // 2. Postal Address (Multi-line StaticLayout wrapping, regular font)
        val bodySize = 29f * scale * t.fontSizeMultiplier
        val bodyPaint = TextRenderer.createPaint(
            typeface = Typeface.create(t.bodyFontFamily, t.bodyFontWeight),
            textSize = bodySize,
            textColor = t.bodyColor
        )

        val fullAddress = if (loc.addressLines.isNotEmpty()) {
            loc.addressLines.joinToString("\n")
        } else {
            ""
        }

        if (fullAddress.isNotBlank()) {
            val addressLayout = TextRenderer.buildStaticLayout(
                text = fullAddress,
                paint = bodyPaint,
                availableWidth = availableWidth,
                maxLines = 3,
                lineSpacingMultiplier = t.lineSpacingMultiplier
            )
            val addressHeight = TextRenderer.drawMultiLine(canvas, addressLayout, contentLeft, currentY)
            currentY += addressHeight + (10f * scale)
        }

        // 3. Coordinates (Lat 18.518667° Long 73.833756°)
        val bodyMetrics = bodyPaint.fontMetrics
        val coordsStr = loc.formattedCoordinates()
        val coordsBaseline = currentY - bodyMetrics.ascent
        TextRenderer.drawSingleLine(canvas, coordsStr, contentLeft, coordsBaseline, bodyPaint)
        currentY = coordsBaseline + bodyMetrics.descent + (6f * scale)

        // 4. Date and Time (Tuesday, 03/11/2026 02:45 PM GMT +05:30)
        val dateTimeStr = dt.formattedString()
        val dtBaseline = currentY - bodyMetrics.ascent
        TextRenderer.drawSingleLine(canvas, dateTimeStr, contentLeft, dtBaseline, bodyPaint)
        currentY = dtBaseline + bodyMetrics.descent + (6f * scale)

        // 5. Custom Note (Note : Captured by GPS Map Camera)
        if (note.visible) {
            val noteStr = note.formattedString()
            val noteBaseline = currentY - bodyMetrics.ascent
            TextRenderer.drawSingleLine(canvas, noteStr, contentLeft, noteBaseline, bodyPaint)
        }
    }
}
