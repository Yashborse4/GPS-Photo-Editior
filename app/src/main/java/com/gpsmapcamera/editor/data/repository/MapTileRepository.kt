package com.gpsmapcamera.editor.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.gpsmapcamera.editor.data.model.GpsOverlayState
import com.gpsmapcamera.editor.domain.renderer.SatelliteMapRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MapTileRepository {

    // In-memory cache for map bitmaps (up to 20 tiles)
    private val memoryCache = object : LruCache<String, Bitmap>(20) {
        override fun entryRemoved(evicted: Boolean, key: String?, oldValue: Bitmap?, newValue: Bitmap?) {
            // Note: Don't recycle immediately if still referenced in UI
        }
    }

    /**
     * Retrieves or generates a map thumbnail bitmap.
     * Tries remote Google Maps Static API if apiKey is provided, otherwise generates
     * a high fidelity offline satellite map tile with pin and compass.
     */
    suspend fun getMapTile(
        state: GpsOverlayState,
        apiKey: String? = null,
        targetWidth: Int = 320
    ): Bitmap = withContext(Dispatchers.IO) {
        val cacheKey = buildCacheKey(state, targetWidth)
        memoryCache.get(cacheKey)?.let { return@withContext it }

        var bitmap: Bitmap? = null

        // Try downloading static map tile if apiKey is provided
        if (!apiKey.isNullOrBlank()) {
            bitmap = downloadStaticMap(state, apiKey, targetWidth)
        }

        // Fallback to high-fidelity offline procedural satellite renderer
        if (bitmap == null) {
            bitmap = SatelliteMapRenderer.generateMapTile(
                latitude = state.latitude,
                longitude = state.longitude,
                mapType = state.mapType,
                aspectRatio = state.mapAspectRatio,
                zoom = state.mapZoom,
                heading = state.headingDegrees,
                showCompass = state.showCompass,
                showMarker = state.showMarker,
                widthPx = targetWidth
            )
        }

        memoryCache.put(cacheKey, bitmap)
        bitmap
    }

    private fun downloadStaticMap(state: GpsOverlayState, apiKey: String, width: Int): Bitmap? {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        try {
            val mapTypeParam = state.mapType.name.lowercase(Locale.US)
            val height = when (state.mapAspectRatio) {
                com.gpsmapcamera.editor.data.model.MapAspectRatio.SQUARE,
                com.gpsmapcamera.editor.data.model.MapAspectRatio.CIRCLE -> width
                com.gpsmapcamera.editor.data.model.MapAspectRatio.LANDSCAPE -> (width * 0.75f).toInt()
                com.gpsmapcamera.editor.data.model.MapAspectRatio.WIDE -> (width * 0.5625f).toInt()
            }

            val urlString = "https://maps.googleapis.com/maps/api/staticmap?" +
                    "center=${state.latitude},${state.longitude}&" +
                    "zoom=${state.mapZoom}&" +
                    "size=${width}x${height}&" +
                    "maptype=$mapTypeParam&" +
                    "markers=color:red%7C${state.latitude},${state.longitude}&" +
                    "key=$apiKey"

            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.doInput = true
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.inputStream
                return BitmapFactory.decodeStream(inputStream)
            }
        } catch (_: Exception) {
            // Silently fall back to offline renderer
        } finally {
            inputStream?.close()
            connection?.disconnect()
        }
        return null
    }

    private fun buildCacheKey(state: GpsOverlayState, targetWidth: Int): String {
        return "${state.latitude}_${state.longitude}_${state.mapType.name}_" +
                "${state.mapAspectRatio.name}_${state.mapZoom}_${state.headingDegrees}_" +
                "${state.showCompass}_${state.showMarker}_$targetWidth"
    }

    fun clearCache() {
        memoryCache.evictAll()
    }
}
