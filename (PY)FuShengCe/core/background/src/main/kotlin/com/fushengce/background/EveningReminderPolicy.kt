package com.fushengce.background

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

internal object EveningReminderPolicy {
    private val reminderTime = LocalTime.of(22, 0)

    fun nextRunAt(now: ZonedDateTime): ZonedDateTime {
        val todayAtTen = now.toLocalDate().atTime(reminderTime).atZone(now.zone)
        return if (now.isAfter(todayAtTen)) {
            now.toLocalDate().plusDays(1).atTime(reminderTime).atZone(now.zone)
        } else {
            todayAtTen
        }
    }

    fun shouldNotify(
        candidateCount: Int,
        today: LocalDate,
        lastNotifiedDate: LocalDate?,
    ): Boolean = candidateCount > 0 && lastNotifiedDate != today
}
