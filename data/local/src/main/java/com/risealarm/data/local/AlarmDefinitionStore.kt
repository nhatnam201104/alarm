package com.risealarm.data.local

import android.content.Context
import com.risealarm.domain.AlarmDefinition
import com.risealarm.domain.AlarmRepository
import com.risealarm.domain.WakeSession
import com.risealarm.domain.WakeSessionRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AlarmSnapshot(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val alarms: List<AlarmDefinition> = emptyList(),
) {
    companion object { const val CURRENT_SCHEMA_VERSION = 2 }
}

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
        return runCatching {
            val snapshot = json.decodeFromString<AlarmSnapshot>(value)
            if (snapshot.schemaVersion > AlarmSnapshot.CURRENT_SCHEMA_VERSION) emptyList() else snapshot.alarms
        }
            .getOrDefault(emptyList())
    }
}

class WakeSessionStore private constructor(
    context: Context,
    private val json: Json,
) : WakeSessionRepository {
    constructor(context: Context) : this(context, Json { ignoreUnknownKeys = true; encodeDefaults = true })
    private val preferences = context.createDeviceProtectedStorageContext()
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    @Synchronized
    override fun active(): WakeSession? = preferences.getString(KEY_ACTIVE, null)?.let { value ->
        runCatching { json.decodeFromString<WakeSession>(value) }.getOrNull()
    }

    @Synchronized
    override fun replace(session: WakeSession): WakeSession {
        val current = active()
        if (current != null && current.sessionId != session.sessionId) {
            val superseded = current.copy(
                phase = com.risealarm.domain.WakeSessionPhase.Superseded,
                revision = current.revision + 1,
                lastUpdatedEpochMillis = session.firedAtEpochMillis,
            )
            preferences.edit().putString(KEY_LAST_SUPERSEDED, json.encodeToString(superseded)).commit()
        }
        preferences.edit().putString(KEY_ACTIVE, json.encodeToString(session)).commit()
        return session
    }

    @Synchronized
    override fun update(
        sessionId: String,
        expectedRevision: Long,
        transform: (WakeSession) -> WakeSession,
    ): WakeSession? {
        val current = active() ?: return null
        if (current.sessionId != sessionId || current.revision != expectedRevision) return null
        val updated = transform(current)
        if (updated.sessionId != current.sessionId || updated.revision <= current.revision) return null
        return if (preferences.edit().putString(KEY_ACTIVE, json.encodeToString(updated)).commit()) updated else null
    }

    @Synchronized
    override fun clear(sessionId: String): Boolean {
        val current = active() ?: return true
        if (current.sessionId != sessionId) return false
        return preferences.edit().remove(KEY_ACTIVE).commit()
    }

    private companion object {
        const val FILE_NAME = "rise_wake_session"
        const val KEY_ACTIVE = "active_v1"
        const val KEY_LAST_SUPERSEDED = "last_superseded_v1"
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
