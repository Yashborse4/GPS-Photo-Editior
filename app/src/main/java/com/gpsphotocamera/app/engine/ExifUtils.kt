package com.gpsphotocamera.app.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * Production-grade EXIF metadata and orientation handler.
 *
 * Ensures:
 * - Proper rotation of portrait and landscape photographs based on EXIF tag
 * - Preservation of source camera metadata (lens, exposure, model, ISO)
 * - Injection of high-precision GPS coordinate tags into exported JPEG files
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

            // 4. Inject Date and Time
            val exifDateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
            val timestampStr = exifDateFormat.format(Date())
            targetExif.setAttribute(ExifInterface.TAG_DATETIME, timestampStr)
            targetExif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, timestampStr)

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
