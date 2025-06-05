package com.cloud.sync.zother

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler

class NewPhotoContentObserver(handler: Handler, private val context: Context) : ContentObserver(handler) {

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
//            com.cloud.photo_optimizer.services.OptimizerService.processPhoto(uri, context)
    }
}