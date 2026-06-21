package com.example.expensetracker.core.time

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * An inclusive `[startMillis, endMillis]` window of epoch milliseconds.
 *
 * The [companion][DateRange.Companion] factories produce the common ranges used across the app
 * (today, this/last week, a calendar month) in the device's default time zone.
 */
data class DateRange(val startMillis: Long, val endMillis: Long) {
    companion object {
        private val zone: ZoneId = ZoneId.systemDefault()

        fun today(): DateRange {
            val start = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
            val end = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            return DateRange(start, end)
        }

        fun week(): DateRange {
            val now = LocalDate.now()
            val startDate = now.minusDays((now.dayOfWeek.value - 1).toLong())
            val start = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = startDate.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            return DateRange(start, end)
        }

        fun lastWeek(): DateRange {
            val now = LocalDate.now()
            val startThisWeek = now.minusDays((now.dayOfWeek.value - 1).toLong())
            val startLastWeek = startThisWeek.minusDays(7)
            val start = startLastWeek.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = startThisWeek.atStartOfDay(zone).toInstant().toEpochMilli() - 1
            return DateRange(start, end)
        }

        fun month(month: YearMonth = YearMonth.now()): DateRange {
            val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            return DateRange(start, end)
        }
    }
}
