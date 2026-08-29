package com.risealarm.engine.alarm

import com.risealarm.domain.AlarmDefinition
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

class NextOccurrenceCalculator {
    fun next(alarm: AlarmDefinition, now: ZonedDateTime): ZonedDateTime? {
        if (!alarm.enabled) return null
        val alarmTime = LocalTime.of(alarm.hour, alarm.minute)

        if (alarm.repeatDays.isEmpty()) {
            val today = at(now.toLocalDate(), alarmTime, now)
            return if (today.isAfter(now)) today else at(now.toLocalDate().plusDays(1), alarmTime, now)
        }

        for (offset in 0L..7L) {
            val date = now.toLocalDate().plusDays(offset)
            if (date.dayOfWeek.value !in alarm.repeatDays) continue
            val candidate = at(date, alarmTime, now)
            if (candidate.isAfter(now)) return candidate
        }
        return null
    }

    private fun at(date: LocalDate, time: LocalTime, now: ZonedDateTime): ZonedDateTime =
        date.atTime(time).atZone(now.zone)
}
