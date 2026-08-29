package com.risealarm.domain

import kotlinx.serialization.Serializable

@Serializable
data class AlarmDefinition(
    val id: String,
    val hour: Int,
    val minute: Int,
    val label: String,
    val repeatDays: Set<Int> = emptySet(),
    val vibration: Boolean = true,
    val enabled: Boolean,
) {
    init {
        require(hour in 0..23) { "hour must be in 0..23" }
        require(minute in 0..59) { "minute must be in 0..59" }
        require(repeatDays.all { it in 1..7 }) { "repeatDays must use ISO 1..7" }
    }
}

interface AlarmRepository {
    fun all(): List<AlarmDefinition>
    fun find(id: String): AlarmDefinition?
    fun upsert(alarm: AlarmDefinition): Boolean
    fun delete(id: String): Boolean
    fun setEnabled(id: String, enabled: Boolean): AlarmDefinition?
}
