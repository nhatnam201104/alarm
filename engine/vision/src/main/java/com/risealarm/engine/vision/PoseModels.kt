package com.risealarm.engine.vision

import com.risealarm.domain.ExerciseChallenge
import com.risealarm.domain.ExerciseType
import kotlin.math.acos
import kotlin.math.sqrt

enum class BodyLandmark(val index: Int) {
    Nose(0), LeftShoulder(11), RightShoulder(12), LeftElbow(13), RightElbow(14),
    LeftWrist(15), RightWrist(16), LeftHip(23), RightHip(24), LeftKnee(25),
    RightKnee(26), LeftAnkle(27), RightAnkle(28),
}

data class PosePoint(val x: Float, val y: Float, val z: Float = 0f, val visibility: Float = 1f)

data class PoseObservation(
    val timestampMillis: Long,
    val landmarks: Map<BodyLandmark, PosePoint>,
    val inferenceMillis: Long = 0,
) {
    operator fun get(landmark: BodyLandmark): PosePoint? = landmarks[landmark]
}

enum class TrackingQuality { Searching, Reframe, Valid }

data class ExerciseProgress(
    val value: Int,
    val target: Int,
    val quality: TrackingQuality,
    val instruction: String,
    val completed: Boolean = value >= target,
)

interface ExerciseEvaluator {
    fun evaluate(observation: PoseObservation): ExerciseProgress
    fun reset()
}

fun evaluatorFor(challenge: ExerciseChallenge): ExerciseEvaluator = when (challenge.exercise) {
    ExerciseType.Plank -> HoldEvaluator(challenge, HoldKind.Plank)
    ExerciseType.SquatHold -> HoldEvaluator(challenge, HoldKind.Squat)
    ExerciseType.PushUp -> RepEvaluator(challenge, RepKind.PushUp)
    ExerciseType.SitUp -> RepEvaluator(challenge, RepKind.SitUp)
}

internal fun angle(a: PosePoint, vertex: PosePoint, c: PosePoint): Float {
    val abx = a.x - vertex.x
    val aby = a.y - vertex.y
    val cbx = c.x - vertex.x
    val cby = c.y - vertex.y
    val denominator = sqrt((abx * abx + aby * aby) * (cbx * cbx + cby * cby)).coerceAtLeast(1e-6f)
    val cosine = ((abx * cbx + aby * cby) / denominator).coerceIn(-1f, 1f)
    return Math.toDegrees(acos(cosine).toDouble()).toFloat()
}

internal data class SideJoints(
    val shoulder: PosePoint,
    val elbow: PosePoint,
    val wrist: PosePoint,
    val hip: PosePoint,
    val knee: PosePoint,
    val ankle: PosePoint,
)

internal fun PoseObservation.bestSide(minVisibility: Float = 0.55f): SideJoints? {
    fun side(left: Boolean): SideJoints? {
        val values = if (left) listOf(
            BodyLandmark.LeftShoulder, BodyLandmark.LeftElbow, BodyLandmark.LeftWrist,
            BodyLandmark.LeftHip, BodyLandmark.LeftKnee, BodyLandmark.LeftAnkle,
        ) else listOf(
            BodyLandmark.RightShoulder, BodyLandmark.RightElbow, BodyLandmark.RightWrist,
            BodyLandmark.RightHip, BodyLandmark.RightKnee, BodyLandmark.RightAnkle,
        )
        val points = values.map { this[it] ?: return null }
        if (points.any { it.visibility < minVisibility }) return null
        return SideJoints(points[0], points[1], points[2], points[3], points[4], points[5])
    }
    val left = side(true)
    val right = side(false)
    return when {
        left == null -> right
        right == null -> left
        listOf(left.shoulder, left.elbow, left.wrist, left.hip, left.knee, left.ankle).sumOf { it.visibility.toDouble() } >=
            listOf(right.shoulder, right.elbow, right.wrist, right.hip, right.knee, right.ankle).sumOf { it.visibility.toDouble() } -> left
        else -> right
    }
}

internal enum class HoldKind { Plank, Squat }

internal class HoldEvaluator(
    private val challenge: ExerciseChallenge,
    private val kind: HoldKind,
) : ExerciseEvaluator {
    private var validMillis = 0L
    private var lastTimestamp: Long? = null
    private var wasValid = false

    override fun evaluate(observation: PoseObservation): ExerciseProgress {
        val side = observation.bestSide()
            ?: return progress(TrackingQuality.Reframe, "Đưa toàn thân vào khung").also {
                lastTimestamp = null
                wasValid = false
            }
        val valid = when (kind) {
            HoldKind.Plank -> angle(side.shoulder, side.hip, side.ankle) >= 155f &&
                angle(side.shoulder, side.elbow, side.wrist) in 65f..120f
            HoldKind.Squat -> angle(side.hip, side.knee, side.ankle) in 65f..120f &&
                side.hip.y > side.knee.y - 0.12f
        }
        val previous = lastTimestamp
        if (valid && wasValid && previous != null) validMillis += (observation.timestampMillis - previous).coerceIn(0, 250)
        lastTimestamp = observation.timestampMillis
        wasValid = valid
        return progress(
            if (valid) TrackingQuality.Valid else TrackingQuality.Reframe,
            if (valid) "Giữ vững tư thế" else if (kind == HoldKind.Plank) "Giữ vai, hông và chân thẳng" else "Hạ hông và giữ đầu gối ổn định",
        )
    }

    private fun progress(quality: TrackingQuality, instruction: String) = ExerciseProgress(
        value = (validMillis / 1_000L).toInt(),
        target = challenge.target,
        quality = quality,
        instruction = instruction,
    )

    override fun reset() { validMillis = 0; lastTimestamp = null; wasValid = false }
}

internal enum class RepKind { PushUp, SitUp }
private enum class RepPhase { SeekingStart, Start, Middle }

internal class RepEvaluator(
    private val challenge: ExerciseChallenge,
    private val kind: RepKind,
) : ExerciseEvaluator {
    private var count = 0
    private var phase = RepPhase.SeekingStart
    private var phaseSince = 0L

    override fun evaluate(observation: PoseObservation): ExerciseProgress {
        val side = observation.bestSide()
            ?: return ExerciseProgress(count, challenge.target, TrackingQuality.Reframe, "Đưa toàn thân vào khung")
        val bodyLine = angle(side.shoulder, side.hip, side.ankle)
        val metric = when (kind) {
            RepKind.PushUp -> angle(side.shoulder, side.elbow, side.wrist)
            RepKind.SitUp -> angle(side.shoulder, side.hip, side.knee)
        }
        val start = when (kind) { RepKind.PushUp -> metric >= 155f && bodyLine >= 145f; RepKind.SitUp -> metric >= 135f }
        val middle = when (kind) { RepKind.PushUp -> metric <= 105f && bodyLine >= 140f; RepKind.SitUp -> metric <= 90f }
        when (phase) {
            RepPhase.SeekingStart -> if (start) enter(RepPhase.Start, observation.timestampMillis)
            RepPhase.Start -> if (middle && observation.timestampMillis - phaseSince >= 250) enter(RepPhase.Middle, observation.timestampMillis)
            RepPhase.Middle -> if (start && observation.timestampMillis - phaseSince >= 250) {
                count++
                enter(RepPhase.Start, observation.timestampMillis)
            }
        }
        val instruction = when {
            phase == RepPhase.SeekingStart -> if (kind == RepKind.PushUp) "Vào tư thế chống đẩy thẳng người" else "Nằm xuống để bắt đầu"
            phase == RepPhase.Start -> if (kind == RepKind.PushUp) "Hạ người xuống" else "Gập người lên"
            else -> if (kind == RepKind.PushUp) "Đẩy người lên hết biên độ" else "Hạ người về vị trí ban đầu"
        }
        return ExerciseProgress(count, challenge.target, TrackingQuality.Valid, instruction)
    }

    private fun enter(next: RepPhase, timestamp: Long) { phase = next; phaseSince = timestamp }
    override fun reset() { count = 0; phase = RepPhase.SeekingStart; phaseSince = 0 }
}
