package com.cloud.sync.domain.repositroy

import com.cloud.sync.domain.model.TimeInterval
import kotlinx.coroutines.flow.Flow

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