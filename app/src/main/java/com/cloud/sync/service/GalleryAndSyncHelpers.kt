package com.cloud.sync.service

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.cloud.sync.data.GalleryPhoto
import com.cloud.sync.data.TimeInterval
import kotlinx.coroutines.delay

class GalleryAndSyncHelpers(private val context: Context) {
    // Simulates uploading a photo. later change to use communication library
    suspend fun uploadPhoto(photo: GalleryPhoto) {
        println("Uploading photo: ${photo.displayName} (Timestamp: ${photo.dateAdded})")
        delay(500) // Simulate network latency
        println("✅ Uploaded ${photo.displayName}")
    }

    fun getPhotos(startTimeSeconds: Long = 0): List<GalleryPhoto> {
        return queryPhotos(startTimeSeconds)
    }

    fun getPhotosInInterval(start: Long, end: Long): List<GalleryPhoto> {
        if (start > end) return emptyList()
        return queryPhotos(start).filter { it.dateAdded <= end }
    }

    fun queryPhotos(
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


        // --- DYNAMICALLY BUILD THE SELECTION CLAUSE ---
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


        val sortOrder =
            "${MediaStore.Images.Media.DATE_ADDED} ASC" // Sort by date added (ascending)

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

    fun mergeIntervals(intervals: List<TimeInterval>): List<TimeInterval> {
        if (intervals.isEmpty()) return emptyList()
        val sorted = intervals.sortedBy { it.start }
        val merged = mutableListOf<TimeInterval>()
        var current = sorted.first()
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.start <= current.end + 1) {
                current = current.copy(end = maxOf(current.end, next.end))
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }
}



