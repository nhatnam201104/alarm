package com.risealarm.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WakeSessionPolicyTest {
    private val session = WakeSession(
        sessionId = "session",
        alarmId = "alarm",
        firedAtEpochMillis = 100L,
        challenge = ExerciseChallenge(ExerciseType.PushUp, 10),
    )

    @Test
    fun fallbackPrefersQrOnlyWhenCameraAndEnrollmentAreUsable() {
        val result = WakeSessionPolicy.authorizeFallback(
            session = session,
            reason = TechnicalFailureReason.Inference,
            cameraUsable = true,
            qrEnrolled = true,
            stepSensorAvailable = true,
            shakeSensorAvailable = true,
            nowEpochMillis = 200L,
        )

        assertEquals(WakeSessionPhase.QrFallback, result.phase)
        assertEquals(TechnicalFailureReason.Inference, result.fallbackReason)
        assertEquals(1L, result.revision)
    }

    @Test
    fun fallbackUsesStepsThenShakeThenMathByCapability() {
        val steps = WakeSessionPolicy.authorizeFallback(
            session, TechnicalFailureReason.CameraPipeline,
            cameraUsable = false, qrEnrolled = true,
            stepSensorAvailable = true, shakeSensorAvailable = true,
            nowEpochMillis = 200L,
        )
        val shake = WakeSessionPolicy.authorizeFallback(
            session, TechnicalFailureReason.HardwareUnavailable,
            cameraUsable = false, qrEnrolled = false,
            stepSensorAvailable = false, shakeSensorAvailable = true,
            nowEpochMillis = 200L,
        )
        val math = WakeSessionPolicy.authorizeFallback(
            session, TechnicalFailureReason.PermissionMissing,
            cameraUsable = false, qrEnrolled = false,
            stepSensorAvailable = false, shakeSensorAvailable = false,
            nowEpochMillis = 200L,
        )

        assertEquals(WakeSessionPhase.StepFallback, steps.phase)
        assertEquals(WakeSessionPhase.ShakeFallback, shake.phase)
        assertEquals(WakeSessionPhase.MathFallback, math.phase)
    }

    @Test
    fun progressNeverMovesBackwards() {
        val progressed = WakeSessionPolicy.progress(session.copy(progress = 7), 4, 300L)
        assertEquals(7, progressed.progress)
        assertEquals(1L, progressed.revision)
    }
}
