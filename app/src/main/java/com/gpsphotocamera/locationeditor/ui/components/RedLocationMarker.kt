package com.gpsphotocamera.locationeditor.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/**
 * Generates the authentic GPS Map Camera red location marker pin.
 *
 * Characteristics:
 * - High-contrast vibrant red body with subtle top-to-bottom depth gradient
 * - Concentric white center ring with deep crimson inner dot
 * - Realistic contact shadow at the anchor point on the satellite surface
 * - Anchor point positioned precisely at the bottom tip (0.5f, 1.0f)
 */
object RedLocationMarker {

    // Cached bitmap descriptor for Google Maps marker rendering
    private var cachedDescriptor: BitmapDescriptor? = null

    /**
     * Get or create BitmapDescriptor for GoogleMap Marker.
     */
    fun getBitmapDescriptor(context: Context): BitmapDescriptor {
        cachedDescriptor?.let { return it }
        val bitmap = createPinBitmap(context, widthDp = 48f, heightDp = 62f)
        val descriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        cachedDescriptor = descriptor
        return descriptor
    }

    /**
     * Creates a high-resolution, anti-aliased Bitmap of the red GPS location pin.
     */
    fun createPinBitmap(
        context: Context,
        widthDp: Float = 48f,
        heightDp: Float = 62f
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val width = (widthDp * density).toInt()
        val height = (heightDp * density).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val w = width.toFloat()
        val h = height.toFloat()

        // Reserve bottom 12% for contact shadow
        val shadowHeight = h * 0.14f
        val pinBottomY = h - (shadowHeight * 0.5f)
        val pinTopY = 2f * density
        val pinWidth = w * 0.82f
        val centerX = w / 2f
        val headRadius = pinWidth / 2f
        val headCenterY = pinTopY + headRadius

        // 1. Draw contact drop shadow at the pin's base tip
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                centerX,
                pinBottomY,
                pinWidth * 0.45f,
                intArrayOf(Color.argb(160, 0, 0, 0), Color.argb(60, 0, 0, 0), Color.TRANSPARENT),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        val shadowRect = RectF(
            centerX - (pinWidth * 0.45f),
            pinBottomY - (shadowHeight * 0.45f),
            centerX + (pinWidth * 0.45f),
            pinBottomY + (shadowHeight * 0.55f)
        )
        canvas.drawOval(shadowRect, shadowPaint)

        // 2. Build teardrop path
        val pinPath = Path().apply {
            // Rounded head (top half circle)
            arcTo(
                RectF(centerX - headRadius, pinTopY, centerX + headRadius, pinTopY + (2 * headRadius)),
                180f,
                180f,
                false
            )
            // Right taper down to bottom tip
            quadTo(
                centerX + headRadius * 0.98f,
                headCenterY + headRadius * 0.95f,
                centerX,
                pinBottomY
            )
            // Left taper back up to rounded head
            quadTo(
                centerX - headRadius * 0.98f,
                headCenterY + headRadius * 0.95f,
                centerX - headRadius,
                headCenterY
            )
            close()
        }

        // 3. Paint teardrop with vibrant 3D red gradient
        val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                centerX,
                pinTopY,
                centerX,
                pinBottomY,
                intArrayOf(
                    Color.parseColor("#FF5252"), // Bright scarlet top
                    Color.parseColor("#E53935"), // Core vibrant GPS camera red
                    Color.parseColor("#B71C1C")  // Deep dark red bottom edge
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            isDither = true
        }
        canvas.drawPath(pinPath, redPaint)

        // Subtle dark outline to ensure visibility against bright satellite desert/snow tiles
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * density
            color = Color.parseColor("#88000000")
        }
        canvas.drawPath(pinPath, strokePaint)

        // 4. White center circle
        val whiteCircleRadius = headRadius * 0.48f
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isDither = true
        }
        canvas.drawCircle(centerX, headCenterY, whiteCircleRadius, whitePaint)

        // 5. Deep crimson center inner dot (standard GPS camera pin style)
        val innerDotRadius = whiteCircleRadius * 0.48f
        val innerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D32F2F")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, headCenterY, innerDotRadius, innerDotPaint)

        return bitmap
    }
}
