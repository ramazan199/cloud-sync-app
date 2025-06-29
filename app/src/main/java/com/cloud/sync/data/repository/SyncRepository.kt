package com.cloud.sync.data.repository

import com.cloud.sync.data.local.datastore.SyncPreferencesDataSource
import com.cloud.sync.domain.model.TimeInterval
import com.cloud.sync.domain.repositroy.ISyncRepository
import javax.inject.Inject

class SyncRepositoryImpl @Inject constructor(
    private val prefs: SyncPreferencesDataSource
) : ISyncRepository {

    override val syncedIntervals = prefs.syncedIntervals

    override suspend fun saveSyncedIntervals(intervals: List<TimeInterval>) {
        prefs.saveSyncedIntervals(intervals)
    }

    override val syncFromNowPoint = prefs.syncFromNowPoint

    override suspend fun saveSyncFromNowPoint(timestamp: Long) {
        prefs.saveSyncFromNowPoint(timestamp)
    }

    override suspend fun deleteSyncFromNowPoint() {
        prefs.deleteSyncFromNowPoint()
    }

    override suspend fun clearAllData() {
        prefs.clearAllData()
    }
}
