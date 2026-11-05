package com.gpsphotocamera.app.engine

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Root configuration container for the High-Precision GPS Photograph Overlay Rendering Engine.
 *
 * Every visual parameter is completely configurable, while default values are mathematically
 * calibrated to match the reference GPS Map Camera photograph proportions:
 * - Base reference resolution: 1086 x 1448
 * - Map card: Left aligned, 283 x 283 px square, rounded corners (16px)
 * - Text panel: Right side, 691 x 283 px, translucent black (62% opacity), rounded corners (16px)
 * - Badge: Top-right "GPS Map Camera" with camera icon (218 x 75 px)
 * - Typography: Roboto sans-serif, bold heading, regular address/coords/date/note
 */
data class OverlayConfig(
    val panel: PanelConfig = PanelConfig(),
    val map: MapConfig = MapConfig(),
    val location: LocationData = LocationData(),
    val dateTime: DateTimeConfig = DateTimeConfig(),
    val note: NoteConfig = NoteConfig(),
    val badge: BadgeConfig = BadgeConfig(),
    val typography: TypographyConfig = TypographyConfig(),
    val output: OutputConfig = OutputConfig()
) : Serializable {

    companion object {
        /**
         * Creates an [OverlayConfig] matching the exact layout, text, coordinates,
         * date/time, and visual style of the reference GPS Map Camera photograph.
         */
        fun createReferencePreset(): OverlayConfig {
            return OverlayConfig(
                panel = PanelConfig(
                    position = PanelPosition.BOTTOM,
                    horizontalMarginPercent = 0.04144f,  // 45px / 1086px
                    bottomMarginPercent = 0.01520f,       // 22px / 1448px
                    gapBetweenMapAndPanelPercent = 0.02026f, // 22px / 1086px
                    panelWidthPercent = 0.63628f,         // 691px / 1086px
                    panelHeightPercent = 0.19544f,        // 283px / 1448px
                    cornerRadiusPercent = 0.01473f,       // 16px / 1086px
                    backgroundColor = 0x9E0C0C0C.toInt(), // ~62% translucent black (#9E0C0C0C)
                    paddingLeftPercent = 0.02302f,        // 25px / 1086px
                    paddingRightPercent = 0.02302f,       // 25px / 1086px
                    paddingTopPercent = 0.02279f,         // 33px / 1448px
                    paddingBottomPercent = 0.01796f       // 26px / 1448px
                ),
                map = MapConfig(
                    enabled = true,
                    widthPercent = 0.26059f,              // 283px / 1086px
                    heightPercent = 0.26059f,             // 283px / 1086px (Square)
                    cornerRadiusPercent = 0.01473f,       // 16px / 1086px
                    showMarker = true,
                    markerColor = 0xFFEA4335.toInt(),
                    markerCenterColor = 0xFFB31412.toInt(),
                    showGoogleLogo = true
                ),
                location = LocationData(
                    heading = "Pune, Maharashtra, India",
                    flagEmoji = "🇮🇳",
                    addressLines = listOf(
                        "851, Near Yes Bank Corner, Deccan",
                        "Gymkhana, Pune, Maharashtra 411004, India"
                    ),
                    latitude = 18.518667,
                    longitude = 73.833756,
                    decimalPrecision = 6
                ),
                dateTime = DateTimeConfig(
                    day = "Tuesday",
                    date = "03/11/2026",
                    time = "02:45 PM",
                    timezone = "GMT +05:30"
                ),
                note = NoteConfig(
                    visible = true,
                    prefix = "Note : ",
                    text = "Captured by GPS Map Camera"
                ),
                badge = BadgeConfig(
                    visible = true,
                    text = "GPS Map Camera",
                    widthPercent = 0.20074f,              // 218px / 1086px
                    heightPercent = 0.06906f,             // 75px / 1086px
                    cornerRadiusPercent = 0.01473f,
                    backgroundColor = 0x9E0C0C0C.toInt()
                ),
                typography = TypographyConfig(
                    headingFontSizePercent = 0.04236f,    // 46px / 1086px
                    bodyFontSizePercent = 0.02670f,       // 29px / 1086px
                    badgeFontSizePercent = 0.01842f,      // 20px / 1086px
                    headingColor = 0xFFFFFFFF.toInt(),
                    bodyColor = 0xFFFFFFFF.toInt(),
                    lineSpacingMultiplier = 1.25f
                )
            )
        }

        /**
         * Creates an [OverlayConfig] populated with current live device timestamp
         * and configurable defaults.
         */
        fun createLiveDefault(): OverlayConfig {
            val preset = createReferencePreset()
            val now = Date()
            val dayFormat = SimpleDateFormat("EEEE", Locale.US)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

            val tz = TimeZone.getDefault()
            val offsetHours = tz.rawOffset / (1000 * 60 * 60)
            val offsetMinutes = Math.abs((tz.rawOffset / (1000 * 60)) % 60)
            val tzStr = String.format(Locale.US, "GMT %+03d:%02d", offsetHours, offsetMinutes)

            return preset.copy(
                dateTime = DateTimeConfig(
                    day = dayFormat.format(now),
                    date = dateFormat.format(now),
                    time = timeFormat.format(now),
                    timezone = tzStr
                )
            )
        }
    }
}

/**
 * Vertical placement mode of the main metadata panel.
 */
enum class PanelPosition {
    BOTTOM,
    TOP,
    CUSTOM
}

/**
 * Geometry and visual styling for the main translucent information card.
 */
data class PanelConfig(
    val position: PanelPosition = PanelPosition.BOTTOM,
    val horizontalMarginPercent: Float = 0.04144f,
    val bottomMarginPercent: Float = 0.01520f,
    val topMarginPercent: Float = 0.01520f,
    val gapBetweenMapAndPanelPercent: Float = 0.02026f,
    val panelWidthPercent: Float = 0.63628f,
    val panelHeightPercent: Float = 0.19544f,
    val cornerRadiusPercent: Float = 0.01473f,
    val backgroundColor: Int = 0x9E0C0C0C.toInt(),
    val paddingLeftPercent: Float = 0.02302f,
    val paddingRightPercent: Float = 0.02302f,
    val paddingTopPercent: Float = 0.02279f,
    val paddingBottomPercent: Float = 0.01796f,
    val customCenterXPercent: Float = 0.5f,
    val customCenterYPercent: Float = 0.85f
) : Serializable {
    val opacity: Float
        get() = ((backgroundColor ushr 24) and 0xFF) / 255f

    fun withOpacity(newOpacity: Float): PanelConfig {
        val clamped = newOpacity.coerceIn(0f, 1f)
        val alpha = (clamped * 255f).toInt()
        val rgb = backgroundColor and 0x00FFFFFF
        return copy(backgroundColor = (alpha shl 24) or rgb)
    }
}

/**
 * Configuration for the satellite map thumbnail on the left.
 */
data class MapConfig(
    val enabled: Boolean = true,
    val widthPercent: Float = 0.26059f,
    val heightPercent: Float = 0.26059f,
    val cornerRadiusPercent: Float = 0.01473f,
    @Transient val satelliteBitmap: Bitmap? = null,
    val showMarker: Boolean = true,
    val markerColor: Int = 0xFFEA4335.toInt(),
    val markerCenterColor: Int = 0xFFB31412.toInt(),
    val markerWidthPercent: Float = 0.0295f,
    val markerHeightPercent: Float = 0.0506f,
    val showGoogleLogo: Boolean = true
) : Serializable

/**
 * Location data displayed in heading, address, and coordinates lines.
 */
data class LocationData(
    val heading: String = "Pune, Maharashtra, India",
    val flagEmoji: String = "🇮🇳",
    val addressLines: List<String> = listOf(
        "851, Near Yes Bank Corner, Deccan",
        "Gymkhana, Pune, Maharashtra 411004, India"
    ),
    val latitude: Double = 18.518667,
    val longitude: Double = 73.833756,
    val decimalPrecision: Int = 6,
    val customCoordinatesString: String? = null
) : Serializable {

    fun formattedCoordinates(): String {
        if (!customCoordinatesString.isNullOrBlank()) {
            return customCoordinatesString
        }
        val formatPattern = "%.${decimalPrecision}f"
        val latStr = String.format(Locale.US, formatPattern, latitude)
        val lonStr = String.format(Locale.US, formatPattern, longitude)
        return "Lat $latStr° Long $lonStr°"
    }

    val displayHeading: String
        get() = if (flagEmoji.isNotBlank()) "$heading $flagEmoji" else heading
}

/**
 * Date and time components allowing independent editing.
 */
data class DateTimeConfig(
    val day: String = "Tuesday",
    val date: String = "03/11/2026",
    val time: String = "02:45 PM",
    val timezone: String = "GMT +05:30",
    val customFormattedString: String? = null
) : Serializable {

    fun formattedString(): String {
        if (!customFormattedString.isNullOrBlank()) {
            return customFormattedString
        }
        val builder = StringBuilder()
        if (day.isNotBlank()) builder.append(day).append(", ")
        if (date.isNotBlank()) builder.append(date).append(" ")
        if (time.isNotBlank()) builder.append(time).append(" ")
        if (timezone.isNotBlank()) builder.append(timezone)
        return builder.toString().trim()
    }
}

/**
 * Custom note line at the bottom of the information card.
 */
data class NoteConfig(
    val visible: Boolean = true,
    val prefix: String = "Note : ",
    val text: String = "Captured by GPS Map Camera",
    val customFullString: String? = null
) : Serializable {

    fun formattedString(): String {
        if (!customFullString.isNullOrBlank()) {
            return customFullString
        }
        return "$prefix$text"
    }
}

/**
 * Top-right badge sitting on the main panel ("GPS Map Camera").
 */
data class BadgeConfig(
    val visible: Boolean = true,
    val text: String = "GPS Map Camera",
    @Transient val customIconBitmap: Bitmap? = null,
    val widthPercent: Float = 0.20074f,
    val heightPercent: Float = 0.06906f,
    val cornerRadiusPercent: Float = 0.01473f,
    val backgroundColor: Int = 0x9E0C0C0C.toInt(),
    val textColor: Int = 0xFFFFFFFF.toInt()
) : Serializable

/**
 * Proportional typography sizing, weights, line spacing, and fonts.
 */
data class TypographyConfig(
    val headingFontFamily: String = "sans-serif",
    val headingFontWeight: Int = Typeface.BOLD,
    val bodyFontFamily: String = "sans-serif",
    val bodyFontWeight: Int = Typeface.NORMAL,
    val headingFontSizePercent: Float = 0.04236f,  // 46px on 1086px
    val bodyFontSizePercent: Float = 0.02670f,     // 29px on 1086px
    val badgeFontSizePercent: Float = 0.01842f,    // 20px on 1086px
    val headingColor: Int = 0xFFFFFFFF.toInt(),
    val bodyColor: Int = 0xFFFFFFFF.toInt(),
    val badgeTextColor: Int = 0xFFFFFFFF.toInt(),
    val lineSpacingMultiplier: Float = 1.25f,
    val fontSizeMultiplier: Float = 1.0f
) : Serializable

/**
 * Output format and compression settings for image export.
 */
enum class ExportFormat {
    JPEG,
    PNG
}

data class OutputConfig(
    val format: ExportFormat = ExportFormat.JPEG,
    val jpegQuality: Int = 95,
    val preserveExif: Boolean = true
) : Serializable
