package com.gpsmapcamera.editor.domain.util

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

data class ExtractedExifData(
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
    val formattedDate: String?,
    val formattedTime: String?,
    val cameraModel: String?
)

object ExifReader {

    fun extract(context: Context, uri: Uri): ExtractedExifData {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri) ?: return ExtractedExifData(null, null, null, null, null, null)
            val exif = ExifInterface(inputStream)

            val latLong = exif.latLong
            val latitude = latLong?.get(0)
            val longitude = latLong?.get(1)
            val altitude = exif.getAltitude(0.0).takeIf { it > 0.0 }
            val model = exif.getAttribute(ExifInterface.TAG_MODEL)

            // Parse Date and Time
            val dateTimeStr = exif.getAttribute(ExifInterface.TAG_DATETIME)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)

            var extractedDate: String? = null
            var extractedTime: String? = null

            if (!dateTimeStr.isNullOrBlank()) {
                try {
                    val parser = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                    val parsed = parser.parse(dateTimeStr)
                    if (parsed != null) {
                        val outDate = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                        val outTime = SimpleDateFormat("hh:mm:ss a", Locale.US)
                        extractedDate = outDate.format(parsed)
                        extractedTime = outTime.format(parsed)
                    }
                } catch (_: Exception) {}
            }

            return ExtractedExifData(
                latitude = latitude,
                longitude = longitude,
                altitude = altitude,
                formattedDate = extractedDate,
                formattedTime = extractedTime,
                cameraModel = model
            )
        } catch (e: Exception) {
            return ExtractedExifData(null, null, null, null, null, null)
        } finally {
            inputStream?.close()
        }
    }
}
