package com.gpsphotocamera.app.engine

import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.Typeface
import com.gpsphotocamera.app.data.model.AppMapType
import com.gpsphotocamera.app.data.model.GpsPhotoData
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Geometric layout bounds and typography sizes computed for a specific target image resolution.
 */
data class OverlayLayoutBounds(
    val mapRect: RectF,
    val panelRect: RectF,
    val badgeRect: RectF,
    val cornerRadius: Float,
    val badgeCornerRadius: Float,
    val headingTextSize: Float,
    val bodyTextSize: Float,
    val badgeTextSize: Float,
    val paddingLeft: Float,
    val paddingRight: Float,
    val paddingTop: Float,
    val paddingBottom: Float,
    val linePitch: Float
) : Serializable

/**
 * Root configuration container for the High-Precision GPS Photograph Overlay Rendering Engine.
 *
 * Implements resolution-independent, normalized parameters with mathematically calibrated
 * geometry matching the GPS Map Camera reference photograph:
 * - Independent Satellite Map thumbnail on bottom-left with rounded corners.
 * - Independent translucent black Information Panel on bottom-right (64-67% width on portrait).
 * - Independent small GPS Map Camera branding badge sitting atop the information panel.
 * - Full support for Portrait, Landscape, and Square image aspect ratios without distortion.
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

    /**
     * Dynamically computes exact layout bounds, margins, padding, and typography scaling
     * for any target image width and height (portrait, landscape, or square).
     */
    fun computeLayout(imageWidth: Int, imageHeight: Int): OverlayLayoutBounds {
        val W = imageWidth.toFloat()
        val H = imageHeight.toFloat()

        val mapWidth: Float
        val cardHeight: Float
        val gap: Float
        val panelWidth: Float
        val leftMargin: Float
        val bottomMargin: Float

        if (W <= H) {
            // Portrait or Square aspect ratio
            val aspect = W / H
            if (aspect <= 0.75f) {
                // Standard smartphone portrait (9:16, 3:4, etc.)
                // Information panel uses ~65% of photograph width (strictly in range 64–67%)
                panelWidth = W * 0.65f
                mapWidth = W * 0.26f
                gap = W * 0.02f
            } else {
                // Near-square or 1:1 square
                panelWidth = W * 0.62f
                mapWidth = W * 0.24f
                gap = W * 0.02f
            }
            cardHeight = mapWidth // Square map thumbnail; panel height matches map height
            val totalWidth = mapWidth + gap + panelWidth
            leftMargin = (W - totalWidth) / 2f
            bottomMargin = H * 0.02f
        } else {
            // Landscape aspect ratio (e.g. 4:3, 16:9, matching the 1024x768 reference photo)
            // Height is the bounding constraint to prevent the card from dominating the frame
            cardHeight = H * 0.258f // Exactly 198px on 768h
            mapWidth = cardHeight
            gap = cardHeight * (17f / 198f) // Exactly 17px
            panelWidth = cardHeight * (596f / 198f) // Exactly 596px
            val totalWidth = mapWidth + gap + panelWidth
            leftMargin = (W - totalWidth) / 2f // Symmetrically centered (106-107px)
            bottomMargin = H * (16f / 768f) // Exactly 16px on 768h
        }

        val cardBottom = H - bottomMargin
        val cardTop = cardBottom - cardHeight

        val mapRect = RectF(leftMargin, cardTop, leftMargin + mapWidth, cardBottom)
        val panelLeft = leftMargin + mapWidth + gap
        val panelRect = RectF(panelLeft, cardTop, panelLeft + panelWidth, cardBottom)

        // Branding badge sits atop the right side of the information panel
        val badgeWidth = cardHeight * (158f / 198f)
        val badgeHeight = cardHeight * (34f / 198f)
        val badgeRight = panelRect.right
        val badgeBottom = cardTop + (1.5f * (cardHeight / 198f)) // 1.5px overlap for clean union
        val badgeLeft = badgeRight - badgeWidth
        val badgeTop = badgeBottom - badgeHeight
        val badgeRect = RectF(badgeLeft, badgeTop, badgeRight, badgeBottom)

        val cornerRadius = cardHeight * (16f / 198f)
        val badgeCornerRadius = cardHeight * (9f / 198f)

        // Calibrated typography sizing matching the reference photograph
        val mult = typography.fontSizeMultiplier
        val headingTextSize = cardHeight * (30.5f / 198f) * mult
        val bodyTextSize = cardHeight * (20.5f / 198f) * mult
        val badgeTextSize = cardHeight * (13.5f / 198f) * mult

        val paddingLeft = cardHeight * (16f / 198f)
        val paddingRight = cardHeight * (16f / 198f)
        val paddingTop = cardHeight * (17f / 198f)
        val paddingBottom = cardHeight * (15f / 198f)
        val linePitch = cardHeight * (24f / 198f)

        return OverlayLayoutBounds(
            mapRect = mapRect,
            panelRect = panelRect,
            badgeRect = badgeRect,
            cornerRadius = cornerRadius,
            badgeCornerRadius = badgeCornerRadius,
            headingTextSize = headingTextSize,
            bodyTextSize = bodyTextSize,
            badgeTextSize = badgeTextSize,
            paddingLeft = paddingLeft,
            paddingRight = paddingRight,
            paddingTop = paddingTop,
            paddingBottom = paddingBottom,
            linePitch = linePitch
        )
    }

    companion object {
        /**
         * Converts domain [GpsPhotoData] into [OverlayConfig] with optional satellite imagery bitmap.
         */
        fun fromGpsPhotoData(data: GpsPhotoData, satelliteBitmap: Bitmap? = null): OverlayConfig {
            val bgAlpha = (data.backgroundOpacity * 255).toInt().coerceIn(0, 255)
            val bgRgb = (data.backgroundColorHex.toInt()) and 0x00FFFFFF
            val panelBgColor = (bgAlpha shl 24) or bgRgb

            return OverlayConfig(
                panel = PanelConfig(
                    backgroundColor = panelBgColor
                ),
                map = MapConfig(
                    enabled = data.showMap,
                    satelliteBitmap = satelliteBitmap,
                    mapType = data.mapType,
                    showMarker = data.showMarker,
                    showRadarBeam = data.showCompass
                ),
                location = LocationData(
                    heading = data.locationHeading,
                    latitude = data.latitude,
                    longitude = data.longitude,
                    postalAddress = data.postalAddress
                ),
                dateTime = DateTimeConfig(
                    day = data.dayOfWeek,
                    date = data.dateString,
                    time = data.timeString,
                    timezone = data.timezone
                ),
                note = NoteConfig(
                    visible = data.showNote,
                    text = data.customNote
                ),
                badge = BadgeConfig(
                    visible = true,
                    text = "GPS Map Camera"
                ),
                typography = TypographyConfig(
                    fontSizeMultiplier = data.fontSizeMultiplier
                )
            )
        }

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
                    backgroundColor = 0x9E0C0C0C.toInt()
                ),
                map = MapConfig(
                    enabled = true,
                    widthPercent = 0.26059f,              // 283px / 1086px
                    heightPercent = 0.26059f,             // 283px / 1086px (Square)
                    cornerRadiusPercent = 0.01473f,       // 16px / 1086px
                    showMarker = true,
                    markerColor = 0xFFEA4335.toInt(),
                    markerCenterColor = 0xFFFFFFFF.toInt(),
                    markerWidthPercent = 0.0295f,
                    markerHeightPercent = 0.0506f,
                    showGoogleLogo = true,
                    showRadarBeam = true
                ),
                location = LocationData(
                    heading = "Pune, Maharashtra, India",
                    flagEmoji = "🇮🇳",
                    postalAddress = "851, Near Yes Bank Corner, Deccan Gymkhana, Pune, Maharashtra 411004, India",
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
                    headingFontSizePercent = 0.04236f,
                    bodyFontSizePercent = 0.02670f,
                    badgeFontSizePercent = 0.01842f,
                    headingColor = 0xFFFFFFFF.toInt(),
                    bodyColor = 0xFFFFFFFF.toInt()
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
    val horizontalMarginPercent: Float = 0.10449f,
    val bottomMarginPercent: Float = 0.02083f,
    val topMarginPercent: Float = 0.02083f,
    val gapBetweenMapAndPanelPercent: Float = 0.01660f,
    val panelWidthPercent: Float = 0.58203f,
    val panelHeightPercent: Float = 0.25781f,
    val cornerRadiusPercent: Float = 0.01563f,
    val backgroundColor: Int = 0xCC202225.toInt(),
    val paddingLeftPercent: Float = 0.01563f,
    val paddingRightPercent: Float = 0.01563f,
    val paddingTopPercent: Float = 0.01660f,
    val paddingBottomPercent: Float = 0.01465f,
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
    val widthPercent: Float = 0.19336f,
    val heightPercent: Float = 0.25781f,
    val cornerRadiusPercent: Float = 0.01563f,
    @Transient val satelliteBitmap: Bitmap? = null,
    val mapType: AppMapType = AppMapType.SATELLITE,
    val showMarker: Boolean = true,
    val markerColor: Int = 0xFFE53935.toInt(),
    val markerCenterColor: Int = 0xFFFFFFFF.toInt(),
    val markerWidthPercent: Float = 0.0295f,
    val markerHeightPercent: Float = 0.0506f,
    val showGoogleLogo: Boolean = true,
    val showRadarBeam: Boolean = true
) : Serializable

/**
 * Location data displayed in heading, address, and coordinates lines.
 */
data class LocationData(
    val heading: String = "Pune, Maharashtra, India",
    val flagEmoji: String = "🇮🇳",
    val postalAddress: String = "870/7, Bhandarkar Rd, Near Hdfc Bank, Shreeman Society, Deccan Gymkhana, Pune, Maharashtra 411004, India",
    val addressLines: List<String> = emptyList(),
    val latitude: Double = 18.518084,
    val longitude: Double = 73.835936,
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
        get() {
            val trimmed = heading.trim()
            return when {
                trimmed.contains("🇮🇳") || trimmed.contains("🇺🇸") || trimmed.contains("🇬🇧") -> trimmed
                flagEmoji.isNotBlank() && (trimmed.endsWith("India", ignoreCase = true) || trimmed.contains("India", ignoreCase = true)) -> "$trimmed $flagEmoji"
                flagEmoji.isNotBlank() -> "$trimmed $flagEmoji"
                else -> trimmed
            }
        }
}

/**
 * Date and time components allowing independent editing.
 */
data class DateTimeConfig(
    val day: String = "Monday",
    val date: String = "19/10/2026",
    val time: String = "12:18 PM",
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
 * Top-right badge sitting atop the main panel ("GPS Map Camera").
 */
data class BadgeConfig(
    val visible: Boolean = true,
    val text: String = "GPS Map Camera",
    @Transient val customIconBitmap: Bitmap? = null,
    val widthPercent: Float = 0.15430f,
    val heightPercent: Float = 0.04427f,
    val cornerRadiusPercent: Float = 0.00879f,
    val backgroundColor: Int = 0xE0222428.toInt(),
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
    val headingFontSizePercent: Float = 0.02980f,
    val bodyFontSizePercent: Float = 0.02003f,
    val badgeFontSizePercent: Float = 0.01319f,
    val headingColor: Int = 0xFFFFFFFF.toInt(),
    val bodyColor: Int = 0xFFFFFFFF.toInt(),
    val badgeTextColor: Int = 0xFFFFFFFF.toInt(),
    val lineSpacingMultiplier: Float = 1.0f,
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
