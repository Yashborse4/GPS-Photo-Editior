package com.gpsphotocamera.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gpsphotocamera.app.data.model.AppMapType
import com.gpsphotocamera.app.data.model.SavedLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Repository managing saved and favorite GPS locations.
 * Backed by SharedPreferences JSON storage for instantaneous offline retrieval.
 */
class SavedLocationsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _savedLocations = MutableStateFlow<List<SavedLocation>>(emptyList())
    val savedLocations: StateFlow<List<SavedLocation>> = _savedLocations.asStateFlow()

    init {
        loadLocations()
    }

    private fun loadLocations() {
        val json = prefs.getString(KEY_LOCATIONS, null)
        if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<SavedLocation>>() {}.type
                val items: List<SavedLocation> = gson.fromJson(json, type) ?: emptyList()
                if (items.isNotEmpty()) {
                    _savedLocations.value = items.sortedByDescending { it.timestamp }
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Initialize default pre-seeded locations for instant 1-tap usability
        val defaultSeeds = listOf(
            SavedLocation(
                id = "seed-pune",
                title = "Pune Reference Site",
                heading = "Pune, Maharashtra, India",
                address = "Shivaji Nagar, Pune, Maharashtra 411005, India",
                latitude = 18.520430,
                longitude = 73.856744,
                timezone = "GMT +05:30",
                mapType = AppMapType.OPEN_STREET_MAP,
                note = "Site Survey & Inspection",
                timestamp = System.currentTimeMillis()
            ),
            SavedLocation(
                id = "seed-mumbai",
                title = "Mumbai Gateway",
                heading = "Mumbai, Maharashtra, India",
                address = "Apollo Bandar, Colaba, Mumbai, Maharashtra 400001, India",
                latitude = 18.922000,
                longitude = 72.834700,
                timezone = "GMT +05:30",
                mapType = AppMapType.OPEN_STREET_MAP,
                note = "Field Inspection",
                timestamp = System.currentTimeMillis() - 1000
            ),
            SavedLocation(
                id = "seed-delhi",
                title = "New Delhi Central",
                heading = "New Delhi, Delhi, India",
                address = "Kartavya Path, India Gate, New Delhi 110001, India",
                latitude = 28.612900,
                longitude = 77.229500,
                timezone = "GMT +05:30",
                mapType = AppMapType.OPEN_STREET_MAP,
                note = "Central Project Site",
                timestamp = System.currentTimeMillis() - 2000
            )
        )
        _savedLocations.value = defaultSeeds
        persistLocations(defaultSeeds)
    }

    suspend fun saveLocation(location: SavedLocation) = withContext(Dispatchers.IO) {
        val current = _savedLocations.value.toMutableList()
        current.removeAll { it.id == location.id }
        current.add(0, location) // Prepend newest
        _savedLocations.value = current
        persistLocations(current)
    }

    suspend fun deleteLocation(id: String) = withContext(Dispatchers.IO) {
        val current = _savedLocations.value.toMutableList()
        current.removeAll { it.id == id }
        _savedLocations.value = current
        persistLocations(current)
    }

    private fun persistLocations(locations: List<SavedLocation>) {
        val json = gson.toJson(locations)
        prefs.edit().putString(KEY_LOCATIONS, json).apply()
    }

    companion object {
        private const val PREFS_NAME = "gps_camera_saved_locations"
        private const val KEY_LOCATIONS = "saved_locations_json"
    }
}
