package com.gpsphotocamera.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gpsphotocamera.app.data.model.SavedPhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Repository responsible for persisting and retrieving recently stamped photos.
 * Uses JSON storage backed by SharedPreferences for instantaneous offline access.
 */
class RecentPhotosRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _recentPhotos = MutableStateFlow<List<SavedPhotoItem>>(emptyList())
    val recentPhotos: StateFlow<List<SavedPhotoItem>> = _recentPhotos.asStateFlow()

    init {
        loadPhotos()
    }

    private fun loadPhotos() {
        val json = prefs.getString(KEY_PHOTOS, null)
        if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<SavedPhotoItem>>() {}.type
                val items: List<SavedPhotoItem> = gson.fromJson(json, type) ?: emptyList()
                _recentPhotos.value = items.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                e.printStackTrace()
                _recentPhotos.value = emptyList()
            }
        }
    }

    suspend fun addPhoto(item: SavedPhotoItem) = withContext(Dispatchers.IO) {
        val current = _recentPhotos.value.toMutableList()
        current.removeAll { it.id == item.id }
        current.add(0, item) // Insert newest at beginning
        val trimmed = if (current.size > MAX_RECENT_ITEMS) current.take(MAX_RECENT_ITEMS) else current
        _recentPhotos.value = trimmed

        val json = gson.toJson(trimmed)
        prefs.edit().putString(KEY_PHOTOS, json).apply()
    }

    suspend fun removePhoto(id: String) = withContext(Dispatchers.IO) {
        val current = _recentPhotos.value.toMutableList()
        current.removeAll { it.id == id }
        _recentPhotos.value = current

        val json = gson.toJson(current)
        prefs.edit().putString(KEY_PHOTOS, json).apply()
    }

    companion object {
        private const val PREFS_NAME = "gps_camera_recent_photos"
        private const val KEY_PHOTOS = "recent_photos_json"
        private const val MAX_RECENT_ITEMS = 50
    }
}
