package com.gpsphotocamera.app.engine

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils

/**
 * High-precision typography layout and rendering engine for Android Canvas.
 *
 * Handles:
 * - Proportional font sizing scaled to source image resolution
 * - Accurate baseline alignment using FontMetrics
 * - Multi-line paragraph wrapping via StaticLayout
 * - Full Unicode and Emoji rendering (including country flags like 🇮🇳)
 * - Safe clipping and overflow prevention
 */
object TextRenderer {

    /**
     * Builds an anti-aliased [TextPaint] with specified typeface, size, and color.
     */
    fun createPaint(
        typeface: Typeface,
        textSize: Float,
        textColor: Int
    ): TextPaint {
        return TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.typeface = typeface
            this.textSize = textSize
            this.color = textColor
            this.isFilterBitmap = true
            this.isDither = true
        }
    }

    /**
     * Measures the cap-height of an uppercase character for baseline alignment.
     */
    fun measureCapHeight(paint: Paint): Float {
        val bounds = Rect()
        paint.getTextBounds("H", 0, 1, bounds)
        return bounds.height().toFloat()
    }

    /**
     * Measures the exact width of a single text line.
     */
    fun measureTextWidth(text: String, paint: Paint): Float {
        return paint.measureText(text)
    }

    /**
     * Draws a single line of text with full emoji support at the given baseline coordinates.
     */
    fun drawSingleLine(
        canvas: Canvas,
        text: String,
        x: Float,
        baselineY: Float,
        paint: TextPaint
    ) {
        canvas.drawText(text, x, baselineY, paint)
    }

    /**
     * Truncates text with an ellipsis if it exceeds [maxWidth].
     */
    fun ellipsizeText(
        text: String,
        paint: TextPaint,
        maxWidth: Float
    ): String {
        return TextUtils.ellipsize(
            text,
            paint,
            maxWidth,
            TextUtils.TruncateAt.END
        ).toString()
    }

    /**
     * Builds a [StaticLayout] for multi-line text wrapping with line spacing and maximum lines.
     */
    fun buildStaticLayout(
        text: CharSequence,
        paint: TextPaint,
        availableWidth: Int,
        maxLines: Int = 10,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        lineSpacingMultiplier: Float = 1.0f
    ): StaticLayout {
        val safeWidth = availableWidth.coerceAtLeast(10)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, safeWidth)
                .setAlignment(alignment)
                .setLineSpacing(0f, lineSpacingMultiplier)
                .setIncludePad(false)
                .setMaxLines(maxLines)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                text,
                paint,
                safeWidth,
                alignment,
                lineSpacingMultiplier,
                0f,
                false
            )
        }
    }

    /**
     * Draws a multi-line paragraph using [StaticLayout] translated to [startX, startY].
     * Returns the total height occupied by the paragraph.
     */
    fun drawMultiLine(
        canvas: Canvas,
        layout: StaticLayout,
        startX: Float,
        startY: Float
    ): Float {
        canvas.save()
        canvas.translate(startX, startY)
        layout.draw(canvas)
        canvas.restore()
        return layout.height.toFloat()
    }

    /**
     * Helper to compute proportional font size in pixels given the image width
     * and a reference proportion ratio.
     */
    fun computeScaledFontSize(
        imageWidth: Int,
        fontPercent: Float,
        multiplier: Float = 1.0f
    ): Float {
        return imageWidth * fontPercent * multiplier
    }
}
