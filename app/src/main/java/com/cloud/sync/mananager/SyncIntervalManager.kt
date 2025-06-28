package com.cloud.sync.mananager

import com.cloud.sync.data.TimeInterval
import javax.inject.Inject

interface ISyncIntervalManager {
    fun mergeIntervals(intervals: List<TimeInterval>): List<TimeInterval>
}

class SyncIntervalManager  @Inject constructor(): ISyncIntervalManager {
    override fun mergeIntervals(intervals: List<TimeInterval>): List<TimeInterval> {
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

