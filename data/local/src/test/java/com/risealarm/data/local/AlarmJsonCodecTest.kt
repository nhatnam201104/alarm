package com.risealarm.data.local

import com.risealarm.domain.AlarmDefinition
import com.risealarm.domain.ExerciseChallenge
import com.risealarm.domain.ExerciseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmJsonCodecTest {
    private val codec = AlarmJsonCodec()

    @Test
    fun roundTrip_preservesAlarmDefinitions() {
        val alarms = listOf(
            AlarmDefinition("work", 6, 30, "Đi làm", setOf(1, 2, 3, 4, 5), true, true),
            AlarmDefinition("once", 8, 5, "Một lần", emptySet(), false, false),
        )

        assertEquals(alarms, codec.decode(codec.encode(alarms)))
    }

    @Test
    fun corruptSnapshot_returnsEmptyList() {
        assertEquals(emptyList<AlarmDefinition>(), codec.decode("not-json"))
    }

    @Test
    fun versionOneSnapshot_migratesAsLegacyManualAlarm() {
        val decoded = codec.decode(
            """{"schemaVersion":1,"alarms":[{"id":"legacy","hour":6,"minute":15,"label":"Cũ","repeatDays":[],"vibration":true,"enabled":true}]}""",
        )

        assertEquals(1, decoded.size)
        assertNull(decoded.single().challenge)
    }

    @Test
    fun versionTwoRoundTrip_preservesExerciseChallenge() {
        val alarm = AlarmDefinition(
            id = "vision",
            hour = 6,
            minute = 30,
            label = "Plank",
            enabled = true,
            challenge = ExerciseChallenge(
                exercise = ExerciseType.Plank,
                target = 20,
                calibrationVersion = 1,
                calibratedAtEpochMillis = 1234L,
            ),
        )

        assertEquals(alarm, codec.decode(codec.encode(listOf(alarm))).single())
    }

    @Test
    fun futureSnapshot_isRejectedWithoutPartialDecode() {
        val future = """{"schemaVersion":99,"alarms":[]}"""
        assertEquals(emptyList<AlarmDefinition>(), codec.decode(future))
    }
}
