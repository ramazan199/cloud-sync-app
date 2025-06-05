package com.cloud.sync.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import javax.inject.Inject

interface IMediaRepository {
    fun getPhotoUris(context: Context): List<Uri>
}

class MediaRepositoryImpl @Inject constructor() : IMediaRepository {
    override fun getPhotoUris(context: Context): List<Uri> {
        val uris = mutableListOf<Uri>()
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val resolver = context.contentResolver

        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                uris.add(
                    ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                )
            }
        }
        return uris
    }
}