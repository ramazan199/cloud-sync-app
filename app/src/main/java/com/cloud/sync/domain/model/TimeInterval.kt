package com.cloud.sync.domain.model

import kotlinx.serialization.Serializable

// Data class to hold a synced time interval.
@Serializable
data class TimeInterval(val start: Long, val end: Long)
