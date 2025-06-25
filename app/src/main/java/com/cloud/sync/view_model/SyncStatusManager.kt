package com.cloud.sync.view_model;

import com.cloud.sync.data.SyncProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A singleton object to manage and broadcast the state of the full scan.
 * The Service writes to it, and the ViewModel reads from it.
 */
object SyncStatusManager {
private val _progress = MutableStateFlow(SyncProgress())
val progress = _progress.asStateFlow()

fun update(isSyncing: Boolean, text: String) {
    _progress.value = SyncProgress(isSyncing, text)
}

fun isSyncing(): Boolean {
    return _progress.value.isSyncing
}
}