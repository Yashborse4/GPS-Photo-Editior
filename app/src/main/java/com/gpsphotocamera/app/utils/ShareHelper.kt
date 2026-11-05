package com.gpsphotocamera.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Handles invoking Android's system share sheet for sharing generated GPS photographs.
 */
object ShareHelper {

    fun shareImage(
        context: Context,
        imageUri: Uri,
        heading: String = "GPS Map Photograph",
        mimeType: String = "image/jpeg"
    ) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_SUBJECT, heading)
                putExtra(Intent.EXTRA_TEXT, "Captured with GPS Photo Camera - $heading")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share GPS Stamped Photo")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
