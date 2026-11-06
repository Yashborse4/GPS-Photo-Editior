package com.gpsphotocamera.app.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility for formatting and parsing dates, times, timezone strings, and EXIF timestamps.
 */
object AppDateTimeFormatter {

    private val supportedDatePatterns = listOf(
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "yyyy-MM-dd",
        "yyyy:MM:dd",
        "MM/dd/yyyy",
        "d/M/yyyy",
        "d-M-yyyy"
    )

    private val supportedTimePatterns = listOf(
        "hh:mm:ss a",
        "hh:mm a",
        "h:mm:ss a",
        "h:mm a",
        "HH:mm:ss",
        "HH:mm"
    )

    fun getCurrentDateString(): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
    }

    fun getCurrentTimeString(includeSeconds: Boolean = false): String {
        val pattern = if (includeSeconds) "hh:mm:ss a" else "hh:mm a"
        return SimpleDateFormat(pattern, Locale.US).format(Date())
    }

    fun getCurrentTimezoneString(): String {
        val tz = TimeZone.getDefault()
        val calendar = Calendar.getInstance(tz)
        val offsetMillis = tz.getOffset(calendar.timeInMillis)
        val hours = offsetMillis / (1000 * 60 * 60)
        val minutes = Math.abs((offsetMillis / (1000 * 60)) % 60)
        val sign = if (hours >= 0) "+" else "-"
        return String.format(Locale.US, "GMT %s%02d:%02d", sign, Math.abs(hours), minutes)
    }

    /**
     * Calculates the Day of the Week name (e.g. "Monday", "Tuesday") from a date string.
     * Returns null if the date string cannot be parsed.
     */
    fun calculateDayOfWeek(dateStr: String): String? {
        val trimmed = dateStr.trim()
        if (trimmed.isBlank()) return null
        for (pattern in supportedDatePatterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val parsed = sdf.parse(trimmed)
                if (parsed != null) {
                    return SimpleDateFormat("EEEE", Locale.US).format(parsed)
                }
            } catch (e: Exception) {
                // Try next pattern
            }
        }
        return null
    }

    /**
     * Parses a date string into epoch millis for date picker initialization.
     */
    fun parseDateToMillis(dateStr: String): Long? {
        val trimmed = dateStr.trim()
        if (trimmed.isBlank()) return null
        for (pattern in supportedDatePatterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val parsed = sdf.parse(trimmed)
                if (parsed != null) return parsed.time
            } catch (e: Exception) {
                // Try next pattern
            }
        }
        return null
    }

    /**
     * Parses a timezone string (e.g. "GMT +05:30", "+05:30", "GMT-04:00", "UTC") into a [TimeZone].
     */
    fun parseTimezone(timezoneStr: String): TimeZone {
        val trimmed = timezoneStr.trim()
        if (trimmed.contains("UTC", ignoreCase = true) || trimmed == "GMT +00:00" || trimmed == "GMT+0:00") {
            return TimeZone.getTimeZone("UTC")
        }
        val match = Regex("([+-])\\s*(\\d{1,2}):?(\\d{2})?").find(trimmed)
        if (match != null) {
            val sign = match.groupValues[1]
            val hours = match.groupValues[2].toIntOrNull() ?: 0
            val minutes = match.groupValues[3].takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
            val tzId = String.format(Locale.US, "GMT%s%02d:%02d", sign, hours, minutes)
            return TimeZone.getTimeZone(tzId)
        }
        return TimeZone.getDefault()
    }

    /**
     * Parses the user-specified date, time, and timezone into epoch milliseconds.
     * Falls back to [System.currentTimeMillis] if parsing fails.
     */
    fun parseToEpochMillis(dateStr: String, timeStr: String, timezoneStr: String): Long {
        val tz = parseTimezone(timezoneStr)

        var parsedDate: Date? = null
        for (datePattern in supportedDatePatterns) {
            for (timePattern in supportedTimePatterns) {
                try {
                    val sdf = SimpleDateFormat("$datePattern $timePattern", Locale.US).apply {
                        timeZone = tz
                        isLenient = true
                    }
                    val date = sdf.parse("${dateStr.trim()} ${timeStr.trim()}")
                    if (date != null) {
                        parsedDate = date
                        break
                    }
                } catch (e: Exception) {
                    // Try next combination
                }
            }
            if (parsedDate != null) break
        }

        return parsedDate?.time ?: System.currentTimeMillis()
    }

    /**
     * Formats configured date and time to the standard EXIF datetime string:
     * "yyyy:MM:dd HH:mm:ss" (24-hour time).
     */
    fun formatToExifDateTime(dateStr: String, timeStr: String): String {
        for (datePattern in supportedDatePatterns) {
            for (timePattern in supportedTimePatterns) {
                try {
                    val sdf = SimpleDateFormat("$datePattern $timePattern", Locale.US).apply { isLenient = true }
                    val parsed = sdf.parse("${dateStr.trim()} ${timeStr.trim()}")
                    if (parsed != null) {
                        return SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(parsed)
                    }
                } catch (e: Exception) {
                    // Try next combination
                }
            }
        }
        return SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date())
    }

    /**
     * Extracts standardized timezone offset format (e.g. "+05:30", "-04:00")
     * compatible with EXIF 2.3 tags (TAG_OFFSET_TIME, TAG_OFFSET_TIME_ORIGINAL).
     */
    fun extractTimezoneOffset(timezoneStr: String): String {
        val trimmed = timezoneStr.trim()
        val match = Regex("([+-])\\s*(\\d{1,2}):?(\\d{2})?").find(trimmed)
        if (match != null) {
            val sign = match.groupValues[1]
            val hours = match.groupValues[2].toIntOrNull() ?: 0
            val minutes = match.groupValues[3].takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
            return String.format(Locale.US, "%s%02d:%02d", sign, hours, minutes)
        }
        val tz = TimeZone.getDefault()
        val offsetMillis = tz.getOffset(System.currentTimeMillis())
        val hours = offsetMillis / (1000 * 60 * 60)
        val minutes = Math.abs((offsetMillis / (1000 * 60)) % 60)
        val sign = if (hours >= 0) "+" else "-"
        return String.format(Locale.US, "%s%02d:%02d", sign, Math.abs(hours), minutes)
    }

    /**
     * Formats date string to standard EXIF GPS DateStamp: "yyyy:MM:dd".
     */
    fun formatToGpsDateStamp(dateStr: String): String {
        val trimmed = dateStr.trim()
        for (pattern in supportedDatePatterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = true }
                val parsed = sdf.parse(trimmed)
                if (parsed != null) {
                    return SimpleDateFormat("yyyy:MM:dd", Locale.US).format(parsed)
                }
            } catch (e: Exception) {
                // Try next pattern
            }
        }
        return SimpleDateFormat("yyyy:MM:dd", Locale.US).format(Date())
    }

    /**
     * Formats time to standard EXIF GPS TimeStamp (UTC time): "HH:mm:ss".
     */
    fun formatToGpsTimeStamp(dateStr: String, timeStr: String, timezoneStr: String): String {
        val epochMillis = parseToEpochMillis(dateStr, timeStr, timezoneStr)
        val utcFormat = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return utcFormat.format(Date(epochMillis))
    }

    /**
     * Formats full reference timestamp strictly matching GPS Map Camera:
     * Example: "Monday, 19/10/2026 12:18 PM GMT +05:30"
     */
    fun formatFullReferenceTimestamp(
        dayOfWeek: String? = null,
        dateStr: String,
        timeStr: String,
        timezoneStr: String
    ): String {
        val day = if (!dayOfWeek.isNullOrBlank()) {
            dayOfWeek.trim()
        } else {
            calculateDayOfWeek(dateStr) ?: "Monday"
        }

        // Format clean time without seconds (e.g. "12:18 PM")
        val cleanTime = if (timeStr.matches(Regex("\\d{1,2}:\\d{2}:\\d{2}\\s*(AM|PM|am|pm)"))) {
            timeStr.replace(Regex(":(\\d{2})\\s*(AM|PM|am|pm)"), " $2")
        } else {
            timeStr
        }

        val cleanTz = timezoneStr.substringBefore(" (").trim()
        return "$day, $dateStr $cleanTime $cleanTz"
    }

    /**
     * Backward-compatible overload for 3-argument calls.
     */
    fun formatFullReferenceTimestamp(dateStr: String, timeStr: String, timezoneStr: String): String {
        return formatFullReferenceTimestamp(null, dateStr, timeStr, timezoneStr)
    }

    /**
     * Common world timezones list for selection dialog / dropdown.
     */
    val commonTimezones: List<String> = listOf(
        "GMT +05:30 (IST)",
        "GMT +00:00 (UTC)",
        "GMT +01:00 (CET)",
        "GMT +02:00 (EET)",
        "GMT +03:00 (MSK / AST)",
        "GMT +04:00 (GST)",
        "GMT +07:00 (ICT)",
        "GMT +08:00 (SGT / CST)",
        "GMT +09:00 (JST / KST)",
        "GMT +10:00 (AEST)",
        "GMT -04:00 (EDT / AST)",
        "GMT -05:00 (EST / CDT)",
        "GMT -06:00 (CST)",
        "GMT -07:00 (MST / PDT)",
        "GMT -08:00 (PST)"
    )

    fun formatDate(millis: Long): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(millis))
    }

    fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        return SimpleDateFormat("hh:mm a", Locale.US).format(calendar.time)
    }
}
