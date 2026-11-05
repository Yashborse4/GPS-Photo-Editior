package com.gpsphotocamera.app.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility for formatting and parsing dates, times, and timezone strings
 * compatible with Android API 26+.
 */
object AppDateTimeFormatter {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    private val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.US)
    private val shortTimeFormat = SimpleDateFormat("hh:mm a", Locale.US)

    fun getCurrentDateString(): String {
        return dateFormat.format(Date())
    }

    fun getCurrentTimeString(includeSeconds: Boolean = true): String {
        val format = if (includeSeconds) timeFormat else shortTimeFormat
        return format.format(Date())
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
        return dateFormat.format(Date(millis))
    }

    fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        return timeFormat.format(calendar.time)
    }
}
