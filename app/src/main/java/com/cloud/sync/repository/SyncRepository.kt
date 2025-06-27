package com.cloud.sync.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.cloud.sync.data.TimeInterval
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Interface defining the contract for managing synchronization data.
 * This includes storing and retrieving information about synced time intervals
 * and the starting point for future "sync from now" operations.
 */
interface ISyncRepository {
    val syncedIntervals: Flow<List<TimeInterval>>

    /**
     * Saves a list of time intervals that have been successfully synced.
     * @param intervals: The list of time intervals to be saved.
     */
    suspend fun saveSyncedIntervals(intervals: List<TimeInterval>)

    val syncFromNowPoint: Flow<Long>
    suspend fun saveSyncFromNowPoint(timestamp: Long)
    suspend fun deleteSyncFromNowPoint()
    suspend fun clearAllData()
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_settings")

class SyncRepositoryImpl @Inject constructor(
    private val context: Context
) : ISyncRepository {

    companion object {
        private val SYNC_INTERVALS_KEY = stringPreferencesKey("sync_intervals")
        private val SYNC_FROM_NOW_POINT_KEY = longPreferencesKey("sync_from_now_point")
    }

    override val syncedIntervals: Flow<List<TimeInterval>> = context.dataStore.data.map { prefs ->
        Json.decodeFromString<List<TimeInterval>>(prefs[SYNC_INTERVALS_KEY] ?: "[]")
    }

    override suspend fun saveSyncedIntervals(intervals: List<TimeInterval>) {
        context.dataStore.edit { it[SYNC_INTERVALS_KEY] = Json.encodeToString(intervals) }
    }

    override val syncFromNowPoint: Flow<Long> =
        context.dataStore.data.map { it[SYNC_FROM_NOW_POINT_KEY] ?: 0L }

    override suspend fun saveSyncFromNowPoint(timestamp: Long) {
        context.dataStore.edit { it[SYNC_FROM_NOW_POINT_KEY] = timestamp }
    }

    override suspend fun deleteSyncFromNowPoint() {
        context.dataStore.edit { it.remove(SYNC_FROM_NOW_POINT_KEY) }
    }

    override suspend fun clearAllData() {
        context.dataStore.edit { it.clear() }
    }
}
