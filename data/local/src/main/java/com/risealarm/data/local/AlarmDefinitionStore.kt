package com.risealarm.data.local

import android.content.Context
import com.risealarm.domain.AlarmDefinition
import com.risealarm.domain.AlarmRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AlarmSnapshot(
    val schemaVersion: Int = 1,
    val alarms: List<AlarmDefinition> = emptyList(),
)

class AlarmJsonCodec(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
) {
    fun encode(alarms: List<AlarmDefinition>): String =
        json.encodeToString(AlarmSnapshot(alarms = alarms))

    fun decode(value: String?): List<AlarmDefinition> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<AlarmSnapshot>(value).alarms }
            .getOrDefault(emptyList())
    }
}

class AlarmDefinitionStore(
    context: Context,
    private val codec: AlarmJsonCodec = AlarmJsonCodec(),
) : AlarmRepository {
    private val storageContext = context.createDeviceProtectedStorageContext()
    private val preferences = storageContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    @Synchronized
    override fun all(): List<AlarmDefinition> =
        codec.decode(preferences.getString(KEY_SNAPSHOT, null))
            .sortedWith(compareBy(AlarmDefinition::hour, AlarmDefinition::minute, AlarmDefinition::id))

    @Synchronized
    override fun find(id: String): AlarmDefinition? = all().firstOrNull { it.id == id }

    @Synchronized
    override fun upsert(alarm: AlarmDefinition): Boolean {
        val updated = all().filterNot { it.id == alarm.id } + alarm
        return persist(updated)
    }

    @Synchronized
    override fun delete(id: String): Boolean {
        val current = all()
        val updated = current.filterNot { it.id == id }
        return if (updated.size == current.size) true else persist(updated)
    }

    @Synchronized
    override fun setEnabled(id: String, enabled: Boolean): AlarmDefinition? {
        val current = all()
        val existing = current.firstOrNull { it.id == id } ?: return null
        val updatedAlarm = existing.copy(enabled = enabled)
        return if (persist(current.map { if (it.id == id) updatedAlarm else it })) updatedAlarm else null
    }

    private fun persist(alarms: List<AlarmDefinition>): Boolean =
        preferences.edit().putString(KEY_SNAPSHOT, codec.encode(alarms)).commit()

    private companion object {
        const val FILE_NAME = "rise_alarm_definitions"
        const val KEY_SNAPSHOT = "snapshot_v1"
    }
}
