package com.gpsphotocamera.app.image

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Extracts optional embedded EXIF GPS coordinates and capture timestamps
 * from an uploaded photo to pre-fill input fields.
 */
object ExifExtractor {

    data class ExtractedExifData(
        val latitude: Double?,
        val longitude: Double?,
        val dateString: String?,
        val timeString: String?
    )

    suspend fun extract(context: Context, uri: Uri): ExtractedExifData = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val latLong = exif.latLong

                val lat = latLong?.get(0)
                val lng = latLong?.get(1)

                // Try parsing EXIF datetime (format usually: "yyyy:MM:dd HH:mm:ss")
                val dateTimeStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

                var parsedDate: String? = null
                var parsedTime: String? = null

                if (!dateTimeStr.isNullOrBlank()) {
                    try {
                        val inputFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                        val date = inputFormat.parse(dateTimeStr)
                        if (date != null) {
                            val outDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                            val outTimeFormat = SimpleDateFormat("hh:mm:ss a", Locale.US)
                            parsedDate = outDateFormat.format(date)
                            parsedTime = outTimeFormat.format(date)
                        }
                    } catch (e: Exception) {
                        // ignore parse exception
                    }
                }

                ExtractedExifData(
                    latitude = lat,
                    longitude = lng,
                    dateString = parsedDate,
                    timeString = parsedTime
                )
            } ?: ExtractedExifData(null, null, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
            ExtractedExifData(null, null, null, null)
        }
    }
}
