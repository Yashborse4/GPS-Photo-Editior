package com.gpsmapcamera.editor.data.model

import android.graphics.Bitmap

enum class ExportFormat(val extension: String, val mimeType: String, val compressFormat: Bitmap.CompressFormat) {
    JPEG("jpg", "image/jpeg", Bitmap.CompressFormat.JPEG),
    PNG("png", "image/png", Bitmap.CompressFormat.PNG)
}
