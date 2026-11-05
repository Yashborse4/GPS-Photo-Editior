package com.gpsphotocamera.app.image

import android.graphics.Bitmap

/**
 * In-memory holder for the active generated high-resolution photograph.
 * Avoids Android IPC TransactionTooLargeException when navigating between Compose screens.
 */
object BitmapHolder {
    var activeBitmap: Bitmap? = null
    var originalBitmap: Bitmap? = null

    fun clear() {
        activeBitmap = null
        originalBitmap = null
    }
}
