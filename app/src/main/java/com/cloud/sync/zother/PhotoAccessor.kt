package com.cloud.sync.zother

import android.content.Context
import android.os.Environment

class PhotoAccessor(private val context: Context) {


    companion object {

        @JvmStatic
        fun accessDefaultPhotos(context: Context) {
            try {
                // Get the default public Pictures directory
                val picturesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM.plus("/Camera")
                )

                // Alternative: Get app-specific pictures directory
                // val appPicturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

                if (picturesDir == null || !picturesDir.exists()) {
                    println("Default photos directory doesn't exist")
                    return
                }

                println("Accessing photos directory: ${picturesDir.absolutePath}")

                // List files
                val photoFiles = picturesDir.listFiles()
                photoFiles?.forEach { file ->
                    println("Found file: ${file.name}")
                    // Just accessing the file, not reading content
                    file.inputStream().use { } // Open and immediately close
                }

            } catch (e: Exception) {
                println("Error accessing photos: ${e.message}")
            }
        }
    }
}