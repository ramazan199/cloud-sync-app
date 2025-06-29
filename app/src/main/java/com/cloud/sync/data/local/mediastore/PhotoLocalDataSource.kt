package com.cloud.sync.data.local.mediastore

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.cloud.sync.domain.model.GalleryPhoto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PhotoLocalDataSource @Inject constructor(
    @ApplicationContext  private val context: Context
) {

    fun getPhotos(startTimeSeconds: Long): List<GalleryPhoto> {
        return queryPhotos(startTimeSeconds = startTimeSeconds)
    }

    fun getPhotosInInterval(start: Long, end: Long): List<GalleryPhoto> {
        if (start > end) return emptyList()
        return queryPhotos(startTimeSeconds = start, endTimeSeconds = end)
    }

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
