package com.gpsphotocamera.app.maps

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.util.LruCache
import com.gpsphotocamera.app.data.model.AppMapType
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.tan

/**
 * High-performance map thumbnail generator for GPS Photo Camera stamps.
 * Fully integrates OpenStreetMap (OSM) and open satellite imagery without requiring API keys,
 * alongside optional Google Static Maps integration and an offline-first vector synthesizer.
 */
object MapSnapshotHelper {

    // In-memory cache for tile bitmaps (up to ~16MB of tiles)
    private val tileCache = object : LruCache<String, Bitmap>(64) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    /**
     * Generates a crisp map thumbnail bitmap of dimension [size] x [size].
     * Prioritizes OpenStreetMap and open satellite imagery without requiring API keys.
     *
     * @param latitude Target latitude
     * @param longitude Target longitude
     * @param mapType Map style (OpenStreetMap, Satellite, Standard, Terrain, Hybrid)
     * @param size Pixel dimensions of thumbnail (e.g. 480px)
     * @param googleApiKey Optional Google Maps Static API key
     */
    suspend fun generateMapThumbnail(
        latitude: Double,
        longitude: Double,
        mapType: AppMapType = AppMapType.SATELLITE,
        zoom: Int = 18,
        size: Int = 480,
        googleApiKey: String? = null
    ): Bitmap = withContext(Dispatchers.IO) {
        val safeZoom = zoom.coerceIn(12, 19)
        // 1. Fetch live OpenStreetMap / Open Satellite tile grid (No API key needed)
        val osmBitmap = when (mapType) {
            AppMapType.OPEN_STREET_MAP, AppMapType.NORMAL -> {
                fetchOpenStreetMapTileGrid(
                    latitude = latitude,
                    longitude = longitude,
                    zoom = safeZoom,
                    outputSize = size,
                    layerType = LayerType.OSM_STANDARD
                )
            }
            AppMapType.SATELLITE, AppMapType.HYBRID -> {
                fetchOpenStreetMapTileGrid(
                    latitude = latitude,
                    longitude = longitude,
                    zoom = safeZoom,
                    outputSize = size,
                    layerType = LayerType.SATELLITE_IMAGERY
                )
            }
            AppMapType.TERRAIN -> {
                fetchOpenStreetMapTileGrid(
                    latitude = latitude,
                    longitude = longitude,
                    zoom = safeZoom,
                    outputSize = size,
                    layerType = LayerType.TERRAIN_TOPO
                )
            }
        }

        if (osmBitmap != null) {
            return@withContext osmBitmap
        }

        // 2. If Google Maps API key is configured, fallback to Google Static Maps
        if (!googleApiKey.isNullOrBlank() && googleApiKey != "YOUR_GOOGLE_MAPS_API_KEY_HERE") {
            val googleBitmap = fetchOnlineStaticMap(latitude, longitude, mapType, safeZoom, size, googleApiKey)
            if (googleBitmap != null) {
                return@withContext googleBitmap
            }
        }

        // 3. Resilient offline fallback: Vector-synthesized satellite/standard map
        synthesizeDetailedMapTile(latitude, longitude, mapType, size)
    }

    /**
     * Fetches pure satellite or map imagery without any baked-in markers or watermarks,
     * so that the resolution-independent OverlayRenderer can draw clean vector pins,
     * radar cones, and branding overlays directly on the destination bitmap.
     */
    suspend fun fetchCleanSatelliteImagery(
        latitude: Double,
        longitude: Double,
        mapType: AppMapType = AppMapType.SATELLITE,
        zoom: Int = 18,
        size: Int = 480
    ): Bitmap = withContext(Dispatchers.IO) {
        val safeZoom = zoom.coerceIn(12, 19)
        val layer = when (mapType) {
            AppMapType.SATELLITE, AppMapType.HYBRID -> LayerType.SATELLITE_IMAGERY
            AppMapType.TERRAIN -> LayerType.TERRAIN_TOPO
            AppMapType.OPEN_STREET_MAP, AppMapType.NORMAL -> LayerType.OSM_STANDARD
        }
        val cleanBitmap = fetchOpenStreetMapTileGrid(
            latitude = latitude,
            longitude = longitude,
            zoom = safeZoom,
            outputSize = size,
            layerType = layer,
            drawPinAndWatermark = false
        )
        cleanBitmap ?: synthesizeDetailedMapTile(latitude, longitude, mapType, size, drawPinAndWatermark = false)
    }

    enum class LayerType {
        OSM_STANDARD,
        SATELLITE_IMAGERY,
        TERRAIN_TOPO
    }

    /**
     * Downloads and stitches a 3x3 tile grid around target coordinates from OpenStreetMap or open tile servers.
     * Computes exact Web Mercator pixel fractions and crops to an outputSize x outputSize bitmap.
     */
    suspend fun fetchOpenStreetMapTileGrid(
        latitude: Double,
        longitude: Double,
        zoom: Int = 16,
        outputSize: Int = 480,
        layerType: LayerType = LayerType.OSM_STANDARD,
        drawPinAndWatermark: Boolean = true
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val n = 1 shl zoom
            val xFrac = (longitude + 180.0) / 360.0 * n
            val safeLat = latitude.coerceIn(-85.05112878, 85.05112878)
            val latRad = Math.toRadians(safeLat)
            val yFrac = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / Math.PI) / 2.0 * n

            val centerTileX = xFrac.toInt()
            val centerTileY = yFrac.toInt()

            val pxInCenterTile = ((xFrac - centerTileX) * 256.0).toFloat()
            val pyInCenterTile = ((yFrac - centerTileY) * 256.0).toFloat()

            // 3x3 tiles grid surface (768 x 768)
            val stitchedBitmap = Bitmap.createBitmap(256 * 3, 256 * 3, Bitmap.Config.ARGB_8888)
            val stitchedCanvas = Canvas(stitchedBitmap)

            // Concurrently download the 3x3 surrounding tiles
            var tilesFetchedCount = 0
            coroutineScope {
                val jobs = mutableListOf<Deferred<Boolean>>()
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val tileX = centerTileX + dx
                        val tileY = centerTileY + dy
                        if (tileX in 0 until n && tileY in 0 until n) {
                            val job = async {
                                val tileUrl = when (layerType) {
                                    LayerType.OSM_STANDARD -> "https://tile.openstreetmap.org/$zoom/$tileX/$tileY.png"
                                    LayerType.SATELLITE_IMAGERY -> "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$zoom/$tileY/$tileX"
                                    LayerType.TERRAIN_TOPO -> "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/$zoom/$tileY/$tileX"
                                }
                                val tileBmp = getCachedOrDownloadTile(tileUrl)
                                if (tileBmp != null) {
                                    synchronized(stitchedCanvas) {
                                        stitchedCanvas.drawBitmap(
                                            tileBmp,
                                            ((dx + 1) * 256).toFloat(),
                                            ((dy + 1) * 256).toFloat(),
                                            null
                                        )
                                    }
                                    true
                                } else false
                            }
                            jobs.add(job)
                        }
                    }
                }
                val results = jobs.awaitAll()
                tilesFetchedCount = results.count { it }
            }

            // If we couldn't fetch at least the center tile or majority of tiles, return null for offline fallback
            if (tilesFetchedCount < 4) {
                stitchedBitmap.recycle()
                return@withContext null
            }

            // Center position in the stitched 3x3 grid
            val targetCenterX = 256f + pxInCenterTile
            val targetCenterY = 256f + pyInCenterTile

            // Crop window centered on (targetCenterX, targetCenterY)
            val half = outputSize / 2f
            val srcLeft = (targetCenterX - half).coerceAtLeast(0f)
            val srcTop = (targetCenterY - half).coerceAtLeast(0f)
            val srcRight = (srcLeft + outputSize).coerceAtMost(stitchedBitmap.width.toFloat())
            val srcBottom = (srcTop + outputSize).coerceAtMost(stitchedBitmap.height.toFloat())

            val outputBitmap = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
            val outputCanvas = Canvas(outputBitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            outputCanvas.drawBitmap(
                stitchedBitmap,
                Rect(srcLeft.toInt(), srcTop.toInt(), srcRight.toInt(), srcBottom.toInt()),
                Rect(0, 0, outputSize, outputSize),
                paint
            )
            stitchedBitmap.recycle()

            if (drawPinAndWatermark) {
                // Draw iconic red pin marker at exact center
                drawMapPin(outputCanvas, outputSize / 2f, outputSize / 2f, outputSize * 0.16f)

                // Draw attribution badge
                val attributionText = when (layerType) {
                    LayerType.OSM_STANDARD -> "© OpenStreetMap contributors"
                    LayerType.SATELLITE_IMAGERY -> "© OpenStreetMap / Esri"
                    LayerType.TERRAIN_TOPO -> "© OpenTopoMap / OSM"
                }
                drawMapWatermark(outputCanvas, attributionText, outputSize)
            }

            outputBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getCachedOrDownloadTile(url: String): Bitmap? {
        synchronized(tileCache) {
            val cached = tileCache.get(url)
            if (cached != null && !cached.isRecycled) {
                return cached
            }
        }

        val downloaded = downloadTile(url) ?: return null
        synchronized(tileCache) {
            tileCache.put(url, downloaded)
        }
        return downloaded
    }

    private fun downloadTile(urlString: String): Bitmap? {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.setRequestProperty("User-Agent", "GPSPhotoCameraApp/1.0 (Android; dev@gpsphotocamera.app)")
            connection.doInput = true
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Attempts to fetch real Google Maps satellite/roadmap imagery via Static Maps API.
     */
    private fun fetchOnlineStaticMap(
        lat: Double,
        lng: Double,
        mapType: AppMapType,
        zoom: Int,
        size: Int,
        apiKey: String
    ): Bitmap? {
        return try {
            val typeParam = when (mapType) {
                AppMapType.SATELLITE -> "satellite"
                AppMapType.HYBRID -> "hybrid"
                AppMapType.TERRAIN -> "terrain"
                AppMapType.NORMAL, AppMapType.OPEN_STREET_MAP -> "roadmap"
            }
            val urlString = "https://maps.googleapis.com/maps/api/staticmap?" +
                    "center=$lat,$lng&zoom=$zoom&size=${size}x$size&scale=2&maptype=$typeParam" +
                    "&markers=color:red%7C$lat,$lng&key=$apiKey"

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.doInput = true
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generates a realistic, highly stylized map tile with terrain gradients,
     * street grid geometry, compass indicator, and red location pin for offline usage.
     */
    fun synthesizeDetailedMapTile(
        latitude: Double,
        longitude: Double,
        mapType: AppMapType,
        size: Int,
        drawPinAndWatermark: Boolean = true
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val width = size.toFloat()
        val height = size.toFloat()

        // 1. Draw base terrain texture depending on map type
        when (mapType) {
            AppMapType.SATELLITE, AppMapType.HYBRID -> {
                val gradient = LinearGradient(
                    0f, 0f, width, height,
                    intArrayOf(
                        Color.rgb(18, 42, 30),
                        Color.rgb(36, 54, 40),
                        Color.rgb(24, 38, 48),
                        Color.rgb(42, 60, 46)
                    ),
                    floatArrayOf(0.0f, 0.35f, 0.70f, 1.0f),
                    Shader.TileMode.CLAMP
                )
                paint.shader = gradient
                canvas.drawRect(0f, 0f, width, height, paint)
                paint.shader = null

                // Water body
                val riverPath = Path().apply {
                    moveTo(0f, height * 0.7f)
                    cubicTo(width * 0.3f, height * 0.65f, width * 0.6f, height * 0.85f, width, height * 0.75f)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                paint.color = Color.rgb(18, 30, 40)
                canvas.drawPath(riverPath, paint)
            }
            AppMapType.TERRAIN -> {
                val gradient = LinearGradient(
                    0f, 0f, width, height,
                    intArrayOf(
                        Color.rgb(180, 195, 160),
                        Color.rgb(215, 210, 185),
                        Color.rgb(195, 205, 175),
                        Color.rgb(170, 185, 150)
                    ),
                    floatArrayOf(0.0f, 0.3f, 0.7f, 1.0f),
                    Shader.TileMode.CLAMP
                )
                paint.shader = gradient
                canvas.drawRect(0f, 0f, width, height, paint)
                paint.shader = null
            }
            AppMapType.NORMAL, AppMapType.OPEN_STREET_MAP -> {
                paint.color = Color.rgb(242, 239, 233)
                canvas.drawRect(0f, 0f, width, height, paint)

                // Park / green space
                val parkPath = Path().apply {
                    moveTo(width * 0.1f, height * 0.1f)
                    lineTo(width * 0.45f, height * 0.08f)
                    lineTo(width * 0.4f, height * 0.4f)
                    lineTo(width * 0.12f, height * 0.35f)
                    close()
                }
                paint.color = Color.rgb(205, 235, 176)
                canvas.drawPath(parkPath, paint)
            }
        }

        // 2. Synthesize street road grid
        val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (mapType == AppMapType.SATELLITE || mapType == AppMapType.HYBRID) {
                Color.argb(160, 200, 210, 220)
            } else {
                Color.WHITE
            }
            strokeWidth = width * 0.024f
            style = Paint.Style.STROKE
        }

        val majorRoadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (mapType == AppMapType.SATELLITE || mapType == AppMapType.HYBRID) {
                Color.argb(220, 245, 158, 11)
            } else {
                Color.rgb(254, 213, 114)
            }
            strokeWidth = width * 0.045f
            style = Paint.Style.STROKE
        }

        // Diagonal Highway
        val hwPath = Path().apply {
            moveTo(0f, height * 0.35f)
            cubicTo(width * 0.4f, height * 0.4f, width * 0.6f, height * 0.55f, width, height * 0.65f)
        }
        canvas.drawPath(hwPath, majorRoadPaint)

        // Arterial Road
        val arterialPath = Path().apply {
            moveTo(width * 0.5f, 0f)
            lineTo(width * 0.5f, height)
        }
        canvas.drawPath(arterialPath, majorRoadPaint)

        // Secondary cross streets
        val numCross = 5
        for (i in 1..numCross) {
            val y = height * (i.toFloat() / (numCross + 1))
            canvas.drawLine(0f, y, width, y, roadPaint)
        }
        for (i in 1..numCross) {
            val x = width * (i.toFloat() / (numCross + 1))
            canvas.drawLine(x, 0f, x, height, roadPaint)
        }

        if (drawPinAndWatermark) {
            // 3. Red Location Marker Pin
            val markerX = width / 2f
            val markerY = height / 2f
            drawMapPin(canvas, markerX, markerY, width * 0.16f)

            // 4. Subtle Compass Rose (Top Right)
            val compassRadius = width * 0.09f
            drawCompassRose(canvas, width - compassRadius - (width * 0.04f), compassRadius + (width * 0.04f), compassRadius)

            // 5. Watermark attribution
            val attribution = if (mapType == AppMapType.OPEN_STREET_MAP) "© OpenStreetMap" else "GPS Map Camera"
            drawMapWatermark(canvas, attribution, size)
        }

        return bitmap
    }

    private fun drawMapWatermark(canvas: Canvas, text: String, size: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val padding = size * 0.035f

        paint.color = Color.argb(170, 0, 0, 0)
        paint.style = Paint.Style.FILL
        val bgRect = RectF(
            padding,
            padding,
            padding + (size * 0.48f),
            padding + (size * 0.065f)
        )
        canvas.drawRoundRect(bgRect, size * 0.015f, size * 0.015f, paint)

        paint.color = Color.WHITE
        paint.textSize = size * 0.030f
        paint.isFakeBoldText = true
        paint.style = Paint.Style.FILL
        canvas.drawText(text, padding + size * 0.02f, padding + size * 0.045f, paint)
    }

    private fun drawCompassRose(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.argb(190, 15, 23, 42)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, radius, paint)

        paint.color = Color.argb(140, 255, 255, 255)
        paint.strokeWidth = radius * 0.1f
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(cx, cy, radius, paint)

        val northPath = Path().apply {
            moveTo(cx, cy - radius * 0.75f)
            lineTo(cx - radius * 0.3f, cy)
            lineTo(cx + radius * 0.3f, cy)
            close()
        }
        paint.color = Color.rgb(239, 68, 68)
        paint.style = Paint.Style.FILL
        canvas.drawPath(northPath, paint)

        val southPath = Path().apply {
            moveTo(cx, cy + radius * 0.75f)
            lineTo(cx - radius * 0.3f, cy)
            lineTo(cx + radius * 0.3f, cy)
            close()
        }
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawPath(southPath, paint)

        paint.color = Color.rgb(30, 41, 59)
        canvas.drawCircle(cx, cy, radius * 0.2f, paint)
    }

    private fun drawMapPin(canvas: Canvas, cx: Float, cy: Float, pinHeight: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val pinWidth = pinHeight * 0.65f
        val topCircleRadius = pinWidth / 2

        paint.color = Color.argb(100, 0, 0, 0)
        paint.style = Paint.Style.FILL
        canvas.drawOval(
            cx - pinWidth * 0.4f, cy - pinHeight * 0.05f,
            cx + pinWidth * 0.4f, cy + pinHeight * 0.15f,
            paint
        )

        val headCy = cy - pinHeight + topCircleRadius
        val path = Path().apply {
            moveTo(cx, cy)
            cubicTo(
                cx - pinWidth * 0.6f, headCy + topCircleRadius * 0.8f,
                cx - topCircleRadius, headCy + topCircleRadius * 0.2f,
                cx - topCircleRadius, headCy
            )
            arcTo(
                RectF(cx - topCircleRadius, headCy - topCircleRadius, cx + topCircleRadius, headCy + topCircleRadius),
                180f, 180f
            )
            cubicTo(
                cx + topCircleRadius, headCy + topCircleRadius * 0.2f,
                cx + pinWidth * 0.6f, headCy + topCircleRadius * 0.8f,
                cx, cy
            )
            close()
        }

        paint.color = Color.rgb(225, 29, 72)
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)

        paint.color = Color.argb(180, 255, 255, 255)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = pinHeight * 0.03f
        canvas.drawPath(path, paint)

        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, headCy, topCircleRadius * 0.38f, paint)
    }
}
