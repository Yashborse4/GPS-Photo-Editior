package com.gpsphotocamera.app.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.gpsphotocamera.app.data.model.GpsPhotoData
import com.gpsphotocamera.app.utils.AppDateTimeFormatter
import java.io.File
import java.io.InputStream
import java.util.Locale
import kotlin.math.abs

/**
 * Production-grade EXIF metadata and orientation handler.
 *
 * Ensures:
 * - Proper rotation of portrait and landscape photographs based on EXIF tag
 * - Preservation of source camera metadata (lens, exposure, model, ISO)
 * - Injection of high-precision GPS coordinate tags into exported JPEG files
 * - Injection of accurate user-configured Date, Day, Time, and Timezone into EXIF headers
 */
object ExifUtils {

    /**
     * Reads the EXIF orientation tag from a [Uri].
     */
    fun readOrientation(context: Context, uri: Uri): Int {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val exif = ExifInterface(inputStream)
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } else {
                ExifInterface.ORIENTATION_NORMAL
            }
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        } finally {
            inputStream?.close()
        }
    }

    /**
     * Reads the EXIF orientation tag from a [File].
     */
    fun readOrientation(file: File): Int {
        return try {
            val exif = ExifInterface(file)
            exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    /**
     * Creates an affine transformation [Matrix] corresponding to the given EXIF orientation.
     */
    fun createOrientationMatrix(orientation: Int): Matrix {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> {
                // Identity
            }
        }
        return matrix
    }

    /**
     * Applies orientation transformation to [bitmap], recycling the original if a new one is allocated.
     */
    fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) {
            return bitmap
        }
        val matrix = createOrientationMatrix(orientation)
        return try {
            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0,
                bitmap.width, bitmap.height,
                matrix, true
            )
            if (rotated != bitmap && !bitmap.isRecycled) {
                bitmap.recycle()
            }
            rotated
        } catch (e: OutOfMemoryError) {
            bitmap
        }
    }

    /**
     * Injects comprehensive EXIF metadata (GPS, DateTime, Timezone, Address, Note)
     * into a content URI target (Android 10+ Scoped Storage).
     */
    fun injectExifIntoUri(
        context: Context,
        targetUri: Uri,
        data: GpsPhotoData,
        sourceUri: Uri? = null
    ) {
        try {
            context.contentResolver.openFileDescriptor(targetUri, "rw")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                applyExifAttributes(exif, data, sourceUri, context)
                exif.saveAttributes()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Injects comprehensive EXIF metadata into a direct [File] target (Android 8-9 or cache).
     */
    fun injectExifIntoFile(
        targetFile: File,
        data: GpsPhotoData,
        sourceUri: Uri? = null,
        context: Context? = null
    ) {
        try {
            val exif = ExifInterface(targetFile.absolutePath)
            applyExifAttributes(exif, data, sourceUri, context)
            exif.saveAttributes()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Writes all GPS, DateTime, and descriptive tags onto an [ExifInterface] instance.
     */
    fun applyExifAttributes(
        exif: ExifInterface,
        data: GpsPhotoData,
        sourceUri: Uri? = null,
        context: Context? = null
    ) {
        // 1. Copy camera hardware / exposure tags from original photo if available
        if (sourceUri != null && context != null) {
            try {
                context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    val sourceExif = ExifInterface(stream)
                    val preservedAttributes = arrayOf(
                        ExifInterface.TAG_MAKE,
                        ExifInterface.TAG_MODEL,
                        ExifInterface.TAG_F_NUMBER,
                        ExifInterface.TAG_EXPOSURE_TIME,
                        ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
                        ExifInterface.TAG_FOCAL_LENGTH,
                        ExifInterface.TAG_WHITE_BALANCE,
                        ExifInterface.TAG_FLASH,
                        ExifInterface.TAG_LENS_MAKE,
                        ExifInterface.TAG_LENS_MODEL
                    )
                    for (tag in preservedAttributes) {
                        val value = sourceExif.getAttribute(tag)
                        if (value != null) {
                            exif.setAttribute(tag, value)
                        }
                    }
                }
            } catch (e: Exception) {
                // Non-fatal if source EXIF cannot be opened
            }
        }

        // 2. Set Orientation to NORMAL (image is already baked upright)
        exif.setAttribute(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL.toString()
        )

        // 3. Inject High-Precision GPS coordinates
        val lat = data.latitude
        val lon = data.longitude
        exif.setAttribute(
            ExifInterface.TAG_GPS_LATITUDE,
            convertDecimalToDmsRational(abs(lat))
        )
        exif.setAttribute(
            ExifInterface.TAG_GPS_LATITUDE_REF,
            if (lat >= 0) "N" else "S"
        )
        exif.setAttribute(
            ExifInterface.TAG_GPS_LONGITUDE,
            convertDecimalToDmsRational(abs(lon))
        )
        exif.setAttribute(
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            if (lon >= 0) "E" else "W"
        )

        // Altitude
        val altMeters = abs(data.altitudeMeters)
        val altRational = "${(altMeters * 100).toLong()}/100"
        exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, altRational)
        exif.setAttribute(
            ExifInterface.TAG_GPS_ALTITUDE_REF,
            if (data.altitudeMeters >= 0) "0" else "1"
        )

        // GPS Date & Time (UTC)
        val gpsDateStamp = AppDateTimeFormatter.formatToGpsDateStamp(data.dateString)
        val gpsTimeStamp = AppDateTimeFormatter.formatToGpsTimeStamp(data.dateString, data.timeString, data.timezone)
        exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, gpsDateStamp)
        exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, gpsTimeStamp)
        exif.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, "GPS Map Camera")

        // 4. Inject Date and Time (Configured local time)
        val exifDateTime = AppDateTimeFormatter.formatToExifDateTime(data.dateString, data.timeString)
        exif.setAttribute(ExifInterface.TAG_DATETIME, exifDateTime)
        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, exifDateTime)
        exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, exifDateTime)

        // Timezone offsets (EXIF 2.3)
        val tzOffset = AppDateTimeFormatter.extractTimezoneOffset(data.timezone)
        exif.setAttribute(ExifInterface.TAG_OFFSET_TIME, tzOffset)
        exif.setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL, tzOffset)
        exif.setAttribute(ExifInterface.TAG_OFFSET_TIME_DIGITIZED, tzOffset)
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, "00")
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, "00")
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME_DIGITIZED, "00")

        // 5. Inject Description, User Comment, Software
        val description = if (data.postalAddress.isNotBlank()) {
            "${data.locationHeading}\n${data.postalAddress}".trim()
        } else {
            data.locationHeading.trim()
        }
        exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, description)

        val comment = buildString {
            append("Day: ").append(data.dayOfWeek).append("\n")
            append("Date: ").append(data.dateString).append(" ").append(data.timeString).append(" ").append(data.timezone).append("\n")
            append("Location: ").append(data.locationHeading).append("\n")
            if (data.postalAddress.isNotBlank()) {
                append("Address: ").append(data.postalAddress).append("\n")
            }
            append("Latitude: ").append(String.format(Locale.US, "%.6f", data.latitude)).append("\n")
            append("Longitude: ").append(String.format(Locale.US, "%.6f", data.longitude)).append("\n")
            if (data.customNote.isNotBlank()) {
                append("Note: ").append(data.customNote)
            }
        }
        exif.setAttribute(ExifInterface.TAG_USER_COMMENT, comment)
        exif.setAttribute(ExifInterface.TAG_SOFTWARE, "GPS Map Camera")

        if (exif.getAttribute(ExifInterface.TAG_MAKE).isNullOrBlank()) {
            exif.setAttribute(ExifInterface.TAG_MAKE, "GPS Map Camera")
        }
        if (exif.getAttribute(ExifInterface.TAG_MODEL).isNullOrBlank()) {
            exif.setAttribute(ExifInterface.TAG_MODEL, "Android")
        }
    }

    /**
     * Copies EXIF attributes from source photo to target output file, and updates
     * GPS and DateTime tags matching the overlay information.
     */
    fun copyAndInjectExif(
        sourceFile: File?,
        targetFile: File,
        location: LocationData,
        dateTimeConfig: DateTimeConfig
    ) {
        try {
            val targetExif = ExifInterface(targetFile)

            // 1. Copy non-geometric attributes from original photograph if available
            if (sourceFile != null && sourceFile.exists()) {
                val sourceExif = ExifInterface(sourceFile)
                val preservedAttributes = arrayOf(
                    ExifInterface.TAG_MAKE,
                    ExifInterface.TAG_MODEL,
                    ExifInterface.TAG_F_NUMBER,
                    ExifInterface.TAG_EXPOSURE_TIME,
                    ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
                    ExifInterface.TAG_FOCAL_LENGTH,
                    ExifInterface.TAG_WHITE_BALANCE
                )
                for (tag in preservedAttributes) {
                    val value = sourceExif.getAttribute(tag)
                    if (value != null) {
                        targetExif.setAttribute(tag, value)
                    }
                }
            }

            // 2. Set Orientation to NORMAL (image is already baked upright)
            targetExif.setAttribute(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL.toString()
            )

            // 3. Inject High-Precision GPS coordinates
            val lat = location.latitude
            val lon = location.longitude
            targetExif.setAttribute(
                ExifInterface.TAG_GPS_LATITUDE,
                convertDecimalToDmsRational(abs(lat))
            )
            targetExif.setAttribute(
                ExifInterface.TAG_GPS_LATITUDE_REF,
                if (lat >= 0) "N" else "S"
            )
            targetExif.setAttribute(
                ExifInterface.TAG_GPS_LONGITUDE,
                convertDecimalToDmsRational(abs(lon))
            )
            targetExif.setAttribute(
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                if (lon >= 0) "E" else "W"
            )
            targetExif.setAttribute(
                ExifInterface.TAG_GPS_DATESTAMP,
                AppDateTimeFormatter.formatToGpsDateStamp(dateTimeConfig.date)
            )
            targetExif.setAttribute(
                ExifInterface.TAG_GPS_TIMESTAMP,
                AppDateTimeFormatter.formatToGpsTimeStamp(dateTimeConfig.date, dateTimeConfig.time, dateTimeConfig.timezone)
            )

            // 4. Inject Date and Time
            val timestampStr = AppDateTimeFormatter.formatToExifDateTime(dateTimeConfig.date, dateTimeConfig.time)
            targetExif.setAttribute(ExifInterface.TAG_DATETIME, timestampStr)
            targetExif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, timestampStr)
            targetExif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, timestampStr)

            val tzOffset = AppDateTimeFormatter.extractTimezoneOffset(dateTimeConfig.timezone)
            targetExif.setAttribute(ExifInterface.TAG_OFFSET_TIME, tzOffset)
            targetExif.setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL, tzOffset)
            targetExif.setAttribute(ExifInterface.TAG_OFFSET_TIME_DIGITIZED, tzOffset)

            targetExif.saveAttributes()
        } catch (e: Exception) {
            // Non-fatal if EXIF writing is restricted
        }
    }

    /**
     * Converts decimal coordinates to standard EXIF Degrees/Minutes/Seconds rational format.
     */
    fun convertDecimalToDmsRational(decimal: Double): String {
        var d = decimal
        val degrees = d.toInt()
        d = (d - degrees) * 60.0
        val minutes = d.toInt()
        d = (d - minutes) * 60.0
        val seconds = (d * 1000.0).toInt()

        return "$degrees/1,$minutes/1,$seconds/1000"
    }
}
