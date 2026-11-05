package com.gpsmapcamera.editor.domain.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.gpsmapcamera.editor.data.model.MapAspectRatio
import com.gpsmapcamera.editor.data.model.MapType
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-fidelity offline and fallback map renderer.
 * Generates realistic satellite, hybrid, or terrain map tiles with geographic features,
 * coordinates grid, compass rose, and an iconic GPS location pin marker.
 */
object SatelliteMapRenderer {

    fun generateMapTile(
        latitude: Double,
        longitude: Double,
        mapType: MapType,
        aspectRatio: MapAspectRatio,
        zoom: Int,
        heading: Float = 0f,
        showCompass: Boolean = true,
        showMarker: Boolean = true,
        widthPx: Int = 320
    ): Bitmap {
        val heightPx = when (aspectRatio) {
            MapAspectRatio.SQUARE, MapAspectRatio.CIRCLE -> widthPx
            MapAspectRatio.LANDSCAPE -> (widthPx * 0.75f).toInt()
            MapAspectRatio.WIDE -> (widthPx * 0.5625f).toInt()
        }

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Draw base terrain/satellite texture based on map type
        when (mapType) {
            MapType.SATELLITE -> drawSatelliteTexture(canvas, widthPx, heightPx, latitude, longitude, zoom)
            MapType.HYBRID -> {
                drawSatelliteTexture(canvas, widthPx, heightPx, latitude, longitude, zoom)
                drawRoadNetwork(canvas, widthPx, heightPx, latitude, longitude, isHybrid = true)
            }
            MapType.TERRAIN -> drawTerrainTexture(canvas, widthPx, heightPx, latitude, longitude, zoom)
            MapType.ROADMAP -> drawRoadmapTexture(canvas, widthPx, heightPx, latitude, longitude)
        }

        // 2. Draw subtle coordinate grid lines
        drawCoordinateGrid(canvas, widthPx, heightPx)

        // 3. Draw Compass indicator in top corner
        if (showCompass) {
            drawCompassRose(canvas, widthPx, heightPx, heading)
        }

        // 4. Draw Center Location Pin Marker & Pulse
        if (showMarker) {
            drawLocationPin(canvas, widthPx / 2f, heightPx / 2f)
        }

        // 5. Corner watermark / label: "GPS MAP"
        drawMapBadge(canvas, widthPx, heightPx, mapType)

        // 6. If circle aspect ratio, clip to circle
        if (aspectRatio == MapAspectRatio.CIRCLE) {
            return cropToCircle(bitmap)
        }

        return bitmap
    }

    private fun drawSatelliteTexture(canvas: Canvas, w: Int, h: Int, lat: Double, lng: Double, zoom: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Base earth dark emerald/slate gradient
        val baseGradient = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(Color.parseColor("#1B2E1D"), Color.parseColor("#263829"), Color.parseColor("#1C2526")),
            null, Shader.TileMode.CLAMP
        )
        paint.shader = baseGradient
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        // Agricultural / Urban texture patches
        val patchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        // Patch 1: Urban rooftop / concrete cluster
        patchPaint.color = Color.parseColor("#3D4B44")
        canvas.drawRect(w * 0.15f, h * 0.2f, w * 0.55f, h * 0.65f, patchPaint)

        // Patch 2: Crop field / greenery
        patchPaint.color = Color.parseColor("#2E4F28")
        canvas.drawRect(w * 0.45f, h * 0.45f, w * 0.88f, h * 0.85f, patchPaint)

        // Patch 3: Forest canopy
        patchPaint.color = Color.parseColor("#16351B")
        val forestPath = Path().apply {
            moveTo(w * 0.05f, h * 0.6f)
            cubicTo(w * 0.2f, h * 0.7f, w * 0.35f, h * 0.85f, w * 0.3f, h.toFloat())
            lineTo(0f, h.toFloat())
            close()
        }
        canvas.drawPath(forestPath, patchPaint)

        // River / Canal water feature
        val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E3A5F")
            style = Paint.Style.STROKE
            strokeWidth = w * 0.045f
            strokeCap = Paint.Cap.ROUND
        }
        val riverPath = Path().apply {
            moveTo(0f, h * 0.35f)
            cubicTo(w * 0.3f, h * 0.45f, w * 0.65f, h * 0.25f, w.toFloat(), h * 0.55f)
        }
        canvas.drawPath(riverPath, waterPaint)
    }

    private fun drawRoadNetwork(canvas: Canvas, w: Int, h: Int, lat: Double, lng: Double, isHybrid: Boolean) {
        val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isHybrid) Color.parseColor("#E0E0E0") else Color.parseColor("#FFA000")
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }

        val highwayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFCA28")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }

        // Main highway
        val highwayPath = Path().apply {
            moveTo(w * 0.2f, 0f)
            lineTo(w * 0.8f, h.toFloat())
        }
        canvas.drawPath(highwayPath, highwayPaint)

        // Secondary cross roads
        val secondary1 = Path().apply {
            moveTo(0f, h * 0.6f)
            lineTo(w.toFloat(), h * 0.4f)
        }
        val secondary2 = Path().apply {
            moveTo(0f, h * 0.25f)
            lineTo(w * 0.7f, h * 0.3f)
        }
        canvas.drawPath(secondary1, roadPaint)
        canvas.drawPath(secondary2, roadPaint)
    }

    private fun drawTerrainTexture(canvas: Canvas, w: Int, h: Int, lat: Double, lng: Double, zoom: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Topographic earth gradient
        val terrainGradient = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(Color.parseColor("#C8D6AF"), Color.parseColor("#DDD2B2"), Color.parseColor("#E8E0D5")),
            null, Shader.TileMode.CLAMP
        )
        paint.shader = terrainGradient
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        // Contour lines
        val contourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A89E88")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        for (i in 1..5) {
            val offset = i * (h / 6f)
            val path = Path().apply {
                moveTo(0f, offset)
                cubicTo(w * 0.3f, offset - 25f, w * 0.7f, offset + 30f, w.toFloat(), offset - 10f)
            }
            canvas.drawPath(path, contourPaint)
        }
    }

    private fun drawRoadmapTexture(canvas: Canvas, w: Int, h: Int, lat: Double, lng: Double) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5F5F3")
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Parks
        val parkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CEEACD")
        }
        canvas.drawRect(w * 0.05f, h * 0.05f, w * 0.45f, h * 0.45f, parkPaint)

        // Water
        val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#AAD3DF")
        }
        val waterPath = Path().apply {
            moveTo(w * 0.6f, 0f)
            lineTo(w.toFloat(), 0f)
            lineTo(w.toFloat(), h * 0.7f)
            lineTo(w * 0.75f, h * 0.5f)
            close()
        }
        canvas.drawPath(waterPath, waterPaint)

        // Roads
        drawRoadNetwork(canvas, w, h, lat, lng, isHybrid = false)
    }

    private fun drawCoordinateGrid(canvas: Canvas, w: Int, h: Int) {
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        // Subtle 3x3 grid
        canvas.drawLine(w * 0.33f, 0f, w * 0.33f, h.toFloat(), gridPaint)
        canvas.drawLine(w * 0.66f, 0f, w * 0.66f, h.toFloat(), gridPaint)
        canvas.drawLine(0f, h * 0.33f, w.toFloat(), h * 0.33f, gridPaint)
        canvas.drawLine(0f, h * 0.66f, w.toFloat(), h * 0.66f, gridPaint)
    }

    private fun drawCompassRose(canvas: Canvas, w: Int, h: Int, heading: Float) {
        val cx = w - 24f
        val cy = 24f
        val radius = 16f

        // Background circle
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(160, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius, bgPaint)

        // Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        canvas.drawCircle(cx, cy, radius, borderPaint)

        // North needle (Red)
        val northPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF3B30")
            style = Paint.Style.FILL
        }
        val rad = Math.toRadians((heading - 90.0))
        val nx = cx + (radius * 0.75f * cos(rad)).toFloat()
        val ny = cy + (radius * 0.75f * sin(rad)).toFloat()

        val northPath = Path().apply {
            moveTo(nx, ny)
            lineTo(cx + (4f * -sin(rad)).toFloat(), cy + (4f * cos(rad)).toFloat())
            lineTo(cx - (4f * -sin(rad)).toFloat(), cy - (4f * cos(rad)).toFloat())
            close()
        }
        canvas.drawPath(northPath, northPaint)

        // South needle (White)
        val southPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val sx = cx - (radius * 0.75f * cos(rad)).toFloat()
        val sy = cy - (radius * 0.75f * sin(rad)).toFloat()
        val southPath = Path().apply {
            moveTo(sx, sy)
            lineTo(cx + (4f * -sin(rad)).toFloat(), cy + (4f * cos(rad)).toFloat())
            lineTo(cx - (4f * -sin(rad)).toFloat(), cy - (4f * cos(rad)).toFloat())
            close()
        }
        canvas.drawPath(southPath, southPaint)

        // "N" label
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("N", cx, cy - radius - 3f, textPaint)
    }

    private fun drawLocationPin(canvas: Canvas, cx: Float, cy: Float) {
        // 1. Radar pulse ripple
        val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(80, 255, 59, 48)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, 22f, pulsePaint)

        val pulseRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 255, 59, 48)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawCircle(cx, cy, 22f, pulseRingPaint)

        // 2. Drop shadow under pin
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawOval(RectF(cx - 7f, cy + 1f, cx + 7f, cy + 6f), shadowPaint)

        // 3. Iconic Map Pin Body (Vibrant Red)
        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF2D55")
            style = Paint.Style.FILL
        }

        val pinHeadRadius = 9f
        val pinTipY = cy
        val pinCenterY = cy - 14f

        val pinPath = Path().apply {
            moveTo(cx, pinTipY)
            lineTo(cx - pinHeadRadius * 0.85f, pinCenterY)
            arcTo(
                RectF(cx - pinHeadRadius, pinCenterY - pinHeadRadius, cx + pinHeadRadius, pinCenterY + pinHeadRadius),
                180f, 180f, false
            )
            lineTo(cx, pinTipY)
            close()
        }
        canvas.drawPath(pinPath, pinPaint)

        // Pin border highlight
        val pinBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawPath(pinPath, pinBorderPaint)

        // Center white dot in pin head
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, pinCenterY, 3.5f, dotPaint)
    }

    private fun drawMapBadge(canvas: Canvas, w: Int, h: Int, mapType: MapType) {
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(170, 0, 0, 0)
            style = Paint.Style.FILL
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9f
            isFakeBoldText = true
        }

        val label = mapType.label.uppercase(Locale.US)
        val textWidth = textPaint.measureText(label)
        val rect = RectF(6f, h - 20f, 16f + textWidth, h - 6f)
        canvas.drawRoundRect(rect, 4f, 4f, badgePaint)
        canvas.drawText(label, 11f, h - 10f, textPaint)
    }

    private fun cropToCircle(source: Bitmap): Bitmap {
        val size = Math.min(source.width, source.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, 0f, 0f, paint)
        source.recycle()
        return output
    }
}
