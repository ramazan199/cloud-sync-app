package com.cloud.sync.data.repository

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.cloud.sync.data.GalleryPhoto
import javax.inject.Inject

/**
 * Interface for accessing photos from the device's gallery.
 */
interface IGalleryRepository {
    /**
     * Get photos from the gallery starting from a specific timestamp (seconds).
     * @param startTimeSeconds: Time from which to fetch photos (default is 0 for all).
     * @return List<GalleryPhoto>: A list of photos matching the time filter.
     */
    fun getPhotos(startTimeSeconds: Long = 0): List<GalleryPhoto>

    /**
     * Get photos from the gallery within a specific time interval.
     * @param start: Start timestamp in seconds.
     * @param end: End timestamp in seconds.
     * @return List<GalleryPhoto>: A list of photos within the given interval.
     */
    fun getPhotosInInterval(start: Long, end: Long): List<GalleryPhoto>

}

class GalleryRepositoryImpl @Inject constructor( private val context: Context) : IGalleryRepository {

    override fun getPhotos(startTimeSeconds: Long): List<GalleryPhoto> {
        return queryPhotos(startTimeSeconds = startTimeSeconds)
    }

    override fun getPhotosInInterval(start: Long, end: Long): List<GalleryPhoto> {
        if (start > end) return emptyList()
        return queryPhotos(startTimeSeconds = start, endTimeSeconds = end)
    }

    /**
     * Queries the gallery for photos based on the provided time filters.
     * @param startTimeSeconds: Start timestamp in seconds (optional).
     * @param endTimeSeconds: End timestamp in seconds (optional).
     * @return List<GalleryPhoto>: A list of photos matching the criteria.
     */
    private fun queryPhotos(
        startTimeSeconds: Long? = null,
        endTimeSeconds: Long? = null
    ): List<GalleryPhoto> {
        val photos = mutableListOf<GalleryPhoto>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATA
        )

        // Dynamically build the selection clause based on time filters
        val selectionParts = mutableListOf<String>()
        val selectionArgsList = mutableListOf<String>()

        startTimeSeconds?.let {
            selectionParts.add("${MediaStore.Images.Media.DATE_ADDED} >= ?")
            selectionArgsList.add(it.toString())
        }

        endTimeSeconds?.let {
            selectionParts.add("${MediaStore.Images.Media.DATE_ADDED} <= ?")
            selectionArgsList.add(it.toString())
        }

        selectionParts.add("${MediaStore.Images.Media.DATA} LIKE ?")
        val dcimCameraPath =
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath}/Camera"
        selectionArgsList.add("$dcimCameraPath%")

        val selection = selectionParts.joinToString(" AND ")
        val selectionArgs = selectionArgsList.toTypedArray()

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} ASC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                photos.add(
                    GalleryPhoto(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)),
                        dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)),
                        displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                    )
                )
            }
        }

        return photos
    }
}
