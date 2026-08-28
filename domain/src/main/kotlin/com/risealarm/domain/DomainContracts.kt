package com.risealarm.domain

import com.risealarm.core.model.AlarmId

data class AlarmSummary(
    val id: AlarmId,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean,
)

interface AlarmRepository {
    suspend fun alarms(): List<AlarmSummary>
}

