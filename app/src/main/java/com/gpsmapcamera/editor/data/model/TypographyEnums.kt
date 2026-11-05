package com.gpsmapcamera.editor.data.model

import androidx.compose.ui.text.font.FontFamily

enum class FontFamilyOption(val label: String, val composeFontFamily: FontFamily) {
    DEFAULT("System Default", FontFamily.Default),
    SERIF("Classic Serif", FontFamily.Serif),
    SANS_SERIF("Modern Sans", FontFamily.SansSerif),
    MONOSPACE("Digital Monospace", FontFamily.Monospace),
    CURSIVE("Cursive Script", FontFamily.Cursive)
}

enum class OverlayTextAlign(val label: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right")
}
