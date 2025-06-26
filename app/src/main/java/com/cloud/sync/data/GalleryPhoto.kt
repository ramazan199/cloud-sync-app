package com.cloud.sync.data

// Data class to represent a photo from the gallery
data class GalleryPhoto(
    val id: Long,
    val dateAdded: Long, // Timestamp in seconds
    val displayName: String
)