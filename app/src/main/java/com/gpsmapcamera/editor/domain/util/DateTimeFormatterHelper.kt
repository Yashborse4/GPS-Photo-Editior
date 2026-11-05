package com.gpsmapcamera.editor.domain.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeFormatterHelper {

    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    private val time12Format = SimpleDateFormat("hh:mm:ss a", Locale.US)
    private val time24Format = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.US)

    /**
     * Converts a millisecond epoch to formatted date string.
     */
    fun formatDate(epochMillis: Long): String {
        return displayDateFormat.format(Date(epochMillis))
    }

    /**
     * Converts calendar hour and minute to formatted string.
     */
    fun formatTime(hour: Int, minute: Int, second: Int = 0, is24Hour: Boolean): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
        }
        return if (is24Hour) {
            time24Format.format(cal.time)
        } else {
            time12Format.format(cal.time)
        }
    }

    /**
     * Derives Day of Week from dd/MM/yyyy string.
     */
    fun deriveDayOfWeek(dateString: String): String {
        return try {
            val date = displayDateFormat.parse(dateString)
            if (date != null) dayOfWeekFormat.format(date) else "Thursday"
        } catch (e: Exception) {
            "Thursday"
        }
    }

    /**
     * Switch 12-hour and 24-hour time string without loss of minutes or seconds.
     */
    fun convertTimeFormat(timeStr: String, target24Hour: Boolean): String {
        return try {
            val parsedDate = if (timeStr.contains("AM", ignoreCase = true) || timeStr.contains("PM", ignoreCase = true)) {
                SimpleDateFormat("hh:mm:ss a", Locale.US).parse(timeStr)
                    ?: SimpleDateFormat("hh:mm a", Locale.US).parse(timeStr)
            } else {
                SimpleDateFormat("HH:mm:ss", Locale.US).parse(timeStr)
                    ?: SimpleDateFormat("HH:mm", Locale.US).parse(timeStr)
            }

            if (parsedDate != null) {
                if (target24Hour) {
                    time24Format.format(parsedDate)
                } else {
                    time12Format.format(parsedDate)
                }
            } else {
                timeStr
            }
        } catch (e: Exception) {
            timeStr
        }
    }

    /**
     * Get list of common timezones for the selector.
     */
    fun getCommonTimezones(): List<Pair<String, String>> {
        return listOf(
            "GMT +05:30" to "India Standard Time (IST)",
            "GMT +00:00" to "Greenwich Mean Time (UTC/GMT)",
            "GMT -05:00" to "Eastern Standard Time (EST)",
            "GMT -08:00" to "Pacific Standard Time (PST)",
            "GMT -06:00" to "Central Standard Time (CST)",
            "GMT +01:00" to "Central European Time (CET)",
            "GMT +08:00" to "Singapore / China (SGT/CST)",
            "GMT +09:00" to "Japan Standard Time (JST)",
            "GMT +10:00" to "Australian Eastern Time (AEST)",
            "GMT +04:00" to "Gulf Standard Time (GST)"
        )
    }
}
