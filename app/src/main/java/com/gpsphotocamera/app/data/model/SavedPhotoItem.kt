package com.gpsphotocamera.app.data.model

/**
 * Representation of a saved photograph stored in gallery and tracked in recent history.
 */
data class SavedPhotoItem(
    val id: String,
    val contentUri: String,
    val filePath: String?,
    val locationHeading: String,
    val coordinates: String,
    val timestamp: Long,
    val formattedDate: String
)
