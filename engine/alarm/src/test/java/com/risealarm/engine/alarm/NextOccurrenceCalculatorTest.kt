package com.risealarm.engine.alarm

import com.risealarm.domain.AlarmDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class NextOccurrenceCalculatorTest {
    private val calculator = NextOccurrenceCalculator()
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")

    @Test
    fun disabledAlarm_hasNoOccurrence() {
        assertNull(calculator.next(alarm(enabled = false), at(2026, 8, 29, 6, 0)))
    }

    @Test
    fun oneTimeFutureToday_usesToday() {
        val result = calculator.next(alarm(hour = 6, minute = 30), at(2026, 8, 29, 6, 0))
        assertEquals(at(2026, 8, 29, 6, 30), result)
    }

    @Test
    fun oneTimePastToday_usesTomorrow() {
        val result = calculator.next(alarm(hour = 6, minute = 30), at(2026, 8, 29, 7, 0))
        assertEquals(at(2026, 8, 30, 6, 30), result)
    }

    @Test
    fun repeatingAlarm_wrapsToNextSelectedWeekday() {
        val friday = at(2026, 8, 28, 8, 0)
        val result = calculator.next(alarm(hour = 7, minute = 0, repeatDays = setOf(1, 3)), friday)
        assertEquals(at(2026, 8, 31, 7, 0), result)
    }

    @Test
    fun dstGap_resolvesToFirstValidLocalTime() {
        val ny = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2026, 3, 7, 12, 0, 0, 0, ny)
        val result = calculator.next(alarm(hour = 2, minute = 30, repeatDays = setOf(7)), now)
        assertEquals(3, result?.hour)
        assertEquals(8, result?.dayOfMonth)
    }

    @Test
    fun dstOverlap_keepsRequestedLocalTime() {
        val ny = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2026, 10, 31, 12, 0, 0, 0, ny)
        val result = calculator.next(alarm(hour = 1, minute = 30, repeatDays = setOf(7)), now)
        assertEquals(1, result?.hour)
        assertEquals(1, result?.dayOfMonth)
    }

    private fun alarm(
        hour: Int = 6,
        minute: Int = 30,
        repeatDays: Set<Int> = emptySet(),
        enabled: Boolean = true,
    ) = AlarmDefinition("id", hour, minute, "Alarm", repeatDays, true, enabled)

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone)
}
