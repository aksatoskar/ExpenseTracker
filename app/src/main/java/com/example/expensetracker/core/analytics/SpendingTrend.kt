package com.example.expensetracker.core.analytics

import com.example.expensetracker.core.time.DateRange
import com.example.expensetracker.data.local.entity.TransactionEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** Builds time-bucketed spend totals for the analytics trend chart. */
object SpendingTrend {

    fun bucketCount(rangeLabel: String): Int = when (rangeLabel) {
        "Today" -> 6
        "Week" -> 7
        else -> 5
    }

    fun compute(
        transactions: List<TransactionEntity>,
        range: DateRange,
        bucketCount: Int
    ): List<Long> {
        if (bucketCount <= 0) return emptyList()
        val span = (range.endMillis - range.startMillis).coerceAtLeast(1)
        val bucketSize = span / bucketCount
        val buckets = LongArray(bucketCount)
        transactions.forEach { tx ->
            val offset = (tx.timestamp - range.startMillis).coerceAtLeast(0)
            val index = (offset / bucketSize).toInt().coerceIn(0, bucketCount - 1)
            buckets[index] += tx.amountPaise
        }
        return buckets.toList()
    }

    fun labels(rangeLabel: String, range: DateRange, bucketCount: Int): List<String> {
        val zone = ZoneId.systemDefault()
        return when (rangeLabel) {
            "Today" -> listOf("12a", "4a", "8a", "12p", "4p", "8p").take(bucketCount)
            "Week" -> {
                val start = Instant.ofEpochMilli(range.startMillis).atZone(zone).toLocalDate()
                (0 until bucketCount).map { day ->
                    start.plusDays(day.toLong())
                        .dayOfWeek
                        .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                }
            }
            else -> (1..bucketCount).map { "W$it" }
        }
    }
}
