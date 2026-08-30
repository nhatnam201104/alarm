package com.risealarm.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ExerciseType {
    PushUp,
    SitUp,
    SquatHold,
    Plank,
}

@Serializable
data class ExerciseChallenge(
    val exercise: ExerciseType,
    val target: Int,
    val calibrationVersion: Int = 0,
    val calibratedAtEpochMillis: Long? = null,
) {
    val isHold: Boolean get() = exercise == ExerciseType.SquatHold || exercise == ExerciseType.Plank
    val isCalibrated: Boolean get() = calibrationVersion > 0 && calibratedAtEpochMillis != null

    init {
        require(target in if (isHold) 5..120 else 1..50) {
            if (isHold) "hold target must be in 5..120 seconds" else "rep target must be in 1..50"
        }
    }
}

@Serializable
data class AlarmDefinition(
    val id: String,
    val hour: Int,
    val minute: Int,
    val label: String,
    val repeatDays: Set<Int> = emptySet(),
    val vibration: Boolean = true,
    val enabled: Boolean,
    val challenge: ExerciseChallenge? = null,
) {
    init {
        require(hour in 0..23) { "hour must be in 0..23" }
        require(minute in 0..59) { "minute must be in 0..59" }
        require(repeatDays.all { it in 1..7 }) { "repeatDays must use ISO 1..7" }
    }
}

@Serializable
enum class WakeSessionPhase { Starting, Exercise, QrFallback, StepFallback, ShakeFallback, MathFallback, Completed, Superseded }

@Serializable
enum class TechnicalFailureReason { ModelInitialization, Inference, CameraPipeline, Thermal, PermissionMissing, HardwareUnavailable }

@Serializable
data class WakeSession(
    val sessionId: String,
    val alarmId: String,
    val firedAtEpochMillis: Long,
    val challenge: ExerciseChallenge?,
    val phase: WakeSessionPhase = WakeSessionPhase.Starting,
    val progress: Int = 0,
    val fallbackReason: TechnicalFailureReason? = null,
    val revision: Long = 0,
    val lastUpdatedEpochMillis: Long = firedAtEpochMillis,
)

interface WakeSessionRepository {
    fun active(): WakeSession?
    fun replace(session: WakeSession): WakeSession
    fun update(sessionId: String, expectedRevision: Long, transform: (WakeSession) -> WakeSession): WakeSession?
    fun clear(sessionId: String): Boolean
}

object WakeSessionPolicy {
    fun authorizeFallback(
        session: WakeSession,
        reason: TechnicalFailureReason,
        cameraUsable: Boolean,
        qrEnrolled: Boolean,
        stepSensorAvailable: Boolean,
        shakeSensorAvailable: Boolean,
        nowEpochMillis: Long,
    ): WakeSession {
        val next = when {
            cameraUsable && qrEnrolled -> WakeSessionPhase.QrFallback
            stepSensorAvailable -> WakeSessionPhase.StepFallback
            shakeSensorAvailable -> WakeSessionPhase.ShakeFallback
            else -> WakeSessionPhase.MathFallback
        }
        return session.copy(
            phase = next,
            progress = 0,
            fallbackReason = reason,
            revision = session.revision + 1,
            lastUpdatedEpochMillis = nowEpochMillis,
        )
    }

    fun progress(session: WakeSession, value: Int, nowEpochMillis: Long): WakeSession =
        session.copy(progress = value.coerceAtLeast(session.progress), revision = session.revision + 1, lastUpdatedEpochMillis = nowEpochMillis)

    fun complete(session: WakeSession, nowEpochMillis: Long): WakeSession =
        session.copy(phase = WakeSessionPhase.Completed, revision = session.revision + 1, lastUpdatedEpochMillis = nowEpochMillis)
}

interface AlarmRepository {
    fun all(): List<AlarmDefinition>
    fun find(id: String): AlarmDefinition?
    fun upsert(alarm: AlarmDefinition): Boolean
    fun delete(id: String): Boolean
    fun setEnabled(id: String, enabled: Boolean): AlarmDefinition?
}
