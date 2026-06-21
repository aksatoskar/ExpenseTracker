package com.example.expensetracker.presentation.common

import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

/** Formats an epoch timestamp as e.g. `20 Jun, 4:30 PM`. */
fun dateText(timestamp: Long): String =
    SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(Date(timestamp))

/** Human-friendly relative time for the last SMS sync (`Just now`, `5 min ago`, an absolute date). */
fun syncTimeText(millis: Long): String {
    if (millis <= 0L) return "Never"
    val diff = System.currentTimeMillis() - millis
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000L} min ago"
        diff < 86_400_000L -> "${diff / 3_600_000L} hr ago"
        else -> SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(Date(millis))
    }
}

/** Converts a `yyyy-MM` key into a display label like `June 2026`. */
fun monthYearLabel(yearMonth: String): String = runCatching {
    val ym = YearMonth.parse(yearMonth)
    "${ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${ym.year}"
}.getOrDefault(yearMonth)
