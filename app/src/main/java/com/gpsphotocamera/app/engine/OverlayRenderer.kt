package com.gpsphotocamera.app.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint

/**
 * High-Precision GPS Photograph Overlay Rendering Engine.
 *
 * Implements pixel-accurate Canvas and Paint rendering directly onto Android Bitmaps.
 * Faithfully reproduces the visual appearance, typography, transparency, and layout
 * of the GPS Map Camera reference photograph:
 * - 3 independent visual components:
 *   A. Satellite map thumbnail on the bottom left.
 *   B. Translucent black information panel on the bottom right (64-67% width on portrait).
 *   C. Small GPS Map Camera branding badge sitting atop the information panel.
 * - Bold white heading, regular white address/coords/date/note.
 * - Resolution-independent adaptive scaling for Portrait, Landscape, and Square photographs.
 */
object OverlayRenderer {

    /**
     * Renders the complete GPS Map Camera overlay onto [sourceBitmap] at full native resolution.
     *
     * @param sourceBitmap Original photograph bitmap (EXIF orientation corrected).
     * @param config The full overlay configuration model.
     * @return Output bitmap containing the composite photograph with GPS overlay.
     */
    fun renderOverlay(sourceBitmap: Bitmap, config: OverlayConfig): Bitmap {
        val width = sourceBitmap.width
        val height = sourceBitmap.height

        val outputBitmap = try {
            sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        } catch (oom: OutOfMemoryError) {
            System.gc()
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }

        val canvas = Canvas(outputBitmap)
        if (outputBitmap !== sourceBitmap) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(sourceBitmap, 0f, 0f, bgPaint)
        }

        // 1. Compute resolution-independent layout geometry
        val layout = config.computeLayout(width, height)

        // 2. Draw Top-Right Branding Badge (sits atop the right of the information panel)
        if (config.badge.visible) {
            drawBrandingBadge(
                canvas = canvas,
                badgeRect = layout.badgeRect,
                cornerRadius = layout.badgeCornerRadius,
                badgeConfig = config.badge,
                typography = config.typography,
                textSize = layout.badgeTextSize
            )
        }

        // 3. Draw Translucent Black Information Panel Background
        drawInformationPanel(
            canvas = canvas,
            panelRect = layout.panelRect,
            cornerRadius = layout.cornerRadius,
            panelConfig = config.panel
        )

        // 4. Draw Satellite Map Thumbnail
        if (config.map.enabled) {
            drawMapThumbnail(
                canvas = canvas,
                mapRect = layout.mapRect,
                cornerRadius = layout.cornerRadius,
                mapConfig = config.map,
                location = config.location,
                scale = layout.cornerRadius / 16f
            )
        }

        // 5. Draw Information Panel Content (Heading, Address, Coordinates, Date/Time, Note)
        val textLeft = layout.panelRect.left + layout.paddingLeft
        val availableWidth = layout.panelRect.width() - layout.paddingLeft - layout.paddingRight
        val cardHeight = layout.panelRect.height()
        val cardTop = layout.panelRect.top

        // Location Heading (Bold, white, larger font, national flag emoji)
        val headingBaseline = cardTop + (cardHeight * (49f / 198f))
        drawLocationHeading(
            canvas = canvas,
            x = textLeft,
            baselineY = headingBaseline,
            availableWidth = availableWidth,
            location = config.location,
            typography = config.typography,
            textSize = layout.headingTextSize
        )

        // Address (Regular white, multiline wrapped)
        val addrStartBaseline = cardTop + (cardHeight * (77f / 198f))
        val lastAddrBaseline = drawAddress(
            canvas = canvas,
            x = textLeft,
            baselineY = addrStartBaseline,
            linePitch = layout.linePitch,
            availableWidth = availableWidth,
            location = config.location,
            typography = config.typography,
            textSize = layout.bodyTextSize
        )

        // Coordinates (Regular white, e.g. "Lat 18.518084° Long 73.835936°")
        val coordsBaseline = if (lastAddrBaseline > addrStartBaseline) {
            cardTop + (cardHeight * (125f / 198f))
        } else {
            addrStartBaseline + layout.linePitch
        }
        drawCoordinates(
            canvas = canvas,
            x = textLeft,
            baselineY = coordsBaseline,
            availableWidth = availableWidth,
            location = config.location,
            typography = config.typography,
            textSize = layout.bodyTextSize
        )

        // Date and Time (Regular white, e.g. "Monday, 19/10/2026 12:18 PM GMT +05:30")
        val dateTimeBaseline = coordsBaseline + layout.linePitch
        drawDateTime(
            canvas = canvas,
            x = textLeft,
            baselineY = dateTimeBaseline,
            availableWidth = availableWidth,
            dateTime = config.dateTime,
            typography = config.typography,
            textSize = layout.bodyTextSize
        )

        // Custom Note (Regular white, e.g. "Note : Captured by GPS Map Camera")
        if (config.note.visible) {
            val noteBaseline = dateTimeBaseline + layout.linePitch
            drawNote(
                canvas = canvas,
                x = textLeft,
                baselineY = noteBaseline,
                availableWidth = availableWidth,
                note = config.note,
                typography = config.typography,
                textSize = layout.bodyTextSize
            )
        }

        return outputBitmap
    }

    /**
     * Draws the standalone satellite map thumbnail component:
     * - Square/rectangular thumbnail with rounded corners.
     * - Satellite/aerial imagery.
     * - Translucent blue heading radar cone pointing North-East (-45°).
     * - Red location pin with soft shadow and white center dot.
     * - White "Google" script watermark at bottom-left.
     */
    fun drawMapThumbnail(
        canvas: Canvas,
        mapRect: RectF,
        cornerRadius: Float,
        mapConfig: MapConfig,
        location: LocationData,
        scale: Float
    ) {
        val path = Path().apply {
            addRoundRect(mapRect, cornerRadius, cornerRadius, Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(path)

        // 1. Draw Satellite Imagery
        if (mapConfig.satelliteBitmap != null && !mapConfig.satelliteBitmap.isRecycled) {
            drawCenterCroppedBitmap(canvas, mapConfig.satelliteBitmap, mapRect)
        } else {
            drawProceduralSatelliteTerrain(canvas, mapRect)
        }

        val cx = mapRect.centerX()
        val cy = mapRect.centerY()
        val w = mapRect.width()
        val h = mapRect.height()

        // 2. Draw Translucent Blue Heading Radar Cone (pointing North-East at -45°)
        if (mapConfig.showRadarBeam) {
            val beamRadius = w * 0.44f
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
        }

        // 3. Draw Red Location Pin Marker
        if (mapConfig.showMarker) {
            val pinHeight = h * 0.18f
            drawRedPin(canvas, cx, cy, pinHeight)
        }

        // 4. Draw Google Watermark (Bottom-Left)
        if (mapConfig.showGoogleLogo) {
            val watermarkSize = 16f * (w / 198f)
            val wmX = mapRect.left + (14f * (w / 198f))
            val wmY = mapRect.bottom - (12f * (h / 198f))
            drawGoogleWatermark(canvas, wmX, wmY, watermarkSize)
        }

        canvas.restore()
    }

    /**
     * Draws the main translucent rounded rectangle panel background.
     */
    fun drawInformationPanel(
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
     * Draws the location heading (Bold, white, larger font, single line with flag emoji).
     */
    fun drawLocationHeading(
        canvas: Canvas,
        x: Float,
        baselineY: Float,
        availableWidth: Float,
        location: LocationData,
        typography: TypographyConfig,
        textSize: Float
    ) {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = typography.headingColor
            this.textSize = textSize
            typeface = Typeface.create(typography.headingFontFamily, typography.headingFontWeight)
        }
        val headingText = location.displayHeading
        val safeText = ellipsizeText(headingText, textPaint, availableWidth)
        canvas.drawText(safeText, x, baselineY, textPaint)
    }

    /**
     * Draws the postal address with multiline text wrapping (Regular white).
     * Returns the baseline Y of the last drawn address line.
     */
    fun drawAddress(
        canvas: Canvas,
        x: Float,
        baselineY: Float,
        linePitch: Float,
        availableWidth: Float,
        location: LocationData,
        typography: TypographyConfig,
        textSize: Float
    ): Float {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = typography.bodyColor
            this.textSize = textSize
            typeface = Typeface.create(typography.bodyFontFamily, typography.bodyFontWeight)
        }

        val rawAddress = if (location.addressLines.isNotEmpty()) {
            location.addressLines.joinToString(", ")
        } else {
            location.postalAddress
        }

        val lines = wrapTextToLines(rawAddress, textPaint, availableWidth, maxLines = 2)
        var currentBaseline = baselineY
        for (i in lines.indices) {
            canvas.drawText(lines[i], x, currentBaseline, textPaint)
            if (i < lines.size - 1) {
                currentBaseline += linePitch
            }
        }
        return currentBaseline
    }

    /**
     * Draws GPS coordinates (Regular white, e.g. "Lat 18.518084° Long 73.835936°").
     */
    fun drawCoordinates(
        canvas: Canvas,
        x: Float,
        baselineY: Float,
        availableWidth: Float,
        location: LocationData,
        typography: TypographyConfig,
        textSize: Float
    ) {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = typography.bodyColor
            this.textSize = textSize
            typeface = Typeface.create(typography.bodyFontFamily, typography.bodyFontWeight)
        }
        val coordsText = ellipsizeText(location.formattedCoordinates(), textPaint, availableWidth)
        canvas.drawText(coordsText, x, baselineY, textPaint)
    }

    /**
     * Draws Date and Time (Regular white, e.g. "Monday, 19/10/2026 12:18 PM GMT +05:30").
     */
    fun drawDateTime(
        canvas: Canvas,
        x: Float,
        baselineY: Float,
        availableWidth: Float,
        dateTime: DateTimeConfig,
        typography: TypographyConfig,
        textSize: Float
    ) {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = typography.bodyColor
            this.textSize = textSize
            typeface = Typeface.create(typography.bodyFontFamily, typography.bodyFontWeight)
        }
        val dateText = ellipsizeText(dateTime.formattedString(), textPaint, availableWidth)
        canvas.drawText(dateText, x, baselineY, textPaint)
    }

    /**
     * Draws custom note (Regular white, e.g. "Note : Captured by GPS Map Camera").
     */
    fun drawNote(
        canvas: Canvas,
        x: Float,
        baselineY: Float,
        availableWidth: Float,
        note: NoteConfig,
        typography: TypographyConfig,
        textSize: Float
    ) {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = typography.bodyColor
            this.textSize = textSize
            typeface = Typeface.create(typography.bodyFontFamily, typography.bodyFontWeight)
        }
        val noteText = ellipsizeText(note.formattedString(), textPaint, availableWidth)
        canvas.drawText(noteText, x, baselineY, textPaint)
    }

    /**
     * Draws the small GPS Map Camera branding badge above the right side of the information panel.
     */
    fun drawBrandingBadge(
        canvas: Canvas,
        badgeRect: RectF,
        cornerRadius: Float,
        badgeConfig: BadgeConfig,
        typography: TypographyConfig,
        textSize: Float
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeConfig.backgroundColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(badgeRect, cornerRadius, cornerRadius, bgPaint)

        // Draw GPS Map Camera App Logo
        val iconSize = badgeRect.height() * 0.65f
        val iconLeft = badgeRect.left + (badgeRect.height() * 0.24f)
        val iconTop = badgeRect.centerY() - (iconSize / 2f)
        drawBadgeAppLogo(canvas, iconLeft, iconTop, iconSize)

        // Draw "GPS Map Camera" text in bold white
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeConfig.textColor
            this.textSize = textSize
            typeface = Typeface.create(typography.bodyFontFamily, Typeface.BOLD)
        }
        val fontMetrics = textPaint.fontMetrics
        val textBaselineY = badgeRect.centerY() - ((fontMetrics.ascent + fontMetrics.descent) / 2f)
        val textStartX = iconLeft + iconSize + (badgeRect.height() * 0.22f)
        canvas.drawText(badgeConfig.text, textStartX, textBaselineY, textPaint)
    }

    /**
     * Draws the authentic GPS Map Camera squircle app icon.
     */
    private fun drawBadgeAppLogo(canvas: Canvas, left: Float, top: Float, size: Float) {
        val squircleRect = RectF(left, top, left + size, top + size)
        val radius = size * 0.22f

        // 1. Sky blue squircle base
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(2, 136, 209)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(squircleRect, radius, radius, bgPaint)

        val cx = left + (size / 2f)
        val cy = top + (size / 2f)

        // 2. Earth lime green ring
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

        // 4. White reflection glint
        val glintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 255, 255, 255)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx - (size * 0.08f), cy - (size * 0.08f), size * 0.06f, glintPaint)
    }

    /**
     * Draws the teardrop Google red pin marker with soft shadow and white center dot.
     */
    private fun drawRedPin(canvas: Canvas, cx: Float, cy: Float, pinHeight: Float) {
        val pinWidth = pinHeight * 0.68f
        val headRadius = pinWidth / 2f
        val headCy = cy - pinHeight + headRadius

        // Soft drop shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawOval(
            cx - (pinWidth * 0.35f), cy - (pinHeight * 0.05f),
            cx + (pinWidth * 0.35f), cy + (pinHeight * 0.12f),
            shadowPaint
        )

        // Teardrop body (Google Red #E53935)
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

        // Center white circle dot
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, headCy, headRadius * 0.45f, dotPaint)
    }

    /**
     * Draws the white Google script watermark with drop shadow.
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
     * Center-crops a bitmap to fill [destinationRect] without distortion.
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
     * Realistic satellite terrain texture synthesized procedurally if bitmap is null.
     */
    private fun drawProceduralSatelliteTerrain(canvas: Canvas, mapRect: RectF) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(45, 55, 42) // Foliage / earth
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

        val roadPath = Path().apply {
            moveTo(l, t + h * 0.42f)
            cubicTo(l + w * 0.35f, t + h * 0.45f, l + w * 0.65f, t + h * 0.38f, l + w, t + h * 0.65f)
        }
        canvas.drawPath(roadPath, roadPaint)
        canvas.drawLine(l + w * 0.52f, t, l + w * 0.48f, t + h, roadPaint)

        val roofPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val buildingColors = intArrayOf(
            Color.rgb(180, 160, 140),
            Color.rgb(150, 145, 140),
            Color.rgb(120, 115, 110),
            Color.rgb(195, 185, 175)
        )

        val cols = 5
        val rows = 5
        val cellW = w / cols
        val cellH = h / rows

        for (i in 0 until cols) {
            for (j in 0 until rows) {
                if ((i == 2 && j == 2) || (i == 2 && j == 1)) continue
                val bLeft = l + i * cellW + cellW * 0.15f
                val bTop = t + j * cellH + cellH * 0.15f
                val bRight = bLeft + cellW * 0.70f
                val bBottom = bTop + cellH * 0.70f
                roofPaint.color = buildingColors[(i * 3 + j) % buildingColors.size]
                canvas.drawRect(bLeft, bTop, bRight, bBottom, roofPaint)
            }
        }
    }

    /**
     * Splits a text string across up to [maxLines] lines respecting word boundaries.
     */
    private fun wrapTextToLines(text: String, paint: TextPaint, maxWidth: Float, maxLines: Int): List<String> {
        if (text.isBlank()) return emptyList()
        if (paint.measureText(text) <= maxWidth) return listOf(text)

        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (i in words.indices) {
            val word = words[i]
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    if (lines.size == maxLines - 1) {
                        val rest = words.subList(i, words.size).joinToString(" ")
                        val truncated = ellipsizeText(rest, paint, maxWidth)
                        lines.add(truncated)
                        return lines
                    }
                    currentLine = StringBuilder(word)
                } else {
                    lines.add(ellipsizeText(word, paint, maxWidth))
                }
            }
        }
        if (currentLine.isNotEmpty() && lines.size < maxLines) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    /**
     * Ellipsizes single line text with '…' if it exceeds [maxWidth].
     */
    private fun ellipsizeText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        val ellipsisWidth = paint.measureText(ellipsis)
        val availableWidth = maxWidth - ellipsisWidth

        var end = text.length
        while (end > 0 && paint.measureText(text, 0, end) > availableWidth) {
            end--
        }
        return text.substring(0, end).trimEnd() + ellipsis
    }
}
