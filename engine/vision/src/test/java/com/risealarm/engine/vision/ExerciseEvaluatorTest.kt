package com.risealarm.engine.vision

import com.risealarm.domain.ExerciseChallenge
import com.risealarm.domain.ExerciseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExerciseEvaluatorTest {
    @Test fun angleForStraightLineIs180() {
        assertEquals(180f, angle(PosePoint(0f, 0f), PosePoint(1f, 0f), PosePoint(2f, 0f)), 0.01f)
    }

    @Test fun missingLandmarksNeverProgressHold() {
        val evaluator = evaluatorFor(ExerciseChallenge(ExerciseType.Plank, 20))
        val result = evaluator.evaluate(PoseObservation(1_000, emptyMap()))
        assertEquals(0, result.value)
        assertFalse(result.completed)
        assertEquals(TrackingQuality.Reframe, result.quality)
    }
}
