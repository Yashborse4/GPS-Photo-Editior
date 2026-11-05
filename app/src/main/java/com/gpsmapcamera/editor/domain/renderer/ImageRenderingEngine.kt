package com.gpsmapcamera.editor.domain.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.gpsmapcamera.editor.data.model.FontFamilyOption
import com.gpsmapcamera.editor.data.model.GpsOverlayState
import com.gpsmapcamera.editor.data.model.LayoutPreset
import com.gpsmapcamera.editor.data.model.OverlayTextAlign
import com.gpsmapcamera.editor.domain.util.CoordinatesFormatter

object ImageRenderingEngine {

    /**
     * Renders the complete GPS Map Camera overlay onto the original full-resolution photograph.
     * Guaranteed resolution-independent scaling using normalized coordinates and scale factors.
     */
    fun renderFinalImage(
        baseImage: Bitmap,
        state: GpsOverlayState,
        mapTileBitmap: Bitmap? = null
    ): Bitmap {
        // Create a mutable copy of the original bitmap at its native full resolution
        val outputBitmap = baseImage.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)

        val imageWidth = outputBitmap.width.toFloat()
        val imageHeight = outputBitmap.height.toFloat()

        // Base scaling factor calibrated against 1080px baseline
        val scaleFactor = (imageWidth / 1080f).coerceAtLeast(0.8f)

        // Generate map tile at matched resolution if not provided
        val mapTile = mapTileBitmap ?: if (state.showMap) {
            val mapTileSize = (280f * scaleFactor * state.mapScale).toInt().coerceAtLeast(120)
            SatelliteMapRenderer.generateMapTile(
                latitude = state.latitude,
                longitude = state.longitude,
                mapType = state.mapType,
                aspectRatio = state.mapAspectRatio,
                zoom = state.mapZoom,
                heading = state.headingDegrees,
                showCompass = state.showCompass,
                showMarker = state.showMarker,
                widthPx = mapTileSize
            )
        } else null

        // Render overlay according to preset
        when (state.preset) {
            LayoutPreset.CLASSIC -> renderClassicOverlay(canvas, imageWidth, imageHeight, scaleFactor, state, mapTile)
            LayoutPreset.COMPACT -> renderCompactOverlay(canvas, imageWidth, imageHeight, scaleFactor, state, mapTile)
            LayoutPreset.LARGE_CARD -> renderLargeCardOverlay(canvas, imageWidth, imageHeight, scaleFactor, state, mapTile)
            LayoutPreset.MINIMAL -> renderMinimalOverlay(canvas, imageWidth, imageHeight, scaleFactor, state, mapTile)
        }

        return outputBitmap
    }

    private fun renderClassicOverlay(
        canvas: Canvas,
        imgW: Float,
        imgH: Float,
        scale: Float,
        state: GpsOverlayState,
        mapTile: Bitmap?
    ) {
        val panelWidth = (imgW * state.panelWidthPercent * state.scale).coerceAtMost(imgW * 0.98f)
        val padding = state.panelHeightPaddingDp * scale * 1.5f
        val cornerRadius = state.cornerRadiusDp * scale

        // Typeface selection
        val typeface = getTypeface(state.fontFamily, state.isBold)
        val textColor = state.textColorHex.toInt()
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = textColor
            this.typeface = typeface
        }

        // Calculate typography layout & sizing
        val titleSize = 22f * scale * state.fontSizeMultiplier
        val bodySize = 16f * scale * state.fontSizeMultiplier
        val metaSize = 14f * scale * state.fontSizeMultiplier
        val lineSpacing = state.lineSpacingDp * scale

        // Map dimensions
        val showMap = state.showMap && mapTile != null
        val mapWidth = if (showMap) (panelWidth * 0.28f * state.mapScale).coerceAtLeast(100f * scale) else 0f
        val mapHeight = if (showMap && mapTile != null) {
            mapWidth * (mapTile.height.toFloat() / mapTile.width.toFloat())
        } else 0f

        val textContentWidth = if (showMap) {
            panelWidth - mapWidth - (padding * 3f)
        } else {
            panelWidth - (padding * 2f)
        }.coerceAtLeast(100f)

        // Build text lines
        val titleText = state.locationTitle
        val addressText = state.streetAddress
        val coordsText = CoordinatesFormatter.format(state.latitude, state.longitude, state.coordinatesFormat)
        val altCompassText = "Altitude: ${String.format(java.util.Locale.US, "%.1f", state.altitudeMeters)}m   Heading: ${state.headingDegrees.toInt()}° ${state.cardinalDirection}"
        val dateTimeText = "${state.dayOfWeek}, ${state.customDate}   ${state.customTime}   ${state.customTimezone}"
        val noteText = if (state.showNote && state.note.isNotBlank()) "Note: ${state.note}" else null

        // Measure heights of text blocks using StaticLayout
        val titleLayout = createStaticLayout(titleText, textPaint, titleSize, textContentWidth.toInt(), state.textAlign, isBold = true)
        val addressLayout = createStaticLayout(addressText, textPaint, bodySize, textContentWidth.toInt(), state.textAlign)
        val coordsLayout = createStaticLayout(coordsText, textPaint, metaSize, textContentWidth.toInt(), state.textAlign)
        val altLayout = createStaticLayout(altCompassText, textPaint, metaSize, textContentWidth.toInt(), state.textAlign)
        val dateLayout = createStaticLayout(dateTimeText, textPaint, metaSize, textContentWidth.toInt(), state.textAlign)
        val noteLayout = noteText?.let { createStaticLayout(it, textPaint, metaSize, textContentWidth.toInt(), state.textAlign) }

        var textTotalHeight = titleLayout.height + addressLayout.height + coordsLayout.height + altLayout.height + dateLayout.height + (5 * lineSpacing)
        if (noteLayout != null) {
            textTotalHeight += noteLayout.height + lineSpacing
        }

        // Overall panel height is max of text height and map height plus vertical padding
        val panelContentHeight = Math.max(textTotalHeight, mapHeight)
        val panelHeight = panelContentHeight + (padding * 2f)

        // Compute top-left position from normalized coordinates (normX, normY)
        val centerX = imgW * state.normX
        val bottomY = imgH * state.normY
        val left = (centerX - (panelWidth / 2f)).coerceIn(8f * scale, imgW - panelWidth - 8f * scale)
        val top = (bottomY - panelHeight).coerceIn(8f * scale, imgH - panelHeight - 8f * scale)
        val panelRect = RectF(left, top, left + panelWidth, top + panelHeight)

        // Draw Panel Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = state.backgroundColorHex.toInt()
            alpha = (state.backgroundOpacity * 255).toInt().coerceIn(0, 255)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(panelRect, cornerRadius, cornerRadius, bgPaint)

        // Draw Map Thumbnail
        var textStartX = left + padding
        val contentTopY = top + padding

        if (showMap && mapTile != null) {
            val mapLeft = left + padding
            val mapTop = contentTopY + ((panelContentHeight - mapHeight) / 2f)
            val mapRect = RectF(mapLeft, mapTop, mapLeft + mapWidth, mapTop + mapHeight)

            // Map thumbnail rounded border / clipping
            canvas.save()
            val mapClipPath = android.graphics.Path().apply {
                addRoundRect(mapRect, 8f * scale, 8f * scale, android.graphics.Path.Direction.CW)
            }
            canvas.clipPath(mapClipPath)
            canvas.drawBitmap(mapTile, null, mapRect, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restore()

            // Map subtle border
            val mapBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(100, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 1.5f * scale
            }
            canvas.drawRoundRect(mapRect, 8f * scale, 8f * scale, mapBorderPaint)

            textStartX = mapLeft + mapWidth + padding
        }

        // Draw Text Elements
        canvas.save()
        canvas.translate(textStartX, contentTopY)

        // 1. Title
        titleLayout.draw(canvas)
        canvas.translate(0f, titleLayout.height + lineSpacing)

        // 2. Address
        addressLayout.draw(canvas)
        canvas.translate(0f, addressLayout.height + lineSpacing)

        // 3. Coordinates (accented)
        coordsLayout.draw(canvas)
        canvas.translate(0f, coordsLayout.height + lineSpacing)

        // 4. Altitude & Compass
        altLayout.draw(canvas)
        canvas.translate(0f, altLayout.height + lineSpacing)

        // 5. Date & Time
        dateLayout.draw(canvas)
        canvas.translate(0f, dateLayout.height + lineSpacing)

        // 6. Custom Note (if enabled)
        noteLayout?.draw(canvas)

        canvas.restore()
    }

    private fun renderCompactOverlay(
        canvas: Canvas,
        imgW: Float,
        imgH: Float,
        scale: Float,
        state: GpsOverlayState,
        mapTile: Bitmap?
    ) {
        val panelWidth = (imgW * state.panelWidthPercent * state.scale).coerceAtMost(imgW * 0.98f)
        val padding = state.panelHeightPaddingDp * scale * 1.1f
        val cornerRadius = state.cornerRadiusDp * scale

        val typeface = getTypeface(state.fontFamily, state.isBold)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = state.textColorHex.toInt()
            this.typeface = typeface
        }

        val titleSize = 18f * scale * state.fontSizeMultiplier
        val metaSize = 14f * scale * state.fontSizeMultiplier

        val showMap = state.showMap && mapTile != null
        val mapSize = if (showMap) 60f * scale * state.mapScale else 0f
        val textContentWidth = (panelWidth - mapSize - (padding * 3f)).toInt().coerceAtLeast(100)

        val line1Text = "${state.locationTitle} • ${state.customDate} ${state.customTime}"
        val line2Text = "${CoordinatesFormatter.format(state.latitude, state.longitude, state.coordinatesFormat)} • Alt: ${state.altitudeMeters.toInt()}m"

        val l1Layout = createStaticLayout(line1Text, textPaint, titleSize, textContentWidth, state.textAlign, isBold = true)
        val l2Layout = createStaticLayout(line2Text, textPaint, metaSize, textContentWidth, state.textAlign)

        val textHeight = l1Layout.height + l2Layout.height + (4f * scale)
        val panelHeight = Math.max(textHeight, mapSize) + (padding * 2f)

        val centerX = imgW * state.normX
        val bottomY = imgH * state.normY
        val left = (centerX - (panelWidth / 2f)).coerceIn(8f * scale, imgW - panelWidth - 8f * scale)
        val top = (bottomY - panelHeight).coerceIn(8f * scale, imgH - panelHeight - 8f * scale)
        val panelRect = RectF(left, top, left + panelWidth, top + panelHeight)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = state.backgroundColorHex.toInt()
            alpha = (state.backgroundOpacity * 255).toInt().coerceIn(0, 255)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(panelRect, cornerRadius, cornerRadius, bgPaint)

        var textStartX = left + padding
        if (showMap && mapTile != null) {
            val mapRect = RectF(left + padding, top + padding, left + padding + mapSize, top + padding + mapSize)
            canvas.save()
            val clipPath = android.graphics.Path().apply {
                addRoundRect(mapRect, 6f * scale, 6f * scale, android.graphics.Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            canvas.drawBitmap(mapTile, null, mapRect, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
            textStartX += mapSize + padding
        }

        canvas.save()
        canvas.translate(textStartX, top + padding)
        l1Layout.draw(canvas)
        canvas.translate(0f, l1Layout.height + (4f * scale))
        l2Layout.draw(canvas)
        canvas.restore()
    }

    private fun renderLargeCardOverlay(
        canvas: Canvas,
        imgW: Float,
        imgH: Float,
        scale: Float,
        state: GpsOverlayState,
        mapTile: Bitmap?
    ) {
        // Large card uses classic layout with elevated styling and accent line
        renderClassicOverlay(canvas, imgW, imgH, scale * 1.08f, state, mapTile)
    }

    private fun renderMinimalOverlay(
        canvas: Canvas,
        imgW: Float,
        imgH: Float,
        scale: Float,
        state: GpsOverlayState,
        mapTile: Bitmap?
    ) {
        val panelWidth = (imgW * state.panelWidthPercent * state.scale).coerceAtMost(imgW * 0.98f)
        val padding = state.panelHeightPaddingDp * scale

        val typeface = getTypeface(state.fontFamily, state.isBold)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = state.textColorHex.toInt()
            this.typeface = typeface
            setShadowLayer(8f * scale, 2f, 2f, Color.argb(220, 0, 0, 0))
        }

        val titleSize = 24f * scale * state.fontSizeMultiplier
        val metaSize = 16f * scale * state.fontSizeMultiplier
        val textWidth = (panelWidth - (padding * 2f)).toInt().coerceAtLeast(100)

        val line1 = state.locationTitle
        val line2 = CoordinatesFormatter.format(state.latitude, state.longitude, state.coordinatesFormat)
        val line3 = "${state.customDate}   ${state.customTime}   ${state.customTimezone}"

        val l1Layout = createStaticLayout(line1, textPaint, titleSize, textWidth, state.textAlign, isBold = true)
        val l2Layout = createStaticLayout(line2, textPaint, metaSize, textWidth, state.textAlign)
        val l3Layout = createStaticLayout(line3, textPaint, metaSize, textWidth, state.textAlign)

        val totalHeight = l1Layout.height + l2Layout.height + l3Layout.height + (16f * scale)
        val centerX = imgW * state.normX
        val bottomY = imgH * state.normY
        val left = (centerX - (panelWidth / 2f)).coerceIn(8f * scale, imgW - panelWidth - 8f * scale)
        val top = (bottomY - totalHeight).coerceIn(8f * scale, imgH - totalHeight - 8f * scale)

        // Subtle gradient backing scrim
        if (state.backgroundOpacity > 0.05f) {
            val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = state.backgroundColorHex.toInt()
                alpha = (state.backgroundOpacity * 255).toInt().coerceIn(0, 255)
            }
            canvas.drawRoundRect(RectF(left, top, left + panelWidth, top + totalHeight + (padding * 2f)), 8f * scale, 8f * scale, scrimPaint)
        }

        canvas.save()
        canvas.translate(left + padding, top + padding)
        l1Layout.draw(canvas)
        canvas.translate(0f, l1Layout.height + (6f * scale))
        l2Layout.draw(canvas)
        canvas.translate(0f, l2Layout.height + (6f * scale))
        l3Layout.draw(canvas)
        canvas.restore()
    }

    private fun createStaticLayout(
        text: String,
        basePaint: TextPaint,
        textSize: Float,
        width: Int,
        align: OverlayTextAlign,
        isBold: Boolean = false
    ): StaticLayout {
        val paint = TextPaint(basePaint).apply {
            this.textSize = textSize
            if (isBold) this.isFakeBoldText = true
        }

        val alignment = when (align) {
            OverlayTextAlign.LEFT -> Layout.Alignment.ALIGN_NORMAL
            OverlayTextAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
            OverlayTextAlign.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
        }

        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(10))
            .setAlignment(alignment)
            .setLineSpacing(0f, 1.15f)
            .setIncludePad(true)
            .build()
    }

    private fun getTypeface(fontOption: FontFamilyOption, isBold: Boolean): Typeface {
        val style = if (isBold) Typeface.BOLD else Typeface.NORMAL
        return when (fontOption) {
            FontFamilyOption.DEFAULT -> Typeface.create(Typeface.DEFAULT, style)
            FontFamilyOption.SERIF -> Typeface.create(Typeface.SERIF, style)
            FontFamilyOption.SANS_SERIF -> Typeface.create(Typeface.SANS_SERIF, style)
            FontFamilyOption.MONOSPACE -> Typeface.create(Typeface.MONOSPACE, style)
            FontFamilyOption.CURSIVE -> Typeface.create("cursive", style)
        }
    }
}
