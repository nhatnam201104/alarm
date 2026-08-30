package com.risealarm

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.risealarm.core.designsystem.RiseTheme
import com.risealarm.domain.ExerciseType
import com.risealarm.domain.WakeSession
import com.risealarm.domain.WakeSessionPhase
import com.risealarm.domain.WakeSessionPolicy
import com.risealarm.engine.vision.ExerciseProgress
import com.risealarm.engine.vision.TrackingQuality
import com.risealarm.engine.vision.VisionCameraPreview
import com.risealarm.engine.vision.evaluatorFor
import com.risealarm.engine.alarm.MathChallengeGenerator
import com.risealarm.engine.alarm.ShakeFallbackTracker
import com.risealarm.engine.alarm.StepFallbackTracker
import com.risealarm.feature.alarms.displayName
import com.risealarm.feature.wake.*

class AlarmActivity : ComponentActivity() {
    private var activeSession by mutableStateOf<WakeSession?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        loadSession(intent)

        setContent {
            RiseTheme {
                BackHandler(enabled = true) { }
                val session = activeSession
                when {
                    session?.phase == WakeSessionPhase.StepFallback -> StepFallbackContent(session)
                    session?.phase == WakeSessionPhase.ShakeFallback -> ShakeFallbackContent(session)
                    session?.phase == WakeSessionPhase.MathFallback -> MathFallbackContent(session)
                    session?.challenge != null -> VisionWakeContent(session)
                    session != null -> LegacyWakeContent(session)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadSession(intent)
    }

    private fun loadSession(intent: Intent) {
        val requested = intent.getStringExtra(AlarmRingingService.EXTRA_SESSION_ID)
        val stored = (application as RiseApplication).wakeSessionStore.active()
        activeSession = stored?.takeIf { requested == null || it.sessionId == requested }
    }

    @Composable
    private fun LegacyWakeContent(session: WakeSession) {
        val alarm = (application as RiseApplication).alarmStore.find(session.alarmId)
        ManualDismissAlarmScreen(
            time = alarm?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "BÁO THỨC",
            label = alarm?.label.orEmpty(),
            onDismiss = { completeSession(session.sessionId) },
        )
    }

    @Composable
    private fun VisionWakeContent(session: WakeSession) {
        val challenge = session.challenge ?: return
        val evaluator = remember(session.sessionId) { evaluatorFor(challenge) }
        var progress by remember(session.sessionId) {
            mutableStateOf(ExerciseProgress(session.progress, challenge.target, TrackingQuality.Searching, "Đưa toàn thân vào khung"))
        }
        var visionStatus by remember(session.sessionId) { mutableStateOf(VisionStatus.Framing) }

        val camera: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {
            VisionCameraPreview(
                onObservation = { observation ->
                    val next = evaluator.evaluate(observation)
                    runOnUiThread {
                        progress = next
                        visionStatus = if (next.quality == TrackingQuality.Valid) VisionStatus.Valid else VisionStatus.Framing
                        persistProgress(session.sessionId, next.value)
                        if (next.completed) completeSession(session.sessionId)
                    }
                },
                onError = {
                    runOnUiThread {
                        visionStatus = VisionStatus.TechnicalFallback
                        routeTechnicalFallback(session.sessionId)
                    }
                },
                modifier = Modifier.matchParentSize(),
            )
        }

        if (challenge.exercise == ExerciseType.Plank || challenge.exercise == ExerciseType.SquatHold) {
            PlankHoldScreen(
                state = HoldUiState(
                    exercise = challenge.exercise.displayName(),
                    elapsed = progress.value,
                    target = progress.target,
                    isValid = progress.quality == TrackingQuality.Valid,
                ),
                onAction = { },
                cameraContent = camera,
            )
        } else {
            ActiveVisionScreen(
                state = ActiveVisionUiState(
                    exercise = challenge.exercise.displayName(),
                    count = progress.value,
                    target = progress.target,
                    instruction = progress.instruction,
                    status = visionStatus,
                    reFireSeconds = 0,
                ),
                onAction = { },
                cameraContent = camera,
            )
        }
    }

    @Composable
    private fun StepFallbackContent(session: WakeSession) {
        var value by remember(session.sessionId) { mutableIntStateOf(session.progress) }
        val tracker = remember(session.sessionId) {
            StepFallbackTracker(this) { count ->
                runOnUiThread {
                    value = count.coerceAtLeast(value)
                    persistProgress(session.sessionId, value)
                    if (value >= STEP_TARGET) completeSession(session.sessionId)
                }
            }
        }
        DisposableEffect(tracker) {
            tracker.start()
            onDispose { tracker.close() }
        }
        FallbackProgressScreen("Đi bộ $STEP_TARGET bước", "Cầm điện thoại và bước đều để chứng minh bạn đã rời giường", value, STEP_TARGET)
    }

    @Composable
    private fun ShakeFallbackContent(session: WakeSession) {
        var value by remember(session.sessionId) { mutableIntStateOf(session.progress) }
        val tracker = remember(session.sessionId) {
            ShakeFallbackTracker(this) { count ->
                runOnUiThread {
                    value = count.coerceAtLeast(value)
                    persistProgress(session.sessionId, value)
                    if (value >= SHAKE_TARGET) completeSession(session.sessionId)
                }
            }
        }
        DisposableEffect(tracker) {
            tracker.start()
            onDispose { tracker.close() }
        }
        FallbackProgressScreen("Lắc máy $SHAKE_TARGET lần", "Giữ chắc điện thoại và lắc theo nhịp", value, SHAKE_TARGET)
    }

    @Composable
    private fun MathFallbackContent(session: WakeSession) {
        val generator = remember { MathChallengeGenerator() }
        var question by remember(session.sessionId) { mutableStateOf(generator.next()) }
        var correct by remember(session.sessionId) { mutableIntStateOf(session.progress) }
        MathFallbackScreen(question.prompt, question.answers, correct, MATH_TARGET) { answer ->
            if (answer == question.correct) {
                correct++
                persistProgress(session.sessionId, correct)
                if (correct >= MATH_TARGET) completeSession(session.sessionId)
            }
            question = generator.next()
        }
    }

    private fun routeTechnicalFallback(sessionId: String) {
        val store = (application as RiseApplication).wakeSessionStore
        val current = store.active() ?: return
        if (current.sessionId != sessionId || current.phase != WakeSessionPhase.Starting && current.phase != WakeSessionPhase.Exercise) return
        val stepPermission = Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        val step = StepFallbackTracker(this) { }
        val shake = ShakeFallbackTracker(this) { }
        val updated = store.update(sessionId, current.revision) {
            WakeSessionPolicy.authorizeFallback(
                session = it,
                reason = com.risealarm.domain.TechnicalFailureReason.CameraPipeline,
                cameraUsable = false,
                qrEnrolled = false,
                stepSensorAvailable = stepPermission && step.available,
                shakeSensorAvailable = shake.available,
                nowEpochMillis = System.currentTimeMillis(),
            )
        }
        step.close()
        shake.close()
        if (updated != null) activeSession = updated
    }

    private fun persistProgress(sessionId: String, value: Int) {
        val store = (application as RiseApplication).wakeSessionStore
        val current = store.active() ?: return
        if (current.sessionId != sessionId || value <= current.progress) return
        val updated = store.update(sessionId, current.revision) { WakeSessionPolicy.progress(it, value, System.currentTimeMillis()) }
        if (updated != null) activeSession = updated
    }

    private fun completeSession(sessionId: String) {
        val store = (application as RiseApplication).wakeSessionStore
        val current = store.active() ?: return
        if (current.sessionId != sessionId || current.phase == WakeSessionPhase.Superseded) return
        store.update(sessionId, current.revision) { WakeSessionPolicy.complete(it, System.currentTimeMillis()) } ?: return
        stopService(AlarmRingingService.stopIntent(this, sessionId))
        finishAndRemoveTask()
    }

    private companion object {
        const val STEP_TARGET = 500
        const val SHAKE_TARGET = 100
        const val MATH_TARGET = 10
    }
}
