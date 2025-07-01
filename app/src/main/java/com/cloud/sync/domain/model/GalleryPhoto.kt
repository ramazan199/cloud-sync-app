package com.cloud.sync.domain.model

/**
 * Represents a photo from the device's gallery.
 */
data class GalleryPhoto(
    val id: Long,
    val dateAdded: Long, // Timestamp in seconds
    val displayName: String
)