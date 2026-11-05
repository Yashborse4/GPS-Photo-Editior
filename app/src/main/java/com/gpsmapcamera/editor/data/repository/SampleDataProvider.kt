package com.gpsmapcamera.editor.data.repository

import com.gpsmapcamera.editor.data.model.GpsOverlayState

data class LocationPresetItem(
    val title: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val timezone: String
)

object SampleDataProvider {

    val locationPresets = listOf(
        LocationPresetItem(
            title = "Pune, Maharashtra, India",
            address = "Shivaji Nagar, FC Road, 411005",
            latitude = 18.520430,
            longitude = 73.856744,
            altitude = 560.2,
            timezone = "GMT +05:30"
        ),
        LocationPresetItem(
            title = "New York, NY, United States",
            address = "Broadway & 7th Ave, Times Square, 10036",
            latitude = 40.758000,
            longitude = -73.985500,
            altitude = 15.0,
            timezone = "GMT -05:00"
        ),
        LocationPresetItem(
            title = "Paris, Île-de-France, France",
            address = "Champ de Mars, 5 Av. Anatole France, 75007",
            latitude = 48.858400,
            longitude = 2.294500,
            altitude = 35.0,
            timezone = "GMT +01:00"
        ),
        LocationPresetItem(
            title = "Tokyo, Kantō, Japan",
            address = "4 Chome-2-8 Shibakoen, Minato City, 105-0011",
            latitude = 35.658600,
            longitude = 139.745400,
            altitude = 28.0,
            timezone = "GMT +09:00"
        ),
        LocationPresetItem(
            title = "London, Greater London, United Kingdom",
            address = "Westminster, SW1A 0AA, Big Ben Plaza",
            latitude = 51.500700,
            longitude = -0.124600,
            altitude = 12.0,
            timezone = "GMT +00:00"
        ),
        LocationPresetItem(
            title = "Sydney, NSW, Australia",
            address = "Bennelong Point, Sydney Opera House, 2000",
            latitude = -33.856800,
            longitude = 151.215300,
            altitude = 5.0,
            timezone = "GMT +10:00"
        ),
        LocationPresetItem(
            title = "Agra, Uttar Pradesh, India",
            address = "Dharmapuri, Forest Colony, Tajganj, 282001",
            latitude = 27.175100,
            longitude = 78.042100,
            altitude = 171.0,
            timezone = "GMT +05:30"
        )
    )

    fun createInitialState(): GpsOverlayState {
        return GpsOverlayState.createDefault()
    }
}
