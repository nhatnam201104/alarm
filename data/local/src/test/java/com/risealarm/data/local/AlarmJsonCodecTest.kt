package com.risealarm.data.local

import com.risealarm.domain.AlarmDefinition
import org.junit.Assert.assertEquals
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
}
